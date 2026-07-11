package dev.alone.core.mixin;

import dev.alone.core.Carry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rivers as highways (proposal §6). Wood and light goods dropped in <b>flowing</b> water ride the current
 * downstream on their own — a log drive: fell your timber upstream, roll it in, and let the river carry it
 * to your build site instead of hauling every heavy log overland (which the weight system, §5.1, makes
 * brutal). Heavy/dense things (metal, stone, full blocks) don't catch the current; still pools don't carry.
 * We steer a floating item toward a gentle river pace along the flow, so it drifts steadily, never rockets.
 */
@Mixin(ItemEntity.class)
public class ItemEntityFlowMixin {
    private static final double RIVER_SPEED = 0.12; // blocks/tick a well-floating log drifts downstream

    @Inject(method = "tick", at = @At("TAIL"))
    private void alone$driftDownstream(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide() || !self.isInWater()) {
            return;
        }
        BlockPos pos = self.blockPosition();
        Vec3 flow = level.getFluidState(pos).getFlow(level, pos);
        if (flow.lengthSqr() < 1.0e-4) {
            return; // a still pool has no current to catch
        }
        float ride = alone$rideFactor(self.getItem());
        if (ride <= 0f) {
            return; // too heavy/dense to be carried
        }
        Vec3 target = flow.normalize().scale(RIVER_SPEED * ride);
        Vec3 d = self.getDeltaMovement();
        // Ease the horizontal velocity toward the river's pace (a steady drift, capped by the target speed).
        self.setDeltaMovement(d.x + (target.x - d.x) * 0.2, d.y, d.z + (target.z - d.z) * 0.2);
    }

    /** How well an item rides the current: wood floats and drives well, light goods drift, heavy things don't. */
    private static float alone$rideFactor(ItemStack stack) {
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) {
            return 1.0f; // timber — the whole point of a log drive
        }
        float weight = Carry.unitWeight(stack);
        if (weight >= 2.5f) {
            return 0f; // heavy/dense (blocks, metal, big tools) — sinks out of the current
        }
        return Math.max(0f, 1f - weight / 2.5f); // lighter goods ride better
    }
}
