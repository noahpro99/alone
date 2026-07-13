package dev.alone.core.mixin;

import dev.alone.core.AnimalProducts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Milk on a schedule (proposal §7.2). Vanilla lets you milk a cow every tick — an infinite tap. A real
 * cow's udder holds about a day's yield, then it must refill. So we stamp {@link AnimalProducts#LAST_MILKED}
 * on the cow the moment a bucket is drawn, and refuse another bucket until the udder has refilled
 * ({@link AnimalProducts#MILK_COOLDOWN_TICKS}, ~one in-game day).
 *
 * <p>Injected at the HEAD of {@link AbstractCow#mobInteract} (which is where the bucket → milk exchange
 * lives — covers cows and mooshrooms). We replicate vanilla's own milking gate ({@code bucket in hand &amp;
 * not a calf}) so we only ever touch a genuine milking attempt; every other interaction falls straight
 * through to vanilla. When it's too soon we cancel with {@link InteractionResult#SUCCESS} (the arm still
 * swings, but no milk) and tell the player on the action bar. The stamp is set server-side and synced, so
 * the client's own {@code mobInteract} prediction agrees and never flashes a phantom bucket.
 */
@Mixin(AbstractCow.class)
public abstract class CowMilkCooldownMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void alone$milkSchedule(Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        AbstractCow self = (AbstractCow) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        // Only intercept an actual milking — an empty bucket on an adult cow. Anything else (breeding
        // with wheat, a bowl on a mooshroom, an empty hand) is none of our business.
        if (!stack.is(Items.BUCKET) || self.isBaby()) {
            return;
        }
        long now = self.level().getGameTime();
        if (!AnimalProducts.udderRefilled(self.getAttached(AnimalProducts.LAST_MILKED), now)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.literal("The cow's udder is empty — give it a day to refill."), true);
            }
            cir.setReturnValue(InteractionResult.SUCCESS); // consume the click, but no milk
            return;
        }
        // Allowed — record the draw (server authoritative; the attachment syncs to clients) and let
        // vanilla fill the bucket.
        if (!self.level().isClientSide()) {
            self.setAttached(AnimalProducts.LAST_MILKED, now);
        }
    }
}
