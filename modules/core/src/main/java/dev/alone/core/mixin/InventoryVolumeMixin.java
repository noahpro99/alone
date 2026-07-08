package dev.alone.core.mixin;

import dev.alone.core.Carry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hard 1 m³ volume cap on the person (proposal §5.1). A pickup adds only as many items as fit by
 * volume and leaves the rest in the world (like vanilla's full inventory — nothing is dropped or
 * deleted). A merged stack (e.g. two dirt that clumped on the ground) yields the part that fits.
 *
 * <p>We add the fitting portion via the two-arg {@code add(int, ItemStack)} (which this mixin does
 * NOT intercept), shrink the caller's stack by exactly what went in, and report the true result —
 * so no caller ever thinks the whole stack was consumed when it wasn't.
 */
@Mixin(Inventory.class)
public class InventoryVolumeMixin {

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void alone$capPickup(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Inventory self = (Inventory) (Object) this;
        Player player = self.player;
        if (player.level().isClientSide() || player.isCreative()) {
            return; // server is authoritative for pickups — a client-side cap only risks desync
        }
        float unit = Carry.unitVolume(stack);
        if (unit <= 0f) {
            return; // no volume — let vanilla handle it
        }
        float remaining = Carry.volumeLimit(player) + 0.001f - Carry.totalVolume(player);
        int fit = (int) Math.floor(remaining / unit);
        if (fit >= stack.getCount()) {
            return; // it all fits — let vanilla handle it normally
        }
        if (fit <= 0) {
            cir.setReturnValue(false); // nothing fits — leave it all in the world
            return;
        }
        // Partial: add only what fits; the rest stays in the caller's stack.
        ItemStack portion = stack.copyWithCount(fit);
        self.add(-1, portion);                     // two-arg overload — not intercepted
        stack.shrink(fit - portion.getCount());    // remove exactly what actually went in
        cir.setReturnValue(stack.isEmpty());
    }
}
