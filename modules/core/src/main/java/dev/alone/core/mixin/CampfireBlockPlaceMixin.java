package dev.alone.core.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A campfire is laid, not lit (proposal §3.1). You craft and place the unlit fire from sticks + fibre;
 * lighting it takes the friction drill (see {@code FireStarting}). So every placed campfire starts
 * <b>unlit</b> — no free flame from the crafting grid.
 */
@Mixin(CampfireBlock.class)
public class CampfireBlockPlaceMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void alone$placeUnlit(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && state.hasProperty(BlockStateProperties.LIT)) {
            cir.setReturnValue(state.setValue(BlockStateProperties.LIT, Boolean.FALSE));
        }
    }
}
