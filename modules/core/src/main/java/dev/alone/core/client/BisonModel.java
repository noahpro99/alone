package dev.alone.core.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CowRenderState;

public class BisonModel extends QuadrupedModel<CowRenderState> {
    public BisonModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Head (Pivot positioned lower than cow, offset forward)
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                // Head box: size 8x8x10 (large, blocky skull)
                .texOffs(0, 0)
                .addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 10.0F)
                // Beard / throat mane: size 4x6x4
                .texOffs(48, 0)
                .addBox(-2.0F, 4.0F, -6.0F, 4.0F, 6.0F, 4.0F)
                // Right horn base
                .texOffs(36, 0)
                .addBox(-6.0F, -5.0F, -4.0F, 2.0F, 2.0F, 2.0F)
                // Right horn tip curving upward
                .addBox(-6.0F, -8.0F, -4.0F, 2.0F, 3.0F, 2.0F)
                // Left horn base
                .texOffs(36, 0)
                .addBox(4.0F, -5.0F, -4.0F, 2.0F, 2.0F, 2.0F)
                // Left horn tip curving upward
                .addBox(4.0F, -8.0F, -4.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(0.0F, 8.0F, -8.0F)
        );

        // 2. Body (rotated 90 deg on X-axis: X=14, Y=20, Z=12)
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-7.0F, -10.0F, -9.0F, 14.0F, 20.0F, 12.0F)
                // Massive shoulder hump on the front section of the body
                .texOffs(0, 18)
                .addBox(-7.0F, -10.0F, -17.0F, 14.0F, 10.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 9.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );

        // 3. Thick legs (size 5x12x5)
        CubeListBuilder legBuilder = CubeListBuilder.create()
            .texOffs(36, 32)
            .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F);

        root.addOrReplaceChild("right_hind_leg", legBuilder, PartPose.offset(-5.0F, 12.0F, 8.0F));
        root.addOrReplaceChild("left_hind_leg", legBuilder, PartPose.offset(5.0F, 12.0F, 8.0F));
        root.addOrReplaceChild("right_front_leg", legBuilder, PartPose.offset(-5.0F, 12.0F, -5.0F));
        root.addOrReplaceChild("left_front_leg", legBuilder, PartPose.offset(5.0F, 12.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
