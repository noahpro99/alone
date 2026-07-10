package dev.alone.core.mixin;

import dev.alone.core.AloneItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * One pack per person (proposal §6). Since a backpack is worn/carried gear (there's no stacking a
 * second on your back), refuse to add a backpack to an inventory that already holds one. Covers the
 * usual routes in — picking one up, shift-crafting, {@code /give}. (Deliberately cursor-shuffling a
 * second into a slot isn't blocked here; the natural ways to acquire one are.)
 */
@Mixin(Inventory.class)
public class InventoryBackpackLimitMixin {
    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void alone$onlyOneBackpack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.is(AloneItems.BACKPACK)) {
            return;
        }
        Inventory inventory = (Inventory) (Object) this;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(AloneItems.BACKPACK)) {
                cir.setReturnValue(false); // already carrying one — the second stays put
                return;
            }
        }
    }
}
