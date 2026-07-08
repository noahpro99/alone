package dev.alone.core.client;

import dev.alone.core.AloneCore;
import dev.alone.core.net.SurvivalSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/** Client-only entrypoint: receive survival syncs and draw the HUD. */
public class AloneCoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SurvivalSyncPayload.TYPE,
            (payload, context) -> ClientSurvivalState.update(payload));

        // The Condition Panel (§1.5): no hearts bar — the body is a vitality bar + the injury readout,
        // both drawn by our HUD. Hide the vanilla hearts.
        HudElementRegistry.removeElement(VanillaHudElements.HEALTH_BAR);

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(AloneCore.NAMESPACE, "survival_hud"),
            (graphics, deltaTracker) -> SurvivalHud.render(graphics));
    }
}
