package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Campfire fuel (proposal §3). Every campfire burns fuel and goes out when spent — feed it sticks,
 * logs, or planks (right-click) to keep it going, and its warmth dwindles as the fuel runs down.
 * Fuel is stored on the campfire's block entity; an unfuelled/new campfire starts with a full load.
 */
public final class Campfires {
    private Campfires() {
    }

    public static final int INITIAL_FUEL = 3600;  // ~3 min for a fresh campfire
    public static final int FUEL_PER_STICK = 600;  // ~30s
    public static final int FUEL_PER_LOG = 2400;   // ~2 min
    public static final int MAX_FUEL = 24000;      // ~20 min cap

    public static final AttachmentType<Integer> FUEL =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "campfire_fuel"), Codec.INT);

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.CAMPFIRE) || !state.getValue(BlockStateProperties.LIT)) {
                return InteractionResult.PASS;
            }
            ItemStack held = player.getItemInHand(hand);

            // Boil water: hold a filled waterskin to a lit campfire → clean water + a sterile vessel (§2).
            if (held.getItem() instanceof WaterskinItem && held.getOrDefault(AloneItems.WATER_CHARGES, 0) > 0) {
                if (!level.isClientSide()) {
                    boolean wasSalt = held.getOrDefault(AloneItems.WATER_QUALITY, WaterskinItem.RAW) == WaterskinItem.SALT;
                    held.set(AloneItems.WATER_QUALITY, WaterskinItem.CLEAN);
                    held.set(AloneItems.VESSEL_DIRTY, false);
                    if (wasSalt) {
                        // desalination leaves the salt behind — a useful byproduct for preserving food
                        net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(AloneItems.SALT));
                        player.sendSystemMessage(Component.literal("The seawater boils down to fresh water — and a little salt."));
                    } else {
                        player.sendSystemMessage(Component.literal("The water boils clean."));
                    }
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            int add = fuelValue(held);
            if (add <= 0) {
                return InteractionResult.PASS; // let vanilla handle food (cooking), etc.
            }
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    setFuel(be, Math.min(MAX_FUEL, getFuel(be) + add));
                }
                if (!player.isCreative()) {
                    held.shrink(1);
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        });
    }

    private static int fuelValue(ItemStack stack) {
        if (stack.is(Items.STICK)) {
            return FUEL_PER_STICK;
        }
        if (stack.is(ItemTags.LOGS)) {
            return FUEL_PER_LOG;
        }
        if (stack.is(ItemTags.PLANKS)) {
            return FUEL_PER_LOG / 2;
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
