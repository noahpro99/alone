package dev.alone.core.client.mixin;

import dev.alone.core.AloneItems;
import dev.alone.core.Drinking;
import dev.alone.core.FireStarting;
import dev.alone.core.net.DrinkRequestPayload;
import dev.alone.core.net.FireDrillPayload;
import dev.alone.core.net.KnapStrikePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bare-hand right-click doesn't fire Fabric's use events ({@code startUseItem} skips {@code useItem}
 * for empty hands, and {@code useItemOn} needs a block crosshair target — water usually isn't one).
 * So we hook {@code startUseItem} directly: if you're looking at drinkable water bare-handed, tell
 * the server to drink and consume the click.
 */
@Mixin(Minecraft.class)
public class MinecraftUseItemMixin {
    private static int alone$lastSendTick = -100;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void alone$customUse(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null || mc.level == null) {
            return;
        }
        ItemStack main = mc.player.getMainHandItem();

        // Bare-hand right-click on water → drink.
        if (main.isEmpty()) {
            if (Drinking.findWaterSource(mc.player, mc.level) != null) {
                if (mc.player.tickCount - alone$lastSendTick >= 8) {
                    ClientPlayNetworking.send(DrinkRequestPayload.INSTANCE);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    alone$lastSendTick = mc.player.tickCount;
                }
                ci.cancel();
            }
            return;
        }

        // Sneak + hold right-click with a stick on an unlit campfire → drill it alight.
        if (main.is(Items.STICK) && mc.player.isShiftKeyDown()
            && FireStarting.findUnlitCampfire(mc.player, mc.level) != null) {
            if (mc.player.tickCount - alone$lastSendTick >= 4) {
                ClientPlayNetworking.send(FireDrillPayload.INSTANCE);
                mc.player.swing(InteractionHand.MAIN_HAND);
                alone$lastSendTick = mc.player.tickCount;
            }
            ci.cancel();
            return;
        }

        // Sneak + hold right-click with flint + a rock (either hand) → strike to knap a sharp flake.
        ItemStack off = mc.player.getOffhandItem();
        boolean haveFlint = main.is(Items.FLINT) || off.is(Items.FLINT);
        boolean haveRock = main.is(AloneItems.ROCK) || off.is(AloneItems.ROCK);
        if (mc.player.isShiftKeyDown() && haveFlint && haveRock) {
            if (mc.player.tickCount - alone$lastSendTick >= 5) {
                ClientPlayNetworking.send(KnapStrikePayload.INSTANCE);
                mc.player.swing(InteractionHand.MAIN_HAND);
                alone$lastSendTick = mc.player.tickCount;
            }
            ci.cancel();
        }
    }
}
