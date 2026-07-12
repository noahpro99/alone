package dev.alone.core.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CowRenderState;

public class DeerModel extends QuadrupedModel<CowRenderState> {
    public DeerModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Head (Pivot at 0.0F, 4.0F, -5.5F - centered at bottom of neck)
        // Contains Neck, Head, Snout, and Branched Antlers!
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                // Neck (width 3, height 8, depth 3) - centered at pivot
                .texOffs(12, 40)
                .addBox(-1.5F, -8.0F, -1.5F, 3.0F, 8.0F, 3.0F)
                // Head box (width 3, height 4, depth 6) - slender and long
                .texOffs(0, 0)
                .addBox(-1.5F, -12.0F, -3.5F, 3.0F, 4.0F, 6.0F)
                // Snout (width 2, height 2, depth 3) - longer snout
                .texOffs(1, 35)
                .addBox(-1.0F, -10.0F, -6.5F, 2.0F, 2.0F, 3.0F)
                // Right Antler (base + branch)
                .texOffs(22, 0)
                .addBox(-1.5F, -16.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                .addBox(-3.5F, -17.0F, -0.5F, 2.0F, 1.0F, 1.0F)
                // Left Antler (base + branch)
                .texOffs(22, 0)
                .addBox(0.5F, -16.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                .addBox(1.5F, -17.0F, -0.5F, 2.0F, 1.0F, 1.0F),
            PartPose.offset(0.0F, 4.0F, -5.5F)
        );

        // 2. Body (width 6, length 14, depth 6)
        PartDefinition bodyPart = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(18, 4)
                .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 14.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );

        // 3. Tail (as a child of the body, so it rotates/moves with the body)
        // Positioned at the rear of the body
        bodyPart.addOrReplaceChild(
            "tail",
            CubeListBuilder.create()
                .texOffs(30, 40)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 7.0F, -2.0F, 0.5f, 0.0f, 0.0f)
        );

        // 4. Legs (slender: width 2, height 16, depth 2)
        CubeListBuilder legBuilder = CubeListBuilder.create()
            .texOffs(0, 16)
            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F);

        root.addOrReplaceChild("right_hind_leg", legBuilder, PartPose.offset(-2.0F, 8.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", legBuilder, PartPose.offset(2.0F, 8.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", legBuilder, PartPose.offset(-2.0F, 8.0F, -5.0F));
        root.addOrReplaceChild("left_front_leg", legBuilder, PartPose.offset(2.0F, 8.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
