package dev.alone.core;

import java.util.function.Function;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
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
        key -> new Item(new Item.Properties().stacksTo(1).setId(key)));
    /** A placeable bedroll — a real bed block that reads as a flat mat (see {@link AloneBlocks#BEDROLL}). */
    public static final Item BEDROLL = register("bedroll",
        key -> new BedItem(AloneBlocks.BEDROLL, new Item.Properties().stacksTo(1).setId(key)));
    public static final Item WATERSKIN = register("waterskin",
        key -> new WaterskinItem(new Item.Properties().stacksTo(1).setId(key), 3));
    /** Iron pot (§2 vessel ladder) — holds far more water than a skin; the workhorse vessel. */
    public static final Item IRON_POT = register("iron_pot",
        key -> new WaterskinItem(new Item.Properties().stacksTo(1).setId(key), 6));

    /** Salt (§2/§4.2) — boil seawater to get it; use it to preserve food for winter. */
    public static final Item SALT = register("salt",
        key -> new Item(new Item.Properties().setId(key)));

    /** Splint (§1.5 medicine) — sticks bound with cord; sneak + right-click to treat a sprain. */
    public static final Item SPLINT = register("splint",
        key -> new Item(new Item.Properties().setId(key)));

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

    // Primitive knapped tools (§8.1) — a flint tier between wood and stone: low durability, workable.
    private static final ToolMaterial FLINT =
        new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 48, 2.5F, 1.0F, 3, ItemTags.STONE_TOOL_MATERIALS);

    public static final Item FLINT_HATCHET = register("flint_hatchet",
        key -> new AxeItem(FLINT, 6.0F, -3.2F, new Item.Properties().setId(key)));
    public static final Item FLINT_PICK = register("flint_pick",
        key -> new Item(new Item.Properties().pickaxe(FLINT, 1.0F, -2.8F).setId(key)));
    public static final Item FLINT_KNIFE = register("flint_knife",
        key -> new Item(new Item.Properties().sword(FLINT, 2.0F, -2.0F).setId(key)));

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

    /** Touching this class registers the items above. (Obtain them with {@code /give alone:waterskin}, etc.,
     *  or add them to a creative tab once the item-group API / textures are in.) */
    public static void init() {
    }

    private static Item register(String path, Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
    }
}
