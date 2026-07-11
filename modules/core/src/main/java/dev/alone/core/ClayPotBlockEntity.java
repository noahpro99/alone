package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A set-down clay pot catching rain (proposal §2). While rain can reach it (open sky, rainy biome —
 * {@code isRainingAt} checks both), it slowly fills with <b>clean</b> water: rain is already potable, so
 * unlike lake water it needs no boiling. Pour it into a vessel or drink from it. State (how full, how far
 * into the next litre) lives on attachments; the block/interactions are in {@link ClayPotBlock}.
 */
public class ClayPotBlockEntity extends BlockEntity {
    /** A set-down pot is a small reservoir — more than the hand-carried vessel, less than an iron cauldron. */
    public static final int CAPACITY = 8;
    /** ~10s of steady rain per litre caught — filling a pot is a patient, weather-dependent thing. */
    public static final int FILL_TICKS = 200;
    private static final float THIRST_PER_SIP = 30f;

    /**
     * Progress toward the next litre (rain accumulator), 0..{@link #FILL_TICKS}. Kept on the block entity —
     * it's server-only bookkeeping with nothing to show. The water level itself lives on the blockstate
     * ({@link ClayPotBlock#WATER}) so the client can render the surface; see {@link #waterLevel()}.
     */
    public static final AttachmentType<Integer> FILL = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "clay_pot_fill"), Codec.INT);

    public ClayPotBlockEntity(BlockPos pos, BlockState state) {
        super(AloneBlocks.CLAY_POT_BLOCK_ENTITY, pos, state);
    }

    /** Litres standing in the pot, read from the blockstate (the shared, client-visible source of truth). */
    private int waterLevel() {
        return getBlockState().getValue(ClayPotBlock.WATER);
    }

    /** Set the water level on the blockstate so it saves, syncs to the client, and redraws the surface. */
    private void setWaterLevel(Level level, int water) {
        level.setBlock(getBlockPos(), getBlockState().setValue(ClayPotBlock.WATER, water), Block.UPDATE_ALL);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ClayPotBlockEntity pot) {
        int water = state.getValue(ClayPotBlock.WATER);
        if (water >= CAPACITY) {
            return;
        }
        if (!level.isRainingAt(pos.above())) {
            if (pot.getAttachedOrElse(FILL, 0) != 0) {
                pot.setAttached(FILL, 0); // the shower stopped before a full litre — no half-measures banked
            }
            return;
        }
        int fill = pot.getAttachedOrElse(FILL, 0) + 1;
        if (fill >= FILL_TICKS) {
            pot.setAttached(FILL, 0);
            level.setBlock(pos, state.setValue(ClayPotBlock.WATER, water + 1), Block.UPDATE_ALL);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5,
                    4, 0.18, 0.02, 0.18, 0.0);
            }
        } else {
            pot.setAttached(FILL, fill);
        }
    }

    /** Pour clean rainwater from the pot into a held vessel (§2). Won't mix into raw/salt water already in it. */
    public InteractionResult fillVessel(Level level, Player player, ItemStack stack, WaterskinItem vessel) {
        int water = waterLevel();
        if (water <= 0) {
            return InteractionResult.PASS;
        }
        int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
        int quality = stack.getOrDefault(AloneItems.WATER_QUALITY, WaterskinItem.RAW);
        if (charges > 0 && quality != WaterskinItem.CLEAN) {
            return InteractionResult.PASS; // don't dilute/mix dirty water — empty or drink it first
        }
        int room = vessel.maxCharges() - charges;
        if (room <= 0) {
            return InteractionResult.PASS;
        }
        int amount = Math.min(room, water);
        if (!level.isClientSide()) {
            stack.set(AloneItems.WATER_CHARGES, charges + amount);
            stack.set(AloneItems.WATER_QUALITY, WaterskinItem.CLEAN);
            stack.set(AloneItems.VESSEL_DIRTY, false); // clean rainwater rinses the vessel clean
            setWaterLevel(level, water - amount);
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    /** Drink a clean sip straight from the pot. */
    public InteractionResult drink(Level level, Player player) {
        int water = waterLevel();
        if (water <= 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            SurvivalMeters.drink(player, THIRST_PER_SIP);
            setWaterLevel(level, water - 1);
            level.playSound(null, getBlockPos(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }
}
