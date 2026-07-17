package dev.alone.core.mixin;

import dev.alone.core.Construction;
import dev.alone.core.SurvivalMeters;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Building is real labour (proposal §5.5): heaving a log or block of stone into a wall costs wind, and when
 * you're spent you can't lift another until you rest. The construction — not the felling — is what makes a
 * real shelter a multi-day job in the show <em>Alone</em>; the mod already times the woodcutting, this paces
 * the stacking. Gated on the shared {@link SurvivalMeters stamina} system: place a few heavy timbers, blow,
 * rest, repeat, instead of a wall going up as fast as you can click. Only heavy structural blocks
 * ({@link Construction#TIRING_TO_PLACE}) count — torches, tools, workstations and thatch place freely.
 * Server-authoritative; creative is exempt.
 */
@Mixin(BlockItem.class)
public class BlockItemStaminaMixin {
    /** Too winded to lift it? Then you can't place it — rest first. Blocks the placement outright. */
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void alone$gateHeavyPlacement(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return; // instanceof ServerPlayer implies the server side; creative is exempt
        }
        Block block = ((BlockItem) (Object) this).getBlock();
        if (block.defaultBlockState().is(Construction.TIRING_TO_PLACE)
                && SurvivalMeters.getStamina(player) < Construction.PLACE_STAMINA) {
            player.sendSystemMessage(
                Component.literal("You're too winded to heave it into place — catch your breath first."), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    /** A heavy block that actually went down costs its stamina — so a wall winds you a few blocks at a time. */
    @Inject(method = "place", at = @At("RETURN"))
    private void alone$chargeHeavyPlacement(BlockPlaceContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(ctx.getPlayer() instanceof ServerPlayer player) || player.isCreative()
                || !cir.getReturnValue().consumesAction()) {
            return;
        }
        Block block = ((BlockItem) (Object) this).getBlock();
        if (block.defaultBlockState().is(Construction.TIRING_TO_PLACE)) {
            SurvivalMeters.exert(player, Construction.PLACE_STAMINA);
        }
    }
}
