package dev.alone.core.mixin;

import dev.alone.core.VillageDefense;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trampling a village's crops is a crime (§7.2 village defence). Vanilla {@code FarmBlock.fallOn} runs when
 * something lands on farmland (a jump or a fall) — the moment the crop gets stomped. If it's a player coming
 * down on a village's field, we rouse the village exactly as breaking a crop by hand does. A real jump/fall
 * only (a fall distance worth trampling for), so brushing the edge doesn't set the whole settlement on you.
 */
@Mixin(FarmlandBlock.class)
public class FarmBlockTrampleMixin {
    @Inject(method = "fallOn", at = @At("HEAD"))
    private void alone$trampleIsACrime(Level level, BlockState state, BlockPos pos, Entity entity,
                                       double fallDistance, CallbackInfo ci) {
        if (fallDistance > 0.5 && entity instanceof ServerPlayer player && level instanceof ServerLevel serverLevel) {
            VillageDefense.reportTrample(player, serverLevel, pos);
        }
    }
}
