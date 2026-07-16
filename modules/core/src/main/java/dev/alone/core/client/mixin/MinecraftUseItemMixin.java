package dev.alone.core.client.mixin;

import dev.alone.core.AloneItems;
import dev.alone.core.Drinking;
import dev.alone.core.Fibers;
import dev.alone.core.FireStarting;
import dev.alone.core.net.DrinkRequestPayload;
import dev.alone.core.net.FireDrillPayload;
import dev.alone.core.net.KnapStrikePayload;
import dev.alone.core.net.RiveStrokePayload;
import dev.alone.core.net.StripFiberPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
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
        // The send-throttle is gated on the player's tickCount, but a respawn creates a FRESH player whose
        // tickCount restarts at 0 — while our static lastSendTick still holds the (large) pre-death value.
        // That left "now - lastSendTick" negative for a long time after dying, so strip/rive/drill/knap
        // right-clicks silently stopped firing. If the clock has run backwards, the player was replaced —
        // reset the throttle so hold-actions work again immediately.
        if (mc.player.tickCount < alone$lastSendTick) {
            alone$lastSendTick = mc.player.tickCount - 100;
        }
        ItemStack main = mc.player.getMainHandItem();

        // Hold right-click on a fibrous plant (bare-handed or with a cutting blade) → strip plant fibre
        // over a couple of seconds (server accumulates the tugs). Takes priority over the bare-hand water
        // check below, but they never collide — one targets a plant, the other water.
        boolean stripTool = main.isEmpty() || main.is(ItemTags.SWORDS) || main.is(ItemTags.AXES)
            || main.is(ItemTags.HOES) || main.is(AloneItems.FLINT_KNIFE);
        if (stripTool && Fibers.findFibrousPlant(mc.player, mc.level) != null) {
            if (mc.player.tickCount - alone$lastSendTick >= 5) {
                ClientPlayNetworking.send(StripFiberPayload.INSTANCE);
                mc.player.swing(InteractionHand.MAIN_HAND);
                alone$lastSendTick = mc.player.tickCount;
            }
            ci.cancel();
            return;
        }

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

        // Hold right-click with a stick (hand drill) or a bow drill on an unlit campfire → drill it
        // alight (no crouch needed). The bow drill is faster/more reliable; both are handled server-side.
        if ((main.is(Items.STICK) || main.is(AloneItems.BOW_DRILL) || main.is(AloneItems.FERRO_ROD)
                || main.is(AloneItems.FLINT_AND_PYRITE))
            && FireStarting.findUnlitCampfire(mc.player, mc.level) != null) {
            if (mc.player.tickCount - alone$lastSendTick >= 4) {
                ClientPlayNetworking.send(FireDrillPayload.INSTANCE);
                mc.player.swing(InteractionHand.MAIN_HAND);
                alone$lastSendTick = mc.player.tickCount;
            }
            ci.cancel();
            return;
        }

        // Sneak + hold right-click with flint + a rock (either hand) → strike to knap a sharp flake. The crouch
        // is what distinguishes deliberate knapping from just holding the two items (and from throwing the rock).
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
            return;
        }

        // Sneak + hold right-click with a log + a flint hatchet (slow riving) or a hand saw (quick) → work
        // it into boards.
        boolean haveLog = main.is(ItemTags.LOGS) || off.is(ItemTags.LOGS);
        boolean haveRiveTool = main.is(ItemTags.AXES) || off.is(ItemTags.AXES)
            || main.is(AloneItems.HAND_SAW) || off.is(AloneItems.HAND_SAW);
        if (mc.player.isShiftKeyDown() && haveLog && haveRiveTool) {
            if (mc.player.tickCount - alone$lastSendTick >= 6) {
                ClientPlayNetworking.send(RiveStrokePayload.INSTANCE);
                mc.player.swing(InteractionHand.MAIN_HAND);
                alone$lastSendTick = mc.player.tickCount;
            }
            ci.cancel();
        }
    }
}
