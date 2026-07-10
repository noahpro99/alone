package dev.alone.core.mixin;

import dev.alone.core.Carry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Placing a block takes a moment (proposal §5.1 / realism) — generous, but scaled by the block's
 * <b>weight</b> like everything else you carry: a light thing (torch, sapling) goes down almost at
 * once, a full stone/dirt block takes ~half a second to seat. Implemented with the vanilla item
 * cooldown, so you can't re-place until it elapses and the hotbar shows the sweep. Creative is exempt.
 */
@Mixin(BlockItem.class)
public class BlockItemPlaceCooldownMixin {
    private static final double TICKS_PER_KG = 0.33; // a ~30 kg full block ≈ 10 ticks (0.5s)
    private static final int MIN_TICKS = 2;          // even a feather-light block needs a beat
    private static final int MAX_TICKS = 12;         // …and the heaviest is capped at ~0.6s (stays generous)

    /** Already mid-placement — refuse the next one until the cooldown elapses. */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void alone$placeGate(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player != null && !player.isCreative() && player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    /** A placement landed — set a weight-scaled cooldown before the next one. */
    @Inject(method = "useOn", at = @At("RETURN"))
    private void alone$placeCost(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || player.isCreative() || !cir.getReturnValue().consumesAction()) {
            return;
        }
        ItemStack stack = context.getItemInHand();
        int ticks = (int) Math.round(Carry.unitWeight(stack) * TICKS_PER_KG);
        player.getCooldowns().addCooldown(stack, Math.max(MIN_TICKS, Math.min(MAX_TICKS, ticks)));
    }
}
