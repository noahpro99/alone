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

        // 1. Head (width 6, height 6, depth 5)
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.0F, -3.0F, -5.0F, 6.0F, 6.0F, 5.0F) // main head box
                .texOffs(1, 33)
                .addBox(-2.0F, 0.0F, -6.0F, 4.0F, 3.0F, 1.0F)  // snout/muzzle
                // Antlers (on top of head)
                .texOffs(22, 0)
                .addBox("right_horn", -4.0F, -6.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                .texOffs(22, 0)
                .addBox("left_horn", 3.0F, -6.0F, -3.0F, 1.0F, 4.0F, 1.0F),
            PartPose.offset(0.0F, 4.0F, -8.0F)
        );

        // 2. Body (width 6, length 14, depth 6)
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(18, 4)
                .addBox(-3.0F, -7.0F, -3.0F, 6.0F, 14.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );

        // 3. Legs (slender: width 2, height 8, depth 2)
        CubeListBuilder legBuilder = CubeListBuilder.create()
            .texOffs(0, 16)
            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F);

        root.addOrReplaceChild("right_hind_leg", legBuilder, PartPose.offset(-2.0F, 16.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", legBuilder, PartPose.offset(2.0F, 16.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", legBuilder, PartPose.offset(-2.0F, 16.0F, -5.0F));
        root.addOrReplaceChild("left_front_leg", legBuilder, PartPose.offset(2.0F, 16.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
