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
    // Digging earth is brutally slow — a 1x1x2 pit is the better part of a day's labour even with a shovel,
    // and hopeless by hand (§5.4). Packed soil (dirt/clay) is the slog; loose sand/gravel scoops easily.
    private static final float DIRT_FACTOR = 0.015f;   // packed earth WITH a shovel — ~25s a block by wooden shovel
    private static final float CLAY_FACTOR = 0.008f;   // dense clay subsoil with a shovel — the hardest earth (~45s)
    private static final float LOOSE_FACTOR = 0.12f;   // loose sand/gravel with a shovel — scoops quickly
    private static final float NO_SHOVEL_DIG = 0.01f;  // packed earth by hand / wrong tool — near-hopeless (~75s a block)
    private static final float LOOSE_HAND_DIG = 0.15f; // loose sand/gravel by hand — still scoopable (foraging works)
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
