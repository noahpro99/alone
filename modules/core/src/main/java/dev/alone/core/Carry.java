package dev.alone.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Volume &amp; weight (proposal §5.1). <b>Volume</b> is a HARD cap on what you can carry; <b>weight</b>
 * is a SOFT penalty — the heavier your load, the slower you move (overloading is permitted and
 * brutal). Per-item values are coarse category defaults for now (blocks vs. small items), all
 * tunable; a per-item datapack can refine them later.
 */
public final class Carry {
    private Carry() {
    }

    /** The <b>hands/carry</b> budget, in m³ — the bulky things you hold or sling: a block, tools, boards.
     *  You can only manage a couple. (Small items don't count here — they go in {@link #POCKET_VOLUME_LIMIT}.) */
    public static final float PLAYER_VOLUME_LIMIT = 1.0f;
    /** A <b>separate</b> pocket budget for small items (seeds, flint, bones, nuggets, a bit of food). It's
     *  additive: filling your pockets never eats into your ability to carry a bulky thing, and vice versa. */
    public static final float POCKET_VOLUME_LIMIT = 0.30f;
    /** Items with a unit volume at or below this go in pockets; anything bigger is bulky and hand-carried
     *  (a shovel can't be pocketed, so extra shovels have nowhere to go). Tunable size threshold. */
    public static final float POCKET_ITEM_MAX = 0.05f;

    /** Storage volume per slot: a 27-slot chest = 1 m³, so a double chest = 2 m³, a barrel = 1, etc. */
    public static final float STORAGE_VOLUME_PER_SLOT = 1.0f / 27f;

    public static final float FREE_WEIGHT = 8f;     // kg carried with no movement penalty
    public static final float MAX_WEIGHT = 45f;     // kg where you're at the slowest crawl
    private static final float MIN_SPEED_FACTOR = 0.35f;

    private static final Identifier CARRY_MODIFIER = Identifier.fromNamespaceAndPath("alone", "carry_weight");

