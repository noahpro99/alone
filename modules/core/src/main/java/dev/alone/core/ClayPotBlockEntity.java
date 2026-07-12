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
    /** ~10s of steady rain per litre caught by a bare pot's own mouth — a patient, weather-dependent thing.
     *  A tarp funnel rigged above catches far faster (its whole area drains into the pot). */
    public static final int FILL_TICKS = 200;
    /** Cap on tarp-funnel catchment, so a huge roof fills fast but not absurdly (litres of rain per tick). */
    private static final int MAX_CATCHMENT = 12;
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
        // How much rain the pot catches this tick: nothing if sheltered/dry, 1 for a bare pot under open
        // sky (its own small mouth), or the number of connected sky-exposed tarp blocks draped above — a
        // funnel, so a tarp roof fills the pot many times faster (real rain-catchment rigging, §2).
        int catchment = rainCatchment(level, pos);
        if (catchment <= 0) {
            if (pot.getAttachedOrElse(FILL, 0) != 0) {
                pot.setAttached(FILL, 0); // the shower stopped (or it's sheltered) — no half-litre banked
            }
            return;
        }
        int fill = pot.getAttachedOrElse(FILL, 0) + catchment;
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

    /** Rain caught per tick: 0 if sheltered/dry, 1 for a bare pot under open sky, or the number of connected
     *  sky-exposed tarp blocks draped above (a funnel), capped — so a tarp roof fills a pot much faster. */
    private static int rainCatchment(Level level, BlockPos pos) {
        // A tarp draped just above funnels its whole catchment into the pot, even though it shelters the
        // pot's own mouth. Scan up for one; a solid non-tarp block above simply shelters the pot (catches 0).
        for (int dy = 1; dy <= 4; dy++) {
            BlockState above = level.getBlockState(pos.above(dy));
            if (above.is(AloneBlocks.TARP)) {
                return Math.min(MAX_CATCHMENT, tarpCatchment(level, pos.above(dy)));
            }
            if (!above.isAir() && !above.canBeReplaced()) {
                return 0; // roofed by something solid that isn't a tarp — no rain reaches the pot
            }
        }
        return level.isRainingAt(pos.above()) ? 1 : 0; // a bare pot catches only from its own small mouth
    }

    /** Connected tarp blocks (flood-filled horizontally from the given one) that rain actually reaches — the
     *  funnel's catchment area, in litres of rain caught per tick. */
    private static int tarpCatchment(Level level, BlockPos start) {
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int catchment = 0;
        while (!queue.isEmpty() && seen.size() <= 64) {
            BlockPos p = queue.poll();
            if (level.isRainingAt(p.above())) {
                catchment++; // this panel of the roof is open to the sky and catching rain
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(AloneBlocks.TARP)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return catchment;
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
