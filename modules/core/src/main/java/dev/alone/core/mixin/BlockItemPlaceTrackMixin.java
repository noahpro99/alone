package dev.alone.core.mixin;

import dev.alone.core.PlacedBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remember blocks a player sets down (see {@link PlacedBlocks}) so they read as "loose" — quick to pick
 * back up — rather than rooted natural terrain.
 */
@Mixin(BlockItem.class)
public class BlockItemPlaceTrackMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void alone$trackPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (result != InteractionResult.PASS && result != InteractionResult.FAIL && context.getPlayer() != null) {
            PlacedBlocks.markPlaced(context.getLevel(), context.getClickedPos());
        }
    }
}
