package dev.alone.core.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.alone.core.AloneCore;
import dev.alone.core.CraftingTime;
import dev.alone.core.Forging;
import dev.alone.core.SurvivalMeters;
import dev.alone.core.net.BackpackOpenPayload;
import dev.alone.core.net.SurvivalSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Client-only entrypoint: receive survival syncs and draw the HUD. */
public class AloneCoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Quick-open backpack keybind (default B) — opens the first backpack in your pack (§6). Filed
        // under a dedicated "Alone" category in the controls screen, where it can be rebound.
        KeyMapping.Category aloneKeys = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("alone", "main"));
        KeyMapping openBackpack = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.alone.open_backpack", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, aloneKeys));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBackpack.consumeClick()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(BackpackOpenPayload.INSTANCE);
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SurvivalSyncPayload.TYPE,
            (payload, context) -> {
                ClientSurvivalState.update(payload);
                // Mirror stamina onto the client player so Climbing's client-side gate (which drives the
                // actual climb physics) sees the real value and drops you off the wall when you're spent.
                if (context.player() != null) {
                    context.player().setAttached(SurvivalMeters.STAMINA, payload.stamina());
                }
            });
        // A slip on a rock climb: drop the local grip so the client falls in step with the server that
        // rolled it, instead of predicting a climb the server has already ended (§5.4).
        ClientPlayNetworking.registerGlobalReceiver(dev.alone.core.net.ClimbSlipPayload.TYPE,
            (payload, context) -> {
                if (context.player() != null) {
                    dev.alone.core.Climbing.clientSlip(context.player());
                }
            });

        // Timed crafting (§8.2): tick the same craft-timer client-side so the result slot's "can I take
        // it yet?" gate agrees with the server (no take-then-snap-back on the crafting result).
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                CraftingTime.tick(client.player);
                // Client movement is authoritative, so the jump-momentum latch (which protects a jump's
                // full height near a wall) must be maintained here, where velocity is real.
                dev.alone.core.Climbing.updateJumpLatch(client.player);
            }
        });

        // The travois (§6) renders as a low wooden block model (placeholder), like a falling block.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.TRAVOIS, TravoisRenderer::new);

        // The brown bear (§7.2) reuses the polar bear model with a brown coat — a recolour, nothing more.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.BROWN_BEAR, BrownBearRenderer::new);

        // The deer (§7.2) uses a custom DeerModel matching the AI texture's voxel sizes.
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
            DeerRenderer.DEER_MODEL_LAYER, DeerModel::createBodyLayer);

        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
            SquirrelRenderer.SQUIRREL_MODEL_LAYER, SquirrelModel::createBodyLayer);

        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
            BisonRenderer.BISON_MODEL_LAYER, BisonModel::createBodyLayer);

        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
            WildBoarRenderer.WILD_BOAR_MODEL_LAYER, WildBoarModel::createBodyLayer);

        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.DEER, DeerRenderer::new);

        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.SQUIRREL, SquirrelRenderer::new);

        // The wild boar uses custom WildBoarModel
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.WILD_BOAR, WildBoarRenderer::new);

        // The bison uses custom BisonModel
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.BISON, BisonRenderer::new);

        // A thrown rock (§8.1) renders as the loose-rock item itself — vanilla's ThrownItemRenderer draws
        // the entity's carried item (its rock), so it reuses the rock's existing model/texture, no new art.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            dev.alone.core.AloneEntities.THROWN_ROCK,
            net.minecraft.client.renderer.entity.ThrownItemRenderer::new);



        // Show whatever's curing on a drying rack (§4.2/§7.3) — a piece of food drying, or a hide/leather
        // being tanned — read from the rack's synced attachment.
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
            dev.alone.core.AloneBlocks.DRYING_RACK_BLOCK_ENTITY, DryingRackRenderer::new);

        // Hauling must slow the LOCAL player through jumps and sprints too — and player movement is
        // client-authoritative, so a server-side cap can't hold it. If the local player is dragging a
        // travois, cancel sprint here (kills the sprint-jump momentum burst) and cap horizontal speed to
        // the synced haul factor. This is what actually stops "jump to skip the slowdown". (§6)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }
            dev.alone.core.TravoisEntity hauling = null;
            for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof dev.alone.core.TravoisEntity travois
                    && client.player.getId() == travois.getDraggerId()) {
                    hauling = travois;
                    break;
                }
            }
            if (hauling != null) {
                client.player.setSprinting(false);
                double cap = 0.14 * hauling.getHaulFactor();
                net.minecraft.world.phys.Vec3 v = client.player.getDeltaMovement();
                double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
                if (horiz > cap) {
                    double s = cap / horiz;
                    client.player.setDeltaMovement(v.x * s, v.y, v.z * s);
                }
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
