package dev.alone.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.alone.core.TanningRackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the hide (or, once tanned, the leather) currently stretched on a tanning rack, laid flat across the
 * top so you can see what's curing at a glance (purely cosmetic — the tanning itself is driven server-side by
 * {@link TanningRackBlockEntity}). The item comes from the rack's client-synced {@code TANNING} attachment.
 */
public class TanningRackRenderer implements BlockEntityRenderer<TanningRackBlockEntity, TanningRackRenderState> {
    private final ItemModelResolver itemModelResolver;

    public TanningRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public TanningRackRenderState createRenderState() {
        return new TanningRackRenderState();
    }

    @Override
    public void extractRenderState(TanningRackBlockEntity be, TanningRackRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        ItemStack hide = be.heldHide();
        state.hasItem = !hide.isEmpty();
        if (state.hasItem) {
            this.itemModelResolver.updateForTopItem(state.item, hide, ItemDisplayContext.FIXED, be.getLevel(), null,
                (int) be.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(TanningRackRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.hasItem || state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.86f, 0.5f);            // centred, resting on top of the rack frame
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f)); // lay it flat, face-up
        poseStack.scale(0.55f, 0.55f, 0.55f);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
