package dev.alone.core.mixin;

import dev.alone.core.Carry;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Volume caps on container slots (proposal §5.1): the 1 m³ personal limit and a per-container
 * storage limit (a 27-slot chest = 1 m³, scaling with size), applied wherever a menu places an item —
 * shift-click, click-to-place, drag. Functional containers (furnaces, crafting) are left uncapped.
 *
 * <p>Partial placement: a slot accepts an item as long as at least one more fits by volume
 * ({@link #alone$mayPlaceByVolume}), and {@link #alone$capInsert} trims how many actually go in — so
 * placing a big stack into a near-full chest deposits what fits and leaves the rest.
 */
@Mixin(Slot.class)
public class SlotVolumeMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void alone$mayPlaceByVolume(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Container container = ((Slot) (Object) this).container;
        float remaining = Carry.remainingFor(container, stack); // bucket-aware for the player's inventory
        if (remaining == Float.MAX_VALUE) {
            return; // uncapped container
        }
        // Allow if even one more unit fits; the actual count is trimmed on insert.
        if (remaining < Carry.unitVolume(stack)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), argsOnly = true, index = 2)
    private int alone$capInsert(int inputAmount, ItemStack inputStack) {
        Container container = ((Slot) (Object) this).container;
        float remaining = Carry.remainingFor(container, inputStack);
        if (remaining == Float.MAX_VALUE) {
            return inputAmount;
        }
        float unit = Carry.unitVolume(inputStack);
        if (unit <= 0f) {
            return inputAmount;
        }
        int fit = (int) Math.floor(remaining / unit);
        return Math.max(0, Math.min(inputAmount, fit));
    }
}
