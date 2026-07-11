package dev.alone.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alone.core.TravoisEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;

/**
 * Draws the travois as a low wooden block model (placeholder art), following exactly how vanilla renders
 * a falling block: populate a {@link net.minecraft.client.renderer.block.MovingBlockRenderState} from the
 * level's lighting/biome and submit it. Real art (poles + hide platform) can replace this later.
 */
public class TravoisRenderer extends EntityRenderer<TravoisEntity, TravoisRenderState> {
    public TravoisRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public TravoisRenderState createRenderState() {
        return new TravoisRenderState();
    }

    @Override
    public void extractRenderState(TravoisEntity entity, TravoisRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = entity.blockPosition();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = TravoisEntity.SHOWN_BLOCK;
        if (entity.level() instanceof ClientLevel level) {
            state.movingBlockRenderState.biome = level.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = level.cardinalLighting();
            state.movingBlockRenderState.lightEngine = level.getLightEngine();
        }
    }

    @Override
    public void submit(TravoisRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        if (state.movingBlockRenderState.blockState.getRenderShape() != RenderShape.INVISIBLE) {
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);
            collector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.lightCoords);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, cameraState);
    }
}
