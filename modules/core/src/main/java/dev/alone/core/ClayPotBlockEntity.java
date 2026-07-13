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
    /** Rain-ticks to catch one litre through a BARE pot's own tiny mouth — ~50s of steady rain per litre, so
     *  a bare pot (its ~0.02 m² opening) is slow: ~7 min of rain to fill. A tarp funnel above is the real way
     *  to collect. (An inch of rain really does take hours; this is that, compressed for play.) */
    public static final int FILL_TICKS = 1000;
    /** How much rain a single sky-exposed 1 m² tarp panel funnels vs the bare pot's mouth (~a 1 m² sheet
     *  catches ~50x a pot's opening in reality; toned to ~10x for play). So one tarp fills a pot in ~40s. */
    private static final int TARP_UNITS = 10;
    /** Cap on a pot's per-tick catchment, so a huge roof fills fast but not instantly. */
    private static final int MAX_CATCHMENT = 40;
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

    /** Rain caught per tick: 0 if sheltered/dry, 1 for a bare pot's own mouth under open sky, this pot's
     *  <b>share</b> of a tarp roof draped above it, or a fraction of that mouth-catch under a porous leaf
     *  canopy (rain drips through — half at ~3 leaf layers, exponential falloff; see {@link Canopy}). */
    private static int rainCatchment(Level level, BlockPos pos) {
        // A tarp draped just above funnels its whole catchment into the pot, even though it shelters the
        // pot's own mouth. Scan up for one; leaves are porous (fall through to the canopy model below), and
        // a solid non-tarp block above simply shelters the pot (catches 0).
        for (int dy = 1; dy <= 4; dy++) {
            BlockState above = level.getBlockState(pos.above(dy));
            if (above.is(AloneBlocks.TARP)) {
                return roofShare(level, pos.above(dy));
            }
            if (above.is(net.minecraft.tags.BlockTags.LEAVES)) {
                break; // a porous canopy, not a hard roof — let the leaf model attenuate the catch
            }
            if (!above.isAir() && !above.canBeReplaced()) {
                return 0; // roofed by something solid that isn't a tarp — no rain reaches the pot
            }
        }
        // A bare pot catches from its own small mouth; a leaf canopy overhead lets only a fraction through.
        // Drip probabilistically so the sub-litre-per-tick rate averages out over the long fill.
        float exposure = Canopy.rainExposure(level, pos);
        if (exposure <= 0f) {
            return 0;
        }
        return level.getRandom().nextFloat() < exposure ? 1 : 0;
    }

    /** One pot's share of a tarp roof: flood-fill the connected tarp, total the rain it catches
     *  (sky-exposed panels × {@link #TARP_UNITS}), then <b>split that among every pot sheltered beneath the
     *  roof</b> — three pots under one tarp each get a third. Capped per pot. */
    private static int roofShare(Level level, BlockPos start) {
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> pots = new java.util.HashSet<>();
        seen.add(start);
        queue.add(start);
        int exposed = 0;
        while (!queue.isEmpty() && seen.size() <= 256) {
            BlockPos p = queue.poll();
            if (level.isRainingAt(p.above())) {
                exposed++; // this panel of the roof is open to the sky and catching rain
            }
            // Every pot in the sheltered space below this panel draws from the same roof.
            for (int dy = 1; dy <= 4; dy++) {
                BlockState below = level.getBlockState(p.below(dy));
                if (below.is(AloneBlocks.CLAY_POT) || below.is(AloneBlocks.IRON_POT)) {
                    pots.add(p.below(dy));
                } else if (below.is(AloneBlocks.TARP) || (!below.isAir() && !below.canBeReplaced())) {
                    break; // another tarp layer, or a solid floor — stop scanning down this column
                }
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(AloneBlocks.TARP)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        int share = (exposed * TARP_UNITS) / Math.max(1, pots.size());
        return Math.min(MAX_CATCHMENT, share);
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
