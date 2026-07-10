package dev.alone.core.mixin;

import dev.alone.core.AloneCore;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Honest break times (proposal §5.4). Rewrites the player's dig speed:
 * <ul>
 *   <li><b>Logs</b> need the right tool and chop slowly — an axe works, a crude blade barely, a
 *       bare hand not at all.</li>
 *   <li><b>Stone / ore</b> is real quarrying work — tens of seconds, not a fraction (with a
 *       worst-case cap so obsidian-tier blocks stay brutal but finite).</li>
 *   <li><b>Packed earth</b> (dirt/clay) is real excavation — ~3 in-game min a block with a shovel,
 *       near-hopeless by hand — calibrated to the 72x day/night time compression. <b>Loose</b> sand
 *       and gravel still scoop fast, and plants stay vanilla-fast.</li>
 * </ul>
 * Creative instabreak bypasses all of this.
 */
@Mixin(Player.class)
public class PlayerDestroySpeedMixin {
    private static final float LOG_AXE_FACTOR = 0.04f; // an axe: slow but functional
    private static final float LOG_CRUDE_SPEED = 0.1f; // a sword/pickaxe: a real slog
    private static final float STONE_FACTOR = 0.02f;   // ~50x slower quarrying
    // Calibrated to Minecraft's time compression: a 20-min day stands in for 24 real hours, so real
    // time reads at 72x (1 real hour ≈ 50 in-game seconds). Real continuous excavation of 1 m³ of
    // average soil is ~2.5–5 h, i.e. ~125–250s a block at 72x. So packed earth with a wooden shovel
    // lands at ~190s (~3 in-game min); bare-handed it's ~500s, near-hopeless. Loose sand/gravel stays
    // deliberately quick (granular, and foraging flint from gravel must remain viable). (§5.4)
    private static final float DIRT_FACTOR = 0.002f;    // packed earth WITH a shovel — ~190s (2.5–5h real @72x)
    private static final float CLAY_FACTOR = 0.0016f;   // dense clay subsoil with a shovel — hardest earth (~280s)
    // Loose sand/gravel is granular so it's quicker than packed earth, but a whole cubic metre is still
    // real labour: ~1–1.5h continuous, i.e. ~60–75s a block at 72x. Flint isn't gated behind fully
    // clearing a gravel block, though — it shakes loose part-way through (see ServerPlayerGameModeMixin).
    private static final float LOOSE_FACTOR = 0.006f;   // loose sand/gravel with a shovel — ~60–75s a block
    private static final float NO_SHOVEL_DIG = 0.0015f; // packed earth by hand / wrong tool — ~500s, near-hopeless
    private static final float LOOSE_HAND_DIG = 0.008f; // loose sand/gravel by hand — ~90–110s, scoopable but slow
    private static final float LEAVES_HAND_FACTOR = 0.08f;  // tearing foliage by hand is slow, tugging work
    private static final float LEAVES_BLADE_FACTOR = 0.3f;  // an axe/hoe shears through it quicker
    private static final float MAX_BREAK_DIVISOR = 40f; // caps the worst-case time (~60s)

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void alone$destroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;

        if (state.is(BlockTags.LOGS)) {
            // if/else, not switch: a switch on an enum makes the compiler emit a synthetic inner class
            // ($SwitchMap), which mixin then has to relocate into Player and can choke on — avoid it.
            AloneCore.LogTool tool = AloneCore.classifyForLog(self.getMainHandItem());
            if (tool == AloneCore.LogTool.AXE) {
                cir.setReturnValue(cir.getReturnValueF() * LOG_AXE_FACTOR);
            } else if (tool == AloneCore.LogTool.CRUDE) {
                cir.setReturnValue(LOG_CRUDE_SPEED);
            } else {
                cir.setReturnValue(0.0f);
            }
            return;
        }

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            float hardness = state.getDestroySpeed(self.level(), BlockPos.ZERO);
            float slowed = cir.getReturnValueF() * STONE_FACTOR;
            float floor = hardness > 0f ? hardness / MAX_BREAK_DIVISOR : 0f;
            cir.setReturnValue(Math.max(slowed, floor));
            return;
        }

        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            boolean packed = state.is(BlockTags.DIRT) || state.is(Blocks.CLAY); // packed soil / dense subsoil
            if (!self.getMainHandItem().is(ItemTags.SHOVELS)) {
                // No shovel — a FIXED speed (not scaled by the held item), so a pot or an axe is never
                // faster than bare hands. Loose sand/gravel still scoops by hand; packed earth barely
                // yields, so you can't claw a shelter out of the ground without the right tool (§5.4).
                cir.setReturnValue(packed ? NO_SHOVEL_DIG : LOOSE_HAND_DIG);
                return;
            }
            // With a shovel it's still heavy work — packed earth slow, dense clay subsoil slowest, loose
            // sand/gravel quick.
            float factor = state.is(Blocks.CLAY) ? CLAY_FACTOR : (packed ? DIRT_FACTOR : LOOSE_FACTOR);
            cir.setReturnValue(cir.getReturnValueF() * factor);
            return;
        }

        if (state.is(BlockTags.LEAVES)) {
            // Tearing through dense foliage is slow, tugging work; a blade (axe/hoe) shears it quicker.
            ItemStack hand = self.getMainHandItem();
            float factor = (hand.is(ItemTags.AXES) || hand.is(ItemTags.HOES))
                ? LEAVES_BLADE_FACTOR : LEAVES_HAND_FACTOR;
            cir.setReturnValue(cir.getReturnValueF() * factor);
            return;
        }
        // Everything else (plants, glass, wool, …) stays vanilla-fast.
    }
}