    private static final EquipmentSlot[] ARMOR_SLOTS =
        {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Per-item volume is deterministic per item type — compute the block shape once and cache it. */
    private static final Map<Item, Float> VOLUME_CACHE = new ConcurrentHashMap<>();

    public static float itemVolume(ItemStack stack) {
        return unitVolume(stack) * stack.getCount();
    }

    /** Volume (m³) of a single item of this stack's type. */
    public static float unitVolume(ItemStack stack) {
        float baseVolume = perItemVolume(stack.getItem());
        if (stack.getItem() instanceof WaterskinItem) {
            int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
            baseVolume += charges * 0.001f; // 1 liter (0.001 m³) per charge
        }
        return baseVolume;
    }

    public static float itemWeight(ItemStack stack) {
        return unitWeight(stack) * stack.getCount();
    }

    /** Weight (kg) of a single item of this stack's type. */
    public static float unitWeight(ItemStack stack) {
        float baseWeight = perItemWeight(stack.getItem());
        if (stack.getItem() instanceof WaterskinItem) {
            int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
            baseWeight += charges * 1.0f; // water is 1 kg/L, and a charge is ~1 L (matches unitVolume) — it's HEAVY
        }
        // Dried (jerked) food has lost most of its water — much lighter to carry (§4.2). The component lives
        // in the food module; look it up by id so core needn't depend on it.
        var dried = alone$driedComponent();
        if (dried != null && Boolean.TRUE.equals(stack.get(dried))) {
            baseWeight *= 0.4f;
        }
        return baseWeight;
    }

    private static net.minecraft.core.component.DataComponentType<Boolean> alone$dried;

    @SuppressWarnings("unchecked")
    private static net.minecraft.core.component.DataComponentType<Boolean> alone$driedComponent() {
        if (alone$dried == null) {
            alone$dried = (net.minecraft.core.component.DataComponentType<Boolean>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("alone", "dried"));
        }
        return alone$dried;
    }

    private static float perItemVolume(Item item) {
        return VOLUME_CACHE.computeIfAbsent(item, Carry::computeVolume);
    }

    /**
     * A "substantial" block-item is a solid building block (a full collision cube: dirt, stone, logs,
     * planks, ore, wool, glass…). These are the ones the no-throw rule (§5.1) refuses — place or store
     * them. It's <em>false</em> for context-placed things (seeds, sugar cane, saplings, flowers, crops,
     * torches, rails…), which have no full collision and so can be dropped/thrown like any small item.
     */
    public static boolean isSubstantialBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        try {
            return blockItem.getBlock().defaultBlockState()
                .isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (Exception ignored) {
            return false; // shapes that need world context → treat as a droppable small thing
        }
    }

    /**
     * A substantial block takes the space it occupies when placed — a full cube is 1 m³, a slab 0.5.
     * But many "block" items are really small placed things (seeds place crops, saplings, torches,
     * flowers, carpets) — those fall through to the small-item tiers, so a seed isn't a block's weight.
     */
    private static float computeVolume(Item item) {
        // Planks are thin sawn boards, not a solid cube of wood: a log's worth splits into ~4 and they
        // stack compact, so a handful carries easily (you can hold 4 to craft a table) and the wood
        // roughly conserves from log → planks instead of quadrupling out of nowhere.
        if (BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_planks")) {
            return 0.15f;
        }
        // The drying rack is an open lashed-stick frame, not a solid cube. Its collision box is near-full
        // (needed so you can click/hang food on it), which would otherwise read as a ~0.6 m³ block — but
        // you carry it as its handful of sticks: bulky and awkward to lug, yet light. So a quarter of your
        // hands each (you can manage a few), sized to its ~5-stick build rather than its footprint.
        if (item instanceof BlockItem drackVol && drackVol.getBlock() == AloneBlocks.DRYING_RACK) {
            return 0.25f;
        }
        if (item instanceof BlockItem blockItem) {
            float shape = blockShapeVolume(blockItem);
            if (shape >= 0.4f) {
                return Math.min(0.6f, shape); // a real block — a full one nearly fills the hands (carry ~one)
            }
            // else: a small placed thing — treat like any small item below
        }

        String path = BuiltInRegistries.ITEM.getKey(item).getPath();

        // Specific custom Alone items
        // A rigid pot's CARRY bulk (awkward, can't be squashed) is far bigger than its hollow footprint —
        // above the pocket threshold, so it's hand-carried, not pocketed (you can't slip pots in a pocket).
        // A floppy hide waterskin, by contrast, packs/belts fine and stays pocketable.
        if (item == AloneItems.WATERSKIN) return 0.005f; // floppy skin — pockets fine
        if (item == AloneItems.IRON_POT) return 0.20f;   // bulky rigid vessel — hand-carried
        if (item == AloneItems.CLAY_POT) return 0.16f;
        if (item == AloneItems.UNFIRED_CLAY_POT) return 0.15f;
        if (item == AloneItems.BACKPACK) return 0.080f;
        if (item == AloneItems.SALT) return 0.001f;
        if (item == AloneItems.IRON_BLOOM) return 0.35f;       // a dense lump — hand-carried, never pocketed
        if (item == AloneItems.REFRACTORY_CLAY) return 0.030f; // a clay lump — several carriable
        if (item == AloneItems.GROG) return 0.010f;            // gritty temper
        if (item == AloneItems.PLANT_FIBER) return 0.001f;
        if (item == AloneItems.SPLINT) return 0.005f;
        if (item == AloneItems.SMITHING_HAMMER) return 0.300f; // a heavy, unwieldy tool — hand-carried
        if (item == AloneItems.WHETSTONE) return 0.002f;
        if (item == AloneItems.HERBAL_REMEDY) return 0.003f;
        if (item == AloneItems.EMBER) return 0.001f;
        if (item == AloneItems.TORCH || item == AloneItems.TORCH_LIT) return 0.002f;
        if (item == AloneItems.ROPE) return 0.005f;
        if (item == AloneItems.BEDROLL) return 0.150f; // a bulky roll — hand-carried, not pocketed
        // A travois is a frame of long poles (2–3 m) lashed with crossbars. Even undeployed it's a big,
        // awkward bundle you sling over a shoulder — never a pocket item, and it dominates your hands
        // (most of the 1 m³ carry budget). This is why you deploy and DRAG it rather than pocket it.
        if (item == AloneItems.TRAVOIS) return 0.70f;

        // Vanilla item paths
        if (path.contains("ingot") || path.contains("brick") || path.contains("raw_")) {
            return 0.010f;
        }
        if (path.contains("nugget")) {
            return 0.001f;
        }
        // Long, awkward tools ride in your hands or lashed on your back, never your pockets — you can
        // manage a couple, not a stack (§5.1). Custom flint/steel tools are caught by their names too.
        if (path.contains("sword") || path.contains("axe") || path.contains("hatchet")
            || path.contains("pick") || path.contains("shovel") || path.contains("hoe")
            || path.contains("shield") || path.contains("bow") || path.contains("crossbow")
            || path.contains("hammer") || path.contains("scythe")
            || path.contains("spear") || path.contains("trident")) { // a spear is a long haft — never pocketed
            return 0.35f;
        }
        if (path.contains("knife") || path.contains("shears")) {
            return 0.20f; // a blade is less unwieldy than a hafted tool, but still not pocket-small
        }
        if (path.contains("helmet") || path.contains("cap") || path.contains("boots")) {
            return 0.020f;
        }
        if (path.contains("chestplate") || path.contains("tunic") || path.contains("leggings") || path.contains("pants")) {
            return 0.050f;
        }
        if (path.equals("bucket") || path.contains("bucket")) {
            return 0.025f;
        }
        if (path.contains("stick") || path.contains("string") || path.contains("feather") || path.contains("paper")) {
            return 0.001f;
        }
        
        ItemStack def = item.getDefaultInstance();
        if (def.isDamageableItem()) {
            return 0.02f; // default damageable item
        }
        if (def.has(DataComponents.FOOD)) {
            return 0.005f; // food portion (5 liters)
        }

        return 0.003f; // default for seeds, dust, bones…
    }

    private static float blockShapeVolume(BlockItem blockItem) {
        try {
            BlockState state = blockItem.getBlock().defaultBlockState();
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (!shape.isEmpty()) {
                AABB b = shape.bounds();
                return (float) (b.getXsize() * b.getYsize() * b.getZsize());
            }
        } catch (Exception ignored) {
            // some blocks want real world context for their shape
        }
        return 0.25f; // unknown → below the block threshold, treated as a small item
    }

    private static float getBlockDensity(String path) {
        if (path.equals("gold_block") || path.equals("raw_gold_block")) {
            return 19300f;
        }
        if (path.equals("netherite_block")) {
            return 20000f;
        }
        if (path.equals("iron_block") || path.equals("raw_iron_block")) {
            return 7870f;
        }
        if (path.equals("copper_block") || path.equals("raw_copper_block") || path.contains("cut_copper")) {
            return 8960f;
        }
        if (path.contains("anvil")) {
            return 7870f;
        }
        if (path.contains("coal_block")) {
            return 1500f;
        }
        if (path.contains("obsidian")) {
            return 2600f;
        }
        if (path.contains("stone") || path.contains("cobblestone") || path.contains("granite") ||
            path.contains("diorite") || path.contains("andesite") || path.contains("deepslate") ||
            path.contains("tuff") || path.contains("calcite") || path.contains("bricks") ||
            path.contains("basalt") || path.contains("blackstone") || path.contains("netherrack") ||
            path.contains("end_stone") || path.contains("prismarine")) {
            return 2700f;
        }
        if (path.contains("sand") || path.contains("gravel") || path.contains("concrete_powder")) {
            return 1600f;
        }
        if (path.contains("clay")) {
            return 1800f;
        }
        if (path.contains("dirt") || path.contains("grass") || path.contains("podzol") ||
            path.contains("mycelium") || path.contains("farmland") || path.contains("mud") ||
            path.contains("soil") || path.contains("peat") || path.contains("roots")) {
            return 1200f;
        }
        if (path.contains("log") || path.contains("wood") || path.contains("stem") || path.contains("hyphae")) {
            return 700f;
        }
        if (path.contains("planks") || path.contains("fence") || path.contains("gate") ||
            path.contains("stair") || path.contains("slab") || path.contains("door") || path.contains("trapdoor")) {
            return 500f;
        }
        if (path.contains("leaves")) {
            return 100f;
        }
        if (path.contains("wool") || path.contains("carpet")) {
            return 100f;
        }
        if (path.contains("hay") || path.contains("thatch") || path.contains("target")) {
            return 150f;
        }
        if (path.contains("glass")) {
            return 2500f;
        }
        if (path.contains("ice")) {
            return 900f;
        }
        if (path.contains("snow")) {
            return 300f;
        }
        return 1500f; // default density (medium block)
    }

    /** Weight (kg). Coarse tiers — real blocks scale with volume, tools moderate, food a portion, rest light. */
    private static float perItemWeight(Item item) {
        // A single sawn board — a quarter of a log's mass, so 4 planks ≈ one log (mass conserves).
        if (BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_planks")) {
            return 1.5f;
        }
        // A drying rack is ~5 sticks lashed together — light, whatever its footprint (see computeVolume).
        // 5 × the ~0.2 kg stick, plus a little for lashing.
        if (item instanceof BlockItem drackWt && drackWt.getBlock() == AloneBlocks.DRYING_RACK) {
            return 1.2f;
        }
        if (item instanceof BlockItem && perItemVolume(item) >= 0.4f) {
            float shape = perItemVolume(item);
            String path = BuiltInRegistries.BLOCK.getKey(((BlockItem) item).getBlock()).getPath();
            return shape * getBlockDensity(path) / 90f;
        }

        String path = BuiltInRegistries.ITEM.getKey(item).getPath();

        // 1. Specific custom Alone items
        if (item == AloneItems.WATERSKIN) return 0.20f; // empty waterskin
        if (item == AloneItems.IRON_POT) return 3.00f;   // solid iron pot
        if (item == AloneItems.CLAY_POT) return 1.50f;   // fired clay — lighter than iron, but fragile
        if (item == AloneItems.UNFIRED_CLAY_POT) return 1.60f; // wet clay, a touch heavier
        if (item == AloneItems.BACKPACK) return 1.50f;
        if (item == AloneItems.WOVEN_BASKET) return 0.50f; // woven plant fibre — light but bulky
        if (item == AloneItems.SALT) return 0.05f;
        if (item == AloneItems.IRON_BLOOM) return 2.50f;       // a spongy lump of raw iron and slag — heavy
        if (item == AloneItems.REFRACTORY_CLAY) return 0.70f;  // a clay-and-temper lump for lining a furnace
        if (item == AloneItems.GROG) return 0.30f;             // crushed fired pottery — a gritty temper
        if (item == AloneItems.ROCK || item == AloneItems.HOT_ROCK) return 0.40f; // a fist-sized stone
        if (item == AloneItems.PLANT_FIBER) return 0.01f;
        if (item == AloneItems.SPLINT) return 0.40f;
        if (item == AloneItems.SMITHING_HAMMER) return 2.50f;
        if (item == AloneItems.WHETSTONE) return 0.50f;
        if (item == AloneItems.HERBAL_REMEDY) return 0.25f;
        if (item == AloneItems.EMBER) return 0.15f;
        if (item == AloneItems.TORCH || item == AloneItems.TORCH_LIT) return 0.25f;
        if (item == AloneItems.ROPE) return 0.30f;
        if (item == AloneItems.BEDROLL) return 2.00f;
        if (item == AloneItems.TRAVOIS) return 8.00f; // two wooden poles, crossbars and lashing — a real load to lug

        // 2. Specific vanilla items matching real life
        if (path.contains("ingot") || path.contains("brick")) {
            if (path.contains("gold")) return 19.30f / 9f; // in-game weight matches realistic fraction
            if (path.contains("netherite")) return 20.00f / 9f;
            if (path.contains("iron")) return 7.87f / 9f;
            if (path.contains("copper")) return 8.96f / 9f;
            return 1.50f; // default ingot
        }
        if (path.contains("nugget")) {
            if (path.contains("gold")) return (19.30f / 9f) / 9f;
            if (path.contains("iron")) return (7.87f / 9f) / 9f;
            return 0.15f;
        }
        if (path.contains("raw_")) {
            if (path.contains("gold")) return 2.00f;
            if (path.contains("iron")) return 1.20f;
            if (path.contains("copper")) return 1.30f;
            return 1.20f;
        }

        // Tools, weapons, shields, bows
        if (path.contains("sword")) return 1.20f;
        if (path.contains("pickaxe")) return 2.00f;
        if (path.contains("axe")) return 1.80f;
        if (path.contains("shovel")) return 1.50f;
        if (path.contains("hoe")) return 1.20f;
        if (path.contains("shield")) return 5.00f;
        if (path.contains("bow")) return 1.00f;
        if (path.contains("crossbow")) return 3.00f;
        if (path.contains("spear")) return 1.50f;   // a hafted spear — a long shaft and a point
        if (path.contains("trident")) return 2.50f;
        if (path.contains("arrow")) return 0.05f;

        // Armor pieces
        if (path.contains("helmet") || path.contains("cap")) {
            if (path.contains("leather")) return 1.00f;
            if (path.contains("chainmail")) return 2.00f;
            if (path.contains("iron") || path.contains("steel")) return 2.50f;
            if (path.contains("gold")) return 5.00f;
            if (path.contains("netherite")) return 6.00f;
            return 2.00f;
        }
        if (path.contains("chestplate") || path.contains("tunic")) {
            if (path.contains("leather")) return 3.00f;
            if (path.contains("chainmail")) return 8.00f;
            if (path.contains("iron") || path.contains("steel")) return 10.00f;
            if (path.contains("gold")) return 20.00f;
            if (path.contains("netherite")) return 25.00f;
            return 8.00f;
        }
        if (path.contains("leggings") || path.contains("pants")) {
            if (path.contains("leather")) return 2.00f;
            if (path.contains("chainmail")) return 5.00f;
            if (path.contains("iron") || path.contains("steel")) return 6.50f;
            if (path.contains("gold")) return 12.00f;
            if (path.contains("netherite")) return 15.00f;
            return 5.00f;
        }
        if (path.contains("boots")) {
            if (path.contains("leather")) return 0.80f;
            if (path.contains("chainmail")) return 1.50f;
            if (path.contains("iron") || path.contains("steel")) return 2.00f;
            if (path.contains("gold")) return 4.00f;
            if (path.contains("netherite")) return 5.00f;
            return 1.50f;
        }

        // Buckets
        if (path.equals("bucket")) return 3.00f;
        if (path.equals("water_bucket")) return 13.00f;
        if (path.equals("lava_bucket")) return 33.00f;
        if (path.equals("milk_bucket")) return 13.30f;

        // Common resources
        if (path.contains("stick")) return 0.20f;
        if (path.contains("coal") || path.contains("charcoal")) return 0.20f;
        if (path.contains("flint")) return 0.30f;
        if (path.contains("string")) return 0.01f;
        if (path.contains("feather")) return 0.005f;
        if (path.contains("leather")) return 1.00f;
        if (path.contains("paper")) return 0.01f;
        if (path.contains("wheat")) return 0.10f;
        if (path.contains("bone")) return 0.20f;
        if (path.contains("clay_ball")) return 0.50f;
        if (path.contains("egg")) return 0.06f;
        if (path.contains("ender_pearl")) return 0.50f;

        // Food
        ItemStack def = item.getDefaultInstance();
        if (def.has(DataComponents.FOOD)) {
            if (path.contains("berry") || path.contains("cookie") || path.contains("kelp")) {
                return 0.05f; // small snacks
            }
            if (path.contains("apple") || path.contains("melon")) {
                return 0.15f;
            }
            if (path.contains("stew") || path.contains("soup")) {
                return 0.50f;
            }
            return 0.35f; // standard portion
        }

        return 0.05f; // default weight for small misc items
    }

    public static float totalVolume(Player player) {
        return total(player, true);
    }

    public static float totalWeight(Player player) {
        return total(player, false);
    }

    public static float containerVolume(Container container) {
        float sum = 0f;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                sum += itemVolume(stack);
            }
        }
        return sum;
    }

    /** The player's personal volume cap, raised by a backpack (§6). */
    /** How much a backpack can hold, in m³ — its own separate volume budget (§6). */
    public static final float BACKPACK_VOLUME_LIMIT = 1.5f;

    /** A woven pack basket slung on the back raises how much you can carry — the pre-leather carry aid (§6). */
    public static final float BASKET_VOLUME_BONUS = 0.5f;

    /** Your body's volume cap. A backpack no longer raises this — it's separate storage with its own
     *  cap ({@link #BACKPACK_VOLUME_LIMIT}); only its weight lands on you. A woven basket, though, is worn
     *  and simply lets you carry more (no leather, no animals needed). */
    public static float volumeLimit(Player player) {
        return PLAYER_VOLUME_LIMIT + (hasBasket(player) ? BASKET_VOLUME_BONUS : 0f);
    }

    /** True if the player is carrying a woven basket (one is enough — extras don't stack the bonus). */
    private static boolean hasBasket(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(AloneItems.WOVEN_BASKET)) {
                return true;
            }
        }
        return false;
    }

    /** Volume cap for a container, or {@link Float#MAX_VALUE} for uncapped (functional) containers. */
    public static float containerVolumeLimit(Container container) {
        if (container instanceof Inventory inventory) {
            return volumeLimit(inventory.player);
        }
        if (container instanceof BackpackContainer || container instanceof BackpackBlockEntity) {
            return BACKPACK_VOLUME_LIMIT; // a pack has its own volume budget (§6)
        }
        if (container instanceof RandomizableContainerBlockEntity || container instanceof CompoundContainer) {
            return container.getContainerSize() * STORAGE_VOLUME_PER_SLOT; // chests, barrels, shulkers…
        }
        return Float.MAX_VALUE; // furnaces, crafting, brewing, etc.
    }

    // ── Hands vs. pockets (§5.1) ─────────────────────────────────────────────────────────────────
    // Your body carry is two independent budgets: bulky things you hold/sling (hands) and small things
    // you pocket. A shovel is too big to pocket, so extra shovels can only compete for the limited hand
    // space — one in hand is fine, two more won't fit. Small items get their own budget on top.

    /** True if this item is small enough to go in a pocket (vs. bulky, hand-carried). */
    public static boolean isPocketable(ItemStack stack) {
        return unitVolume(stack) <= POCKET_ITEM_MAX;
    }

    /** The hands budget — bulky items you hold or sling; raised by a woven basket. */
    public static float handLimit(Player player) {
        return volumeLimit(player);
    }

    /** The pocket budget — small items only. */
    public static float pocketLimit(Player player) {
        return POCKET_VOLUME_LIMIT;
    }

    /** m³ of bulky (hand) or small (pocket) items currently on the body — worn gear and the basket itself
     *  don't count (they're on your body / are the carry aid, not part of the load). */
    private static float bucketVolume(Player player, boolean pocket) {
        Inventory inventory = player.getInventory();
        float sum = 0f;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || stack.is(AloneItems.WOVEN_BASKET) || isWornArmor(player, stack)) {
                continue;
            }
            if (isPocketable(stack) == pocket) {
                sum += itemVolume(stack);
            }
        }
        return sum;
    }

    public static float handVolume(Player player) {
        return bucketVolume(player, false);
    }

    public static float pocketVolume(Player player) {
        return bucketVolume(player, true);
    }

    /** How much more volume this item could take in the given container — bucket-aware for the player's
     *  own inventory (pockets vs. hands), plain single-budget for chests/backpacks. {@link Float#MAX_VALUE}
     *  means uncapped. Used by the pickup and slot-placement caps so both honour the two budgets. */
    public static float remainingFor(Container container, ItemStack stack) {
        if (container instanceof Inventory inventory) {
            boolean pocket = isPocketable(stack);
            float used = pocket ? pocketVolume(inventory.player) : handVolume(inventory.player);
            float limit = pocket ? pocketLimit(inventory.player) : handLimit(inventory.player);
            return limit + 0.001f - used;
        }
        float limit = containerVolumeLimit(container);
        if (limit == Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        return limit + 0.001f - containerVolume(container);
    }

    /** For the HUD: how full the body is, as the fuller of the two budgets (so the bar warns when EITHER
     *  hands or pockets is full, not a misleading average). */
    public static float volumeFullnessPct(Player player) {
        float hand = handVolume(player) / Math.max(0.0001f, handLimit(player));
        float pocket = pocketVolume(player) / Math.max(0.0001f, pocketLimit(player));
        return Math.max(hand, pocket);
    }

    private static float total(Player player, boolean volume) {
        Inventory inventory = player.getInventory();
        float sum = 0f;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            // Worn armour rides on your body, not in your pack — it doesn't eat your volume budget
            // (its weight still slows you). So a full set of gear never blocks picking a block up.
            if (volume && isWornArmor(player, stack)) {
                continue;
            }
            sum += volume ? itemVolume(stack) : itemWeight(stack);
            // A backpack's contents don't touch your body's VOLUME (the pack has its own separate cap),
            // but their WEIGHT still lands on you — hauling a full pack is heavy (§6).
            if (!volume && stack.is(AloneItems.BACKPACK)) {
                var contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.EMPTY);
                for (var inner : (Iterable<ItemStack>) contents.allItemsCopyStream()::iterator) {
                    sum += itemWeight(inner);
                }
            }
        }
        return sum;
    }

    /** Is this exact stack currently equipped in an armour slot (as opposed to sitting in the pack)? */
    private static boolean isWornArmor(Player player, ItemStack stack) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot) == stack) {
                return true;
            }
        }
        return false;
    }

    /** Recompute the weight → movement penalty and apply it. Call each server tick. */
    public static void applyWeightMovement(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        float factor = speedFactor(totalWeight(player));
        if (factor < 1.0f) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                CARRY_MODIFIER, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(CARRY_MODIFIER);
        }
    }

    private static float speedFactor(float weight) {
        if (weight <= FREE_WEIGHT) {
            return 1.0f;
        }
        float t = Math.min(1.0f, (weight - FREE_WEIGHT) / (MAX_WEIGHT - FREE_WEIGHT));
        return 1.0f - t * (1.0f - MIN_SPEED_FACTOR);
    }
}
