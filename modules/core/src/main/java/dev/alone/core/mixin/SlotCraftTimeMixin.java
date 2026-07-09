package dev.alone.core.mixin;

import dev.alone.core.CraftingTime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Timed crafting gate (proposal §8.2). A crafting {@link ResultSlot} can't be picked up until its
 * craft time has been worked (tracked by {@link CraftingTime}). {@code mayPickup} lives on
 * {@link Slot}, so we mix in here and narrow to result slots — furnace output and every other slot
 * are untouched.
 */
@Mixin(Slot.class)
public class SlotCraftTimeMixin {
    @Inject(method = "mayPickup", at = @At("RETURN"), cancellable = true)
    private void alone$craftTime(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !((Object) this instanceof ResultSlot)) {
            return;
        }
        ItemStack result = ((Slot) (Object) this).getItem();
        if (!result.isEmpty() && !CraftingTime.isReady(player, result)) {
            cir.setReturnValue(false); // still being worked — you can't take it yet
        }
    }
}
