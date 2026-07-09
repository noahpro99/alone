package dev.alone.core.client.mixin;

import dev.alone.core.CraftingTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The visual for timed crafting (proposal §8.2): a small progress bar along the bottom of a crafting
 * result slot, filling as the craft is worked (see {@link CraftingTime}). It appears only while a
 * result is mid-craft — a finished, takeable result shows no bar.
 */
@Mixin(AbstractContainerScreen.class)
public class ScreenCraftBarMixin {
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void alone$craftBar(GuiGraphicsExtractor extractor, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!(slot instanceof ResultSlot) || !slot.hasItem()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack result = slot.getItem();
        float fraction = CraftingTime.progressFraction(player, result);
        if (fraction <= 0f || fraction >= 1f) {
            return; // not started, or done and takeable — no bar
        }
        int x = slot.x;
        int y = slot.y;
        extractor.fill(x, y + 13, x + 16, y + 16, 0xC0000000);                      // dark backdrop
        int width = Math.max(1, Math.round(14 * fraction));
        extractor.fill(x + 1, y + 14, x + 1 + width, y + 15, 0xFF39C13B);           // green fill
    }
}
