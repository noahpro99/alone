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
 *   <li><b>Dirt / sand / gravel</b> and plants stay vanilla-fast — the ground cooperates.</li>
 * </ul>
 * Creative instabreak bypasses all of this.
 */
@Mixin(Player.class)
public class PlayerDestroySpeedMixin {
    private static final float LOG_AXE_FACTOR = 0.04f; // an axe: slow but functional
    private static final float LOG_CRUDE_SPEED = 0.1f; // a sword/pickaxe: a real slog
    private static final float STONE_FACTOR = 0.02f;   // ~50x slower quarrying
    private static final float DIRT_FACTOR = 0.08f;    // earth with a SHOVEL — quicker than stone but still work
    private static final float CLAY_FACTOR = 0.04f;    // dense clay subsoil, with a shovel — twice the toil of loose earth
    private static final float NO_SHOVEL_DIG = 0.03f;  // hands or the wrong tool — a brutal, fixed-slow scrape (no easy dugout)
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
            if (!self.getMainHandItem().is(ItemTags.SHOVELS)) {
                // No shovel — you can't claw a shelter out of the earth with bare hands (or a pot, or an
                // axe). A fixed, very slow speed, NOT scaled by whatever you're holding, so a pot/axe is
                // never faster than your hands, and a dugout without the right tool is a brutal slog (§5.4).
                cir.setReturnValue(NO_SHOVEL_DIG);
                return;
            }
            // With a shovel it's real work, and dense clay subsoil is the hardest of it.
            float factor = state.is(Blocks.CLAY) ? CLAY_FACTOR : DIRT_FACTOR;
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
