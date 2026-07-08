package dev.alone.core.mixin;

import dev.alone.core.Campfires;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Burns a lit campfire's fuel down each tick (proposal §3). When it's spent, the campfire goes out;
 * clearing the attachment lets it start fresh if re-lit. {@code cookTick} is the lit-state ticker.
 */
@Mixin(CampfireBlockEntity.class)
public class CampfireCookTickMixin {

    @Inject(method = "cookTick", at = @At("HEAD"))
    @SuppressWarnings("rawtypes")
    private static void alone$burnFuel(ServerLevel level, BlockPos pos, BlockState state,
                                       CampfireBlockEntity entity, RecipeManager.CachedCheck cache, CallbackInfo ci) {
        int fuel = Campfires.getFuel(entity);
        if (fuel <= 0) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            entity.removeAttached(Campfires.FUEL);
        } else {
            Campfires.setFuel(entity, fuel - 1);
        }
    }
}
