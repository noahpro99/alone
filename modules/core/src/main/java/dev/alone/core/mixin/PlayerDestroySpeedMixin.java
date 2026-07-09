package dev.alone.core.mixin;

import dev.alone.core.AloneCore;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    private static final float DIRT_FACTOR = 0.08f;    // earth is quicker than stone but still work
    private static final float LEAVES_HAND_FACTOR = 0.08f;  // tearing foliage by hand is slow, tugging work
    private static final float LEAVES_BLADE_FACTOR = 0.3f;  // an axe/hoe shears through it quicker
    private static final float MAX_BREAK_DIVISOR = 40f; // caps the worst-case time (~60s)

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void alone$destroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;

        if (state.is(BlockTags.LOGS)) {
            switch (AloneCore.classifyForLog(self.getMainHandItem())) {
                case AXE -> cir.setReturnValue(cir.getReturnValueF() * LOG_AXE_FACTOR);
                case CRUDE -> cir.setReturnValue(LOG_CRUDE_SPEED);
                case NONE -> cir.setReturnValue(0.0f);
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
            // Digging a cubic metre of earth is still real work, even with a shovel (§5.4).
            cir.setReturnValue(cir.getReturnValueF() * DIRT_FACTOR);
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
