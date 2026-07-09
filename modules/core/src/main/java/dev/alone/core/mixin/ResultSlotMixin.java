package dev.alone.core.mixin;

import dev.alone.core.CraftingTime;
import dev.alone.core.SurvivalMeters;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crafting is light exertion (proposal §8.2) — taking a crafted result costs a little stamina, and
 *  restarts the craft timer so the next item must be worked afresh (see {@link CraftingTime}). */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void alone$craftExertion(Player player, ItemStack carried, CallbackInfo ci) {
        CraftingTime.reset(player, carried); // each craft costs its own time
        if (!player.level().isClientSide() && !player.isCreative()) {
            SurvivalMeters.exert(player, 2f);
        }
    }
}
