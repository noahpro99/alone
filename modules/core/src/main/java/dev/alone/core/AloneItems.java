package dev.alone.core;

import java.util.function.Function;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Blocks;

/**
 * Alone's custom items (proposal §2/§5/§6). Textures/models are placeholders for now — items render
 * as missing-texture until art is added, but they fully function. Registered the 26.2 way: an item
 * carries its {@link ResourceKey} via {@code Properties.setId}.
 */
public final class AloneItems {
    private AloneItems() {
    }

    /** How much water a vessel is holding, in sips. */
    public static final DataComponentType<Integer> WATER_CHARGES = intComponent("water_charges");
    /** Quality of the water it holds — raw (0), clean/boiled (1), or tainted (2). */
    public static final DataComponentType<Integer> WATER_QUALITY = intComponent("water_quality");
    /** Whether the vessel itself is dirty (raw water leaves residue; boiling sterilises it). */
    public static final DataComponentType<Boolean> VESSEL_DIRTY = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "vessel_dirty"),
        DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    private static DataComponentType<Integer> intComponent(String path) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("alone", path),
            DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    }

    public static final Item BACKPACK = register("backpack",
        key -> new BackpackItem(new Item.Properties().stacksTo(1).component(
            net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.EMPTY).setId(key)));
    /** A woven pack basket (proposal §6) — the day-one carry aid, made of plant fibre and withies (no
     *  leather, no animals). Slung on the back, it raises how much you can carry ({@link Carry#volumeLimit});
     *  smaller than the leather backpack, which is a proper openable pack. */
    public static final Item WOVEN_BASKET = register("woven_basket",
        key -> new Item(new Item.Properties().stacksTo(1).setId(key)));
    /** A travois (proposal §6) — a dragged cargo sled. Right-click ground to set it down. */
    public static final Item TRAVOIS = register("travois",
        key -> new TravoisItem(new Item.Properties().stacksTo(1).setId(key)));
    /** Spawn egg for the {@link BrownBear} (§7.2) — every custom mob ships one. */
    public static final Item BROWN_BEAR_SPAWN_EGG = register("brown_bear_spawn_egg",
        key -> new SpawnEggItem(new Item.Properties().spawnEgg(AloneEntities.BROWN_BEAR).setId(key)));
    /** Spawn egg for the {@link Deer} (§7.2). */
    public static final Item DEER_SPAWN_EGG = register("deer_spawn_egg",
        key -> new SpawnEggItem(new Item.Properties().spawnEgg(AloneEntities.DEER).setId(key)));

    public static final Item SQUIRREL_SPAWN_EGG = register("squirrel_spawn_egg",
        key -> new SpawnEggItem(new Item.Properties().spawnEgg(AloneEntities.SQUIRREL).setId(key)));
    /** A placeable bedroll — a real bed block that reads as a flat mat (see {@link AloneBlocks#BEDROLL}). */
    public static final Item BEDROLL = register("bedroll",
        key -> new BedItem(AloneBlocks.BEDROLL, new Item.Properties().stacksTo(1).setId(key)));
    /** A warmth-rated sleeping bag — a bedroll that keeps you warm enough to rest well on a cold night
     *  (see {@link AloneBlocks#SLEEPING_BAG}; insulation applied in {@link SurvivalMeters}). */
    public static final Item SLEEPING_BAG = register("sleeping_bag",
        key -> new BedItem(AloneBlocks.SLEEPING_BAG, new Item.Properties().stacksTo(1).setId(key)));
    /** Thatch — quick primitive roofing (sticks + plant fibre) that shelters the space beneath it
     *  (see {@link AloneBlocks#THATCH}). */
    public static final Item THATCH = register("thatch",
        key -> new BlockItem(AloneBlocks.THATCH, new Item.Properties().setId(key)));
    /** Tarp — a waterproof, fireproof, reusable hide sheet: the premium roof above thatch
     *  (see {@link AloneBlocks#TARP}). Hard to make (leather + cordage) or brought. */
    public static final Item TARP = register("tarp",
        key -> new BlockItem(AloneBlocks.TARP, new Item.Properties().setId(key)));
    /** A sewing kit (bone needle + cordage) — mend worn leather/hide clothing by hand, a thread (plant
     *  fibre) per patch (see {@link SewingKitItem}). */
    public static final Item SEWING_KIT = register("sewing_kit",
        key -> new SewingKitItem(new Item.Properties().durability(64).setId(key)));
    public static final Item WATERSKIN = register("waterskin",
        key -> new WaterskinItem(new Item.Properties().stacksTo(1).setId(key), 3, false));
    /** Iron pot (§2 vessel ladder) — holds far more water than a skin; the workhorse vessel. */
    public static final Item IRON_POT = register("iron_pot",
        key -> new WaterskinItem(new Item.Properties().stacksTo(1).setId(key), 6, true));

    /** An unfired clay pot (§2/§3) — shaped from clay by hand; useless as a vessel until you <b>bake it
     *  in a fire</b> (campfire or furnace) into a {@link #CLAY_POT}. The first step of the heat-tech tree. */
    public static final Item UNFIRED_CLAY_POT = register("unfired_clay_pot",
        key -> new Item(new Item.Properties().stacksTo(16).setId(key)));
    /** Clay pot (§2 vessel ladder) — the first <b>fire-safe</b> pot, before metal: set it on a fire to
     *  boil water clean and to cook. The stone-age answer to the iron pot, fired from clay. */
    public static final Item CLAY_POT = register("clay_pot",
        key -> new WaterskinItem(new Item.Properties().stacksTo(1).setId(key), 4, true));

    /** The kiln as a placeable item (§3.2) — lay it down and fire your pottery in it. */
    public static final Item KILN = register("kiln",
        key -> new net.minecraft.world.item.BlockItem(AloneBlocks.KILN, new Item.Properties().setId(key)));

    /** Grog (§3.2) — crushed, previously-fired clay/brick. Mixed into raw clay as temper so it survives
     *  the heat of a smelt (raw clay alone shrinks and cracks). Crush a brick to get it. */
    public static final Item GROG = register("grog",
        key -> new Item(new Item.Properties().setId(key)));
    /** Refractory clay (§3.2) — raw clay tempered with sand or grog so it won't crack when fired hot.
     *  The heat-resistant stuff a {@link AloneBlocks#BLOOMERY bloomery} is built from. */
    public static final Item REFRACTORY_CLAY = register("refractory_clay",
        key -> new Item(new Item.Properties().setId(key)));
    /** A bloom of raw iron (§8.2) — the spongy, slag-riddled lump a bloomery yields; hammer it to
     *  consolidate it into a usable iron ingot. */
    public static final Item IRON_BLOOM = register("iron_bloom",
        key -> new Item(new Item.Properties().setId(key)));
    /** The bloomery as a placeable item (§3.2/§8.2) — the primitive iron furnace. */
    public static final Item BLOOMERY = register("bloomery",
        key -> new net.minecraft.world.item.BlockItem(AloneBlocks.BLOOMERY, new Item.Properties().setId(key)));
    /** A drying rack (§4.2) — hang perishable food to dry into non-spoiling jerky. */
    public static final Item DRYING_RACK = register("drying_rack",
        key -> new net.minecraft.world.item.BlockItem(AloneBlocks.DRYING_RACK, new Item.Properties().setId(key)));

    public static final Item SNARE = register("snare",
        key -> new net.minecraft.world.item.BlockItem(AloneBlocks.SNARE, new Item.Properties().setId(key)));

    public static final Item DEADFALL = register("deadfall",
        key -> new net.minecraft.world.item.BlockItem(AloneBlocks.DEADFALL, new Item.Properties().setId(key)));

    public static final Item FISH_TRAP = register("fish_trap",
        key -> new net.minecraft.world.item.PlaceOnWaterBlockItem(AloneBlocks.FISH_TRAP, new Item.Properties().setId(key)));
    /** A gill net — a portable, open-water fishing net (see {@link AloneBlocks#GILL_NET}); set on deep water. */
    public static final Item GILL_NET = register("gill_net",
        key -> new net.minecraft.world.item.PlaceOnWaterBlockItem(AloneBlocks.GILL_NET, new Item.Properties().setId(key)));

    /** Salt (§2/§4.2) — boil seawater to get it; use it to preserve food for winter. */
    public static final Item SALT = register("salt",
        key -> new Item(new Item.Properties().setId(key)));

    /** A loose rock (§8.1) — foraged off the ground / from gravel; your day-one stone, and the
     *  hammerstone you knap flint with. */
    public static final Item ROCK = register("rock",
        key -> new Item(new Item.Properties().setId(key)));
    /** A knapped flint flake (§8.1) — struck from flint with a rock; the sharp edge for flint tools. */
    public static final Item FLINT_SHARD = register("flint_shard",
        key -> new Item(new Item.Properties().setId(key)));

    /** A stone heated in a fire (§2) for <b>hot-rock boiling</b> — dropped into a hide skin or bark cup
     *  that can't sit over the flame, it brings the water to a boil and turns it clean. It cools as you
     *  carry it (the durability bar is its remaining heat), reverting to a plain {@link #ROCK} once cold. */
    public static final int HOT_ROCK_LIFE = 2400; // ~2 min of usable heat before it cools to a plain rock
    public static final Item HOT_ROCK = register("hot_rock",
        key -> new Item(new Item.Properties().stacksTo(1).durability(HOT_ROCK_LIFE).setId(key)));

    /** Rope coil (§5.7) — throw it down a cliff face to hang a free, safe climb line. */
    public static final Item ROPE = register("rope",
        key -> new RopeItem(new Item.Properties().stacksTo(64).setId(key)));

    /** Plant fiber (§8.1) — stripped from grass/ferns/vines; twist a few into string (no spiders needed). */
    public static final Item PLANT_FIBER = register("plant_fiber",
        key -> new Item(new Item.Properties().setId(key)));

    /** A hank of stripped fibre gathered and bundled ready to twist — the real middle step between loose
     *  fibre and cordage, and the way we load a big material cost into cordage without needing >9 in a grid. */
    public static final Item FIBER_BUNDLE = register("fiber_bundle",
        key -> new Item(new Item.Properties().setId(key)));

    /** Splint (§1.5 medicine) — sticks bound with cord; sneak + right-click to treat a sprain. */
    public static final Item SPLINT = register("splint",
        key -> new Item(new Item.Properties().setId(key)));

    /** Smithing hammer (§8.2) — the tool you forge metal with. Kept out of the forgeable tag (no
     *  chicken-and-egg): craft it normally, then hammer everything else into shape at an anvil. */
    public static final Item SMITHING_HAMMER = register("smithing_hammer",
        key -> new Item(new Item.Properties().durability(512).setId(key)));

    /** Whetstone (§8.5) — hone a worn edge back up. Hold the tool, whetstone in the off hand, sneak +
     *  right-click to re-sharpen (restore durability); the stone itself wears with use. */
    public static final Item WHETSTONE = register("whetstone",
        key -> new Item(new Item.Properties().durability(128).setId(key)));

    /** Herbal remedy (§1.5 medicine) — steeped herbs; drink it to settle a foodborne illness and ease
     *  a festering wound. The treatment the sickness condition was missing. */
    public static final Item HERBAL_REMEDY = register("herbal_remedy",
        key -> new RemedyItem(new Item.Properties().stacksTo(16).setId(key)));

    /** A glowing ember (§3.1) — scoop it from a fire and carry it to the next camp to light a fresh
     *  one without drilling. It cools as you carry it (the durability bar is its remaining life) and
     *  dies to cold charcoal if you don't use it in time. */
    public static final int EMBER_LIFE = 4000; // ~3.3 min of glow before it dies
    public static final Item EMBER = register("ember",
        key -> new Item(new Item.Properties().stacksTo(1).durability(EMBER_LIFE).setId(key)));

    // Torch as a fuel item (§5.6): crafted unlit at full durability; light it and it burns down. The
    // durability bar IS the remaining fuel. A shader lights a held lit torch.
    public static final int TORCH_FUEL = 6000; // ~5 min of burn at 1/tick
    public static final Item TORCH = register("torch",
        key -> new TorchItem(new Item.Properties().durability(TORCH_FUEL).setId(key)));
    // A lit torch can be planted like a vanilla torch (placing it makes an ordinary torch block); held,
    // it burns its fuel down. Light an unlit torch first — you can't place a dark one.
    public static final Item TORCH_LIT = register("torch_lit",
        key -> new TorchBlockItem(Blocks.TORCH, Blocks.WALL_TORCH, Direction.DOWN,
            new Item.Properties().durability(TORCH_FUEL).setId(key)));

    // Primitive knapped tools (§8.1) — a flint tier between wood and stone: low durability, workable, and
    // unlike a stone/copper pick it CAN'T work iron ore (see the incorrect_for_flint_tool tag). That gates
    // the flint -> copper -> iron ladder: you must forge a copper pick before you can mine iron.
    private static final TagKey<Block> INCORRECT_FOR_FLINT = TagKey.create(Registries.BLOCK,
        Identifier.fromNamespaceAndPath("alone", "incorrect_for_flint_tool"));
    private static final ToolMaterial FLINT =
        new ToolMaterial(INCORRECT_FOR_FLINT, 48, 2.5F, 1.0F, 3, ItemTags.STONE_TOOL_MATERIALS);

    public static final Item FLINT_HATCHET = register("flint_hatchet",
        key -> new AxeItem(FLINT, 6.0F, -3.2F, new Item.Properties().setId(key)));
    /** A hand saw (iron) — the woodworking upgrade over splitting boards by hand: sneak + hold right-click
     *  a log with it to saw far faster, for far less sweat, and get more boards per log (see {@link Riving}). */
    public static final Item HAND_SAW = register("hand_saw",
        key -> new Item(new Item.Properties().durability(250).setId(key)));
    /** A towel — right-click to rub yourself dry at once (see {@link TowelItem}); goes damp after a use. */
    public static final Item TOWEL = register("towel",
        key -> new TowelItem(new Item.Properties().stacksTo(1).setId(key)));
    /** Flour — wheat ground down, the first step of the bread path (grind → dough → bake). Placeholder sugar art. */
    public static final Item FLOUR = register("flour",
        key -> new Item(new Item.Properties().setId(key)));
    /** Dough — flour worked with water; bake it on a fire (or in a furnace) into bread. Placeholder clay art. */
    public static final Item DOUGH = register("dough",
        key -> new Item(new Item.Properties().setId(key)));
    public static final Item FLINT_PICK = register("flint_pick",
        key -> new Item(new Item.Properties().pickaxe(FLINT, 1.0F, -2.8F).setId(key)));
    public static final Item FLINT_KNIFE = register("flint_knife",
        key -> new Item(new Item.Properties().sword(FLINT, 2.0F, -2.0F).setId(key)));
    /** A flint hoe (§4.1) — a knapped blade lashed to a handle, the way the Neolithic tilled the ground
     *  long before any metal. It's what lets you farm in the stone age, so farming can precede metalwork
     *  as it did in life — not wait behind a copper hoe. */
    public static final Item FLINT_HOE = register("flint_hoe",
        key -> new Item(new Item.Properties().hoe(FLINT, -2.0F, -1.0F).setId(key)));
    /** A flint-tipped spear (§8.6) — king of the early game: a real thrusting reach so you can hit before
     *  the animal (or attacker) reaches you, plus the vanilla spear's charged thrust and piercing. A
     *  flint point lashed to a shaft, at the flint tier; uses the stone-spear tuning and look. */
    public static final Item FLINT_SPEAR = register("flint_spear",
        key -> new Item(new Item.Properties()
            .spear(FLINT, 0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F).setId(key)));
    /** A bow drill (proposal §3.1) — a spindle spun by a corded bow: the proper primitive fire tool.
     *  Far faster and less exhausting than palming a bare stick. The cordage frays and the spindle burns
     *  down, so it wears out (durability) and eventually needs re-stringing. */
    public static final Item BOW_DRILL = register("bow_drill",
        key -> new Item(new Item.Properties().durability(24).setId(key)));

    /** A ferro rod / fire steel (§3.1) — a hard steel striker that throws hot sparks: a <b>fast, reliable</b>
     *  light that works even in the wet, unlike a friction drill. It's a real gate to get one — you must
     *  have <b>steel</b> to forge it — or you bring it in a starting loadout, which is exactly why it's a
     *  prized pick. Wears down over many, many strikes (durability). */
    public static final Item FERRO_ROD = register("ferro_rod",
        key -> new Item(new Item.Properties().durability(200).setId(key)));
    // Copper (flint → copper → iron → steel) is vanilla in 26.2 — its tools/armour, recipes, and tool
    // tags are all built in, and it needs no forging (a soft metal), so it fits the pack as-is. Nothing
    // to add here; we just don't remove it the way wood/stone were removed.

    // Steel tier (§8.5) — the new top of the tree now that diamond gear is abolished. Made by blasting
    // iron into steel (recipe). Steel armour reuses the iron equipment look for now (placeholder art).
    private static final TagKey<Item> STEEL_INGOTS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "steel_ingots"));
    private static final ToolMaterial STEEL =
        new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 900, 7.0F, 3.0F, 12, STEEL_INGOTS);
    private static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(
        25,
        Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 6),
        12, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, STEEL_INGOTS, EquipmentAssets.IRON);

    public static final Item STEEL_INGOT = register("steel_ingot",
        key -> new Item(new Item.Properties().setId(key)));
    public static final Item STEEL_SWORD = register("steel_sword",
        key -> new Item(new Item.Properties().sword(STEEL, 3.0F, -2.4F).setId(key)));
    public static final Item STEEL_PICKAXE = register("steel_pickaxe",
        key -> new Item(new Item.Properties().pickaxe(STEEL, 1.0F, -2.8F).setId(key)));
    public static final Item STEEL_AXE = register("steel_axe",
        key -> new Item(new Item.Properties().axe(STEEL, 6.0F, -3.1F).setId(key)));
    /** The top-tier reach weapon (§8.5/§8.6) — completes the spear line (flint → copper → iron → steel).
     *  A steel head on a stout shaft: the vanilla spear thrust/reach/pierce at the pack's best tier. */
    public static final Item STEEL_SPEAR = register("steel_spear",
        key -> new Item(new Item.Properties()
            .spear(STEEL, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F).setId(key)));
    public static final Item STEEL_SHOVEL = register("steel_shovel",
        key -> new Item(new Item.Properties().shovel(STEEL, 1.5F, -3.0F).setId(key)));
    public static final Item STEEL_HOE = register("steel_hoe",
        key -> new Item(new Item.Properties().hoe(STEEL, -2.0F, -1.0F).setId(key)));
    public static final Item STEEL_HELMET = register("steel_helmet",
        key -> new Item(new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.HELMET).setId(key)));
    public static final Item STEEL_CHESTPLATE = register("steel_chestplate",
        key -> new Item(new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.CHESTPLATE).setId(key)));
    public static final Item STEEL_LEGGINGS = register("steel_leggings",
        key -> new Item(new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.LEGGINGS).setId(key)));
    public static final Item STEEL_BOOTS = register("steel_boots",
        key -> new Item(new Item.Properties().humanoidArmor(STEEL_ARMOR, ArmorType.BOOTS).setId(key)));

    /** A raw hide (§7.3) — the wet, green skin taken off a fresh kill when you butcher it with a blade.
     *  It is <b>not leather</b>: untanned, it will only rot and stiffen, so it has no use of its own except
     *  to be <b>tanned</b>. Lay it on a {@link AloneBlocks#TANNING_RACK} with a lump of {@link #ANIMAL_BRAINS}
     *  and leave it for days, and it becomes real leather. This is the whole point — leather is worked, not
     *  a drop. Placeholder art. */
    public static final Item RAW_HIDE = register("raw_hide",
        key -> new Item(new Item.Properties().setId(key)));
    /** Animal brains (§7.3) — the emulsified fatty tissue every mammal carries in its skull, and the classic
     *  brain-tanning agent: the old saw that "a beast has just enough brains to tan its own hide" is literally
     *  true, so butchering a kill yields both its {@link #RAW_HIDE} and the brains to tan it. Its only use is
     *  to be worked into a hide on the {@link AloneBlocks#TANNING_RACK}; consumed by the tanning. Placeholder art. */
    public static final Item ANIMAL_BRAINS = register("animal_brains",
        key -> new Item(new Item.Properties().setId(key)));
    /** A tanning rack (§7.3) — a load-and-leave frame where a {@link #RAW_HIDE} and a lump of
     *  {@link #ANIMAL_BRAINS} are worked into {@code minecraft:leather} over the long, patient days that
     *  tanning really takes (see {@link AloneBlocks#TANNING_RACK}). */
    public static final Item TANNING_RACK = register("tanning_rack",
        key -> new net.minecraft.world.item.BlockItem(AloneBlocks.TANNING_RACK, new Item.Properties().setId(key)));

    /** Touching this class registers the items above; {@link #init()} then adds the creative tab. */
    public static void init() {
        registerCreativeTab();
    }

    /**
     * A single "Alone" creative tab holding every {@code alone:} item — so they're reachable in creative
     * and, crucially, so recipe viewers (JEI/EMI) can list them and show their recipes (those tools build
     * their ingredient list from the creative tabs). Populated by scanning the registry for our namespace,
     * so new items appear automatically.
     */
    private static void registerCreativeTab() {
        CreativeModeTab tab = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(WATERSKIN))
            .title(Component.literal("Alone"))
            .displayItems((params, output) -> {
                for (Item item : BuiltInRegistries.ITEM) {
                    if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("alone")) {
                        output.accept(item);
                    }
                }
            })
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("alone", "items"), tab);
    }

    private static Item register(String path, Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
    }
}
