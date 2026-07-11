package dev.alone.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.alone.core.TravoisEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;

/**
 * Draws the travois as a low plank platform with two trailing log poles (placeholder art), each piece a
 * scaled block model submitted through vanilla's moving-block path (the same one falling blocks use). The
 * whole rig is turned to the sled's heading so the poles trail behind you. All the piece sizes/positions
 * are constants below — easy to tune once it's seen in-game. Real art (poles + a hide bed) can replace it.
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
        state.yRot = entity.getYRot();
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        MovingBlockRenderState mb = state.movingBlockRenderState;
        mb.randomSeedPos = entity.blockPosition();
        mb.blockPos = pos;
        mb.blockState = TravoisEntity.PLATFORM_BLOCK;
        if (entity.level() instanceof ClientLevel level) {
            mb.biome = level.getBiome(pos);
            mb.cardinalLighting = level.cardinalLighting();
            mb.lightEngine = level.getLightEngine();
        }
    }

    @Override
    public void submit(TravoisRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState cameraState) {
        MovingBlockRenderState mb = state.movingBlockRenderState;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot)); // face the sled's heading

        // The bed: a flat, wide plank platform, low to the ground.
        mb.blockState = TravoisEntity.PLATFORM_BLOCK;
        piece(poseStack, collector, mb, state.lightCoords, 0.0f, 0.0f, -0.15f, 0.9f, 0.12f, 0.7f, 0f);

        // Two poles, splaying out in a shallow V and trailing forward past the bed.
        mb.blockState = TravoisEntity.POLE_BLOCK;
        piece(poseStack, collector, mb, state.lightCoords, -0.30f, 0.06f, 0.35f, 0.10f, 0.10f, 1.6f, 6f);
        piece(poseStack, collector, mb, state.lightCoords, 0.30f, 0.06f, 0.35f, 0.10f, 0.10f, 1.6f, -6f);

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /** Submit one block model as a box of size (sx,sy,sz) centred on x/z at (cx,cy,cz), optionally yawed. */
    private static void piece(PoseStack pose, SubmitNodeCollector collector, MovingBlockRenderState mb,
                              int light, float cx, float cy, float cz, float sx, float sy, float sz, float rotY) {
        pose.pushPose();
        pose.translate(cx, cy, cz);
        if (rotY != 0f) {
            pose.mulPose(Axis.YP.rotationDegrees(rotY));
        }
        pose.scale(sx, sy, sz);
        pose.translate(-0.5f, 0.0f, -0.5f); // centre the unit cube on x/z, base at y=0
        collector.submitMovingBlock(pose, mb, light);
        pose.popPose();
    }
}
