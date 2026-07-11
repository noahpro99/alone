package dev.alone.core.client.mixin;

import dev.alone.core.client.SurvivalHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the carry bars (hands / pockets / weight) only while the <b>player inventory</b> is open (proposal
 * §5.1). They're off the always-on HUD — managing your load is a pack-time concern. Hooked on the base
 * Screen's <b>final</b> {@code extractRenderStateWithTooltipAndSubtitles}, which every screen goes through
 * (so subclass overrides of the inner extract can't bypass it), using the same {@link GuiGraphicsExtractor}
 * the HUD draws with. Gated to the survival inventory screen.
 */
@Mixin(Screen.class)
public class ContainerScreenCarryMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void alone$drawCarry(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                 CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen) {
            SurvivalHud.renderCarry(graphics);
        }
    }
}
