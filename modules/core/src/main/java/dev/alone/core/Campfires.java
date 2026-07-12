package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Campfire fuel + cooking-pot boiling (proposal §2/§3). A campfire burns fuel and goes out when spent —
 * feed it sticks, logs, planks, fibre, or leaves (right-click) to keep it going; warmth and smoke dwindle
 * as the fuel runs down (see {@code CampfireCookTickMixin}). And you <b>set a fire-safe pot on it to
 * boil</b>: right-click a lit campfire with a filled clay/iron pot to stand it in the flames, wait while
 * it comes to a boil, then right-click empty-handed to lift it back off — clean and safe (§2). Salt water
 * won't boil clean (that needs a still). Fuel and the boiling pot both live on the campfire's block entity.
 */
public final class Campfires {
    private Campfires() {
    }

    public static final int INITIAL_FUEL = 3600;  // ~3 min for a fresh campfire
    public static final int FUEL_PER_STICK = 600;  // ~30s
    public static final int FUEL_PER_LOG = 2400;   // ~2 min
    public static final int MAX_FUEL = 24000;      // ~20 min cap
    public static final int BOIL_TIME = 300;       // ~15s on the fire to bring a pot to a rolling boil

    public static final AttachmentType<Integer> FUEL =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "campfire_fuel"), Codec.INT);
    /** The pot currently standing in the fire, boiling. */
    public static final AttachmentType<ItemStack> BOIL_ITEM =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "boil_item"), ItemStack.CODEC);
    /** Ticks of boiling left before it's clean; only counts down while the fire is lit. */
    public static final AttachmentType<Integer> BOIL_LEFT =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "boil_left"), Codec.INT);

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.CAMPFIRE)) {
                return InteractionResult.PASS;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                return InteractionResult.PASS;
            }
            boolean lit = state.getValue(BlockStateProperties.LIT);
            ItemStack held = player.getItemInHand(hand);

            // Lift a pot back off the fire (plain empty hand — sneak+empty is the ember scoop, see Embers).
            if (held.isEmpty() && !player.isShiftKeyDown() && be.hasAttached(BOIL_ITEM)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    retrievePot(sp, level, pos, be);
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // Stand a filled fire-safe pot in the flames to boil (only on a lit fire).
            if (lit && held.getItem() instanceof WaterskinItem vessel
                && held.getOrDefault(AloneItems.WATER_CHARGES, 0) > 0) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    placePot(sp, level, pos, be, vessel, held);
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // Feed the fire (only a lit one).
            if (lit) {
                int add = fuelValue(held);
                if (add <= 0) {
                    return InteractionResult.PASS; // let vanilla handle food/pottery cooking, etc.
                }
                if (!level.isClientSide()) {
                    int fuel = Math.min(MAX_FUEL, getFuel(be) + add);
                    setFuel(be, fuel);
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    // No furnace menu — you read a fire by eye. But a quick word on how it's burning tells
                    // you roughly how long it'll last, the way a glance at the flames and coals would.
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal(fireStateMessage(fuel)), true);
                    }
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private static void placePot(ServerPlayer player, Level level, BlockPos pos, BlockEntity be,
                                 WaterskinItem vessel, ItemStack held) {
        if (be.hasAttached(BOIL_ITEM)) {
            player.sendSystemMessage(Component.literal("There's already a pot on the fire."), true);
            return;
        }
        int quality = held.getOrDefault(AloneItems.WATER_QUALITY, WaterskinItem.RAW);
        if (quality == WaterskinItem.CLEAN) {
            return; // already safe
        }
        if (!vessel.isFireSafe()) {
            player.sendSystemMessage(Component.literal(
                "A hide waterskin can't go over the flame — boil in a pot, or fill from clean rain."), true);
            return;
        }
        ItemStack pot = held.copy();
        pot.setCount(1);
        be.setAttached(BOIL_ITEM, pot);
        be.setAttached(BOIL_LEFT, BOIL_TIME);
        be.setChanged();
        held.shrink(1); // it's standing in the fire now
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.2f);
        // Seawater won't drink boiled — but boiled to dryness it leaves salt (§2). Fresh water just purifies.
        player.sendSystemMessage(Component.literal(quality == WaterskinItem.SALT
            ? "You set the seawater to boil down for salt."
            : "You stand the pot in the fire to boil."), true);
    }

    /** Credit a completed boil onto a pot dropped without a player (the fire burned out before it was
     *  lifted): clean the fresh water, or — for seawater boiled dry — empty it and drop a salt crust at
     *  the fire. Only call when the boil actually finished (BOIL_LEFT ≤ 0). Mirrors {@link #retrievePot}. */
    public static void finishBoiledPot(Level level, BlockPos pos, ItemStack pot) {
        if (pot.getOrDefault(AloneItems.WATER_QUALITY, WaterskinItem.RAW) == WaterskinItem.SALT) {
            int charges = pot.getOrDefault(AloneItems.WATER_CHARGES, 0);
            int salt = Math.max(1, charges / 2);
            pot.set(AloneItems.WATER_CHARGES, 0);
            pot.remove(AloneItems.WATER_QUALITY);
            net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(AloneItems.SALT, salt));
        } else {
            pot.set(AloneItems.WATER_QUALITY, WaterskinItem.CLEAN);
            pot.set(AloneItems.VESSEL_DIRTY, false);
        }
    }

    private static void retrievePot(ServerPlayer player, Level level, BlockPos pos, BlockEntity be) {
        ItemStack pot = be.getAttached(BOIL_ITEM);
        int left = be.getAttachedOrElse(BOIL_LEFT, BOIL_TIME);
        be.removeAttached(BOIL_ITEM);
        be.removeAttached(BOIL_LEFT);
        be.setChanged();
        if (pot == null) {
            return;
        }
        if (left <= 0) {
            if (pot.getOrDefault(AloneItems.WATER_QUALITY, WaterskinItem.RAW) == WaterskinItem.SALT) {
                // Seawater boiled to dryness — the water's gone as steam, a crust of salt left behind.
                int charges = pot.getOrDefault(AloneItems.WATER_CHARGES, 0);
                int salt = Math.max(1, charges / 2); // a potful of seawater yields a modest handful
                pot.set(AloneItems.WATER_CHARGES, 0);
                pot.remove(AloneItems.WATER_QUALITY); // emptied — scrape the salt out and it's reusable
                ItemStack saltStack = new ItemStack(AloneItems.SALT, salt);
                if (!player.getInventory().add(saltStack)) {
                    player.drop(saltStack, false);
                }
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.6f);
                player.sendSystemMessage(Component.literal(
                    "The seawater boils away to a crust of salt (" + salt + ")."), true);
            } else {
                pot.set(AloneItems.WATER_QUALITY, WaterskinItem.CLEAN);
                pot.set(AloneItems.VESSEL_DIRTY, false);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7f, 1.3f);
                player.sendSystemMessage(Component.literal("The water has boiled clean and safe."), true);
            }
        } else {
            player.sendSystemMessage(Component.literal("It hasn't come to a boil yet — leave it longer."), true);
        }
        if (!player.getInventory().add(pot)) {
            player.drop(pot, false);
        }
    }

    public static final int FUEL_PER_FIBER = 150; // dry tinder flares fast — a few seconds
    public static final int FUEL_PER_LEAF = 100;

    /** A word on how a fire's burning, read from its remaining fuel — roaring down to guttering, with a
     *  rough time. The kind of judgement you'd make glancing at the flames and coals, not a furnace gauge. */
    private static String fireStateMessage(int fuel) {
        int minutes = Math.max(1, Math.round(fuel / 1200f)); // 1200 ticks = 1 minute
        String state = fuel > 12000 ? "roaring" : fuel > 4800 ? "burning steadily"
            : fuel > 1200 ? "burning low" : "guttering";
        return "The fire is " + state + " — about " + minutes + " min of fuel left.";
    }

    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.STICK)) {
            return FUEL_PER_STICK;
        }
        if (stack.is(ItemTags.LOGS)) {
            return FUEL_PER_LOG;
        }
        if (stack.is(ItemTags.PLANKS)) {
            return FUEL_PER_LOG / 2;
        }
        if (stack.is(AloneItems.PLANT_FIBER)) {
            return FUEL_PER_FIBER; // kindling/tinder — burns quick, keeps a fire alive in a pinch
        }
        if (stack.is(Items.LEAF_LITTER) || stack.is(ItemTags.LEAVES)) {
            return FUEL_PER_LEAF;
        }
        return 0;
    }

    public static int getFuel(BlockEntity be) {
        return be.getAttachedOrElse(FUEL, INITIAL_FUEL); // uninitialised = a fresh, full campfire
    }

    public static void setFuel(BlockEntity be, int fuel) {
        be.setAttached(FUEL, fuel);
        be.setChanged();
    }
}
