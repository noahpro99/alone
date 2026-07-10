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
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
    /** A placeable bedroll — a real bed block that reads as a flat mat (see {@link AloneBlocks#BEDROLL}). */
    public static final Item BEDROLL = register("bedroll",
        key -> new BedItem(AloneBlocks.BEDROLL, new Item.Properties().stacksTo(1).setId(key)));
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

    /** Rope coil (§5.7) — throw it down a cliff face to hang a free, safe climb line. */
    public static final Item ROPE = register("rope",
        key -> new RopeItem(new Item.Properties().stacksTo(64).setId(key)));

    /** Plant fiber (§8.1) — stripped from grass/ferns/vines; twist a few into string (no spiders needed). */
    public static final Item PLANT_FIBER = register("plant_fiber",
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

    // Primitive knapped tools (§8.1) — a flint tier between wood and stone: low durability, workable.
    private static final ToolMaterial FLINT =
        new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 48, 2.5F, 1.0F, 3, ItemTags.STONE_TOOL_MATERIALS);

    public static final Item FLINT_HATCHET = register("flint_hatchet",
        key -> new AxeItem(FLINT, 6.0F, -3.2F, new Item.Properties().setId(key)));
    public static final Item FLINT_PICK = register("flint_pick",
        key -> new Item(new Item.Properties().pickaxe(FLINT, 1.0F, -2.8F).setId(key)));
    public static final Item FLINT_KNIFE = register("flint_knife",
        key -> new Item(new Item.Properties().sword(FLINT, 2.0F, -2.0F).setId(key)));
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
