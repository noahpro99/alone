package dev.alone.core.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WildBoarModel extends QuadrupedModel<LivingEntityRenderState> {
    public WildBoarModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Long, wedge-shaped Head (Pivot at 0.0F, 10.0F, -7.0F)
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                // Head box: size 6x6x9 (tapered wedge)
                .texOffs(0, 0)
                .addBox(-3.0F, -3.0F, -7.0F, 6.0F, 6.0F, 9.0F)
                // Snout: size 4x3x3 extending forward
                .texOffs(30, 0)
                .addBox(-2.0F, 0.0F, -10.0F, 4.0F, 3.0F, 3.0F)
                // Right tusk
                .texOffs(44, 0)
                .addBox(-4.0F, -1.0F, -8.0F, 1.0F, 2.0F, 1.0F)
                // Left tusk
                .texOffs(44, 0)
                .addBox(3.0F, -1.0F, -8.0F, 1.0F, 2.0F, 1.0F),
            PartPose.offset(0.0F, 10.0F, -7.0F)
        );

        // 2. Body (size: X=10, Y=16, Z=10, rotated 90 deg on X-axis)
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-5.0F, -8.0F, -5.0F, 10.0F, 16.0F, 10.0F)
                // Coarse hair ridge/mane running down the spine: size 2x16x2
                .texOffs(0, 18)
                .addBox(-1.0F, -8.0F, -7.0F, 2.0F, 16.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 11.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );

        // 3. Legs (size 4x10x4 - taller and more wild than a pig's 4x6x4)
        CubeListBuilder legBuilder = CubeListBuilder.create()
            .texOffs(36, 32)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F);

        root.addOrReplaceChild("right_hind_leg", legBuilder, PartPose.offset(-3.0F, 14.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", legBuilder, PartPose.offset(3.0F, 14.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", legBuilder, PartPose.offset(-3.0F, 14.0F, -5.0F));
        root.addOrReplaceChild("left_front_leg", legBuilder, PartPose.offset(3.0F, 14.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
