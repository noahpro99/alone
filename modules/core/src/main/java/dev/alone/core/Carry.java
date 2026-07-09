package dev.alone.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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

    /** Hard volume cap on the person, in m³. (~0.3 is closer to a real pack; 1.0 per request — tunable.) */
    public static final float PLAYER_VOLUME_LIMIT = 1.0f;

    /** Storage volume per slot: a 27-slot chest = 1 m³, so a double chest = 2 m³, a barrel = 1, etc. */
    public static final float STORAGE_VOLUME_PER_SLOT = 1.0f / 27f;

    private static final float FREE_WEIGHT = 8f;    // kg carried with no movement penalty
    private static final float MAX_WEIGHT = 45f;    // kg where you're at the slowest crawl
    private static final float MIN_SPEED_FACTOR = 0.35f;

    private static final Identifier CARRY_MODIFIER = Identifier.fromNamespaceAndPath("alone", "carry_weight");

    private static final EquipmentSlot[] ARMOR_SLOTS =
        {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Per-item volume is deterministic per item type — compute the block shape once and cache it. */
    private static final Map<Item, Float> VOLUME_CACHE = new ConcurrentHashMap<>();

    public static float itemVolume(ItemStack stack) {
        return perItemVolume(stack.getItem()) * stack.getCount();
    }

    /** Volume (m³) of a single item of this stack's type. */
    public static float unitVolume(ItemStack stack) {
        return perItemVolume(stack.getItem());
    }

    public static float itemWeight(ItemStack stack) {
        return perItemWeight(stack.getItem()) * stack.getCount();
    }

    private static float perItemVolume(Item item) {
        return VOLUME_CACHE.computeIfAbsent(item, Carry::computeVolume);
    }

    /**
     * A substantial block takes the space it occupies when placed — a full cube is 1 m³, a slab 0.5.
     * But many "block" items are really small placed things (seeds place crops, saplings, torches,
     * flowers, carpets) — those fall through to the small-item tiers, so a seed isn't a block's weight.
     */
    private static float computeVolume(Item item) {
        if (item instanceof BlockItem blockItem) {
            float shape = blockShapeVolume(blockItem);
            if (shape >= 0.4f) {
                return Math.min(1.0f, shape); // a real block
            }
            // else: a small placed thing — treat like any small item below
        }
        ItemStack def = item.getDefaultInstance();
        if (def.isDamageableItem()) {
            return 0.02f; // tools, weapons, armour
        }
        if (def.has(DataComponents.FOOD)) {
            return 0.012f; // a portion of food
        }
        return 0.003f; // seeds, dust, bones… (a stack of 64 ≈ 0.19 m³)
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

    /** Weight (kg). Coarse tiers — real blocks scale with volume, tools moderate, food a portion, rest light. */
    private static float perItemWeight(Item item) {
        if (item instanceof BlockItem && perItemVolume(item) >= 0.4f) {
            return perItemVolume(item) * 30f; // a full block ≈ 30 kg; a slab ≈ 15
        }
        ItemStack def = item.getDefaultInstance();
        if (def.isDamageableItem()) {
            return 1.5f; // tools, weapons, armour
        }
        if (def.has(DataComponents.FOOD)) {
            return 0.35f; // a portion of food (a steak, a loaf) — a stack of 64 ≈ 22 kg
        }
        return 0.05f; // seeds and other small items
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
    public static float volumeLimit(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(AloneItems.BACKPACK)) {
                return PLAYER_VOLUME_LIMIT + 1.5f; // a backpack adds ~1.5 m³
            }
        }
        return PLAYER_VOLUME_LIMIT;
    }

    /** Volume cap for a container, or {@link Float#MAX_VALUE} for uncapped (functional) containers. */
    public static float containerVolumeLimit(Container container) {
        if (container instanceof Inventory inventory) {
            return volumeLimit(inventory.player);
        }
        if (container instanceof RandomizableContainerBlockEntity || container instanceof CompoundContainer) {
            return container.getContainerSize() * STORAGE_VOLUME_PER_SLOT; // chests, barrels, shulkers…
        }
        return Float.MAX_VALUE; // furnaces, crafting, brewing, etc.
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
