package dev.alone.core.client;

import dev.alone.core.AloneCore;
import dev.alone.core.Forging;
import dev.alone.core.SurvivalMeters;
import dev.alone.core.net.SurvivalSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Client-only entrypoint: receive survival syncs and draw the HUD. */
public class AloneCoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SurvivalSyncPayload.TYPE,
            (payload, context) -> {
                ClientSurvivalState.update(payload);
                // Mirror stamina onto the client player so Climbing's client-side gate (which drives the
                // actual climb physics) sees the real value and drops you off the wall when you're spent.
                if (context.player() != null) {
                    context.player().setAttached(SurvivalMeters.STAMINA, payload.stamina());
                }
            });

        // The Condition Panel (§1.5): no hearts bar — the body is a vitality bar + the injury readout,
        // both drawn by our HUD. Hide the vanilla hearts.
        HudElementRegistry.removeElement(VanillaHudElements.HEALTH_BAR);

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "survival_hud"),
            (graphics, deltaTracker) -> SurvivalHud.render(graphics));

        // Forge status on metal gear (§8.2): tell the player whether a piece is a raw blank or forged,
        // its grade, and how many times it's been re-tempered.
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!Forging.isForgeable(stack)) {
                return;
            }
            if (!Forging.isForged(stack)) {
                lines.add(Component.literal("⚒ Unforged blank — brittle. Heat it, then hammer it on an anvil.")
                    .withStyle(ChatFormatting.RED));
                return;
            }
            float quality = stack.getOrDefault(Forging.QUALITY, 0.5f);
            lines.add(Component.literal("⚒ Forged: " + Forging.gradeName(quality)
                + " (" + Math.round(quality * 100f) + "%)").withStyle(ChatFormatting.GOLD));
            int reforge = stack.getOrDefault(Forging.REFORGE, 0);
            if (reforge > 0) {
                lines.add(Component.literal("Re-tempered ×" + reforge + " — tired steel")
                    .withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
