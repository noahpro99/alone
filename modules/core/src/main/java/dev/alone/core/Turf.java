package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

/**
 * De-turfing (proposal §5.4). Breaking a grass block isn't excavation — it's clearing the turf. So a
 * grass block doesn't pop into a dirt <em>drop</em>; it's stripped to <b>bare dirt in place</b>, and the
 * packed earth beneath still has to be dug out separately (at the dirt rate). The clearing itself is real
 * labour — see {@code PlayerDestroySpeedMixin} for the de-turfing time (laborious by hand, quicker with a
 * hoe or shovel). Two honest stages — clear the grass, then dig the soil — instead of one free pop.
 */
public final class Turf {
    private Turf() {
    }

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (player.isCreative() || !state.is(Blocks.GRASS_BLOCK)) {
                return true; // creative and every other block break as normal
            }
            if (!level.isClientSide()) {
                // Grab the grass break sound before we swap the state, then leave bare dirt behind — no
                // drop, since clearing turf gives you cleared ground, not a spare block of soil.
                var breakSound = state.getSoundType().getBreakSound();
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                level.playSound(null, pos, breakSound, SoundSource.BLOCKS, 0.8f, 0.9f);
            }
            return false; // cancel the real break — the grass is cleared to dirt, nothing drops
        });
    }
}
