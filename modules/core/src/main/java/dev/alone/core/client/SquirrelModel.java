package dev.alone.core.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.util.Mth;

public class SquirrelModel extends EntityModel<RabbitRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tailBase;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public SquirrelModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tailBase = this.body.getChild("tail_base");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Body: 2x2x5 box at pivot (0, 21, 0)
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 5.0F),
            PartPose.offset(0.0F, 21.0F, 0.0F)
        );

        // 2. Head: 2x2x2 box + nose + ears
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 2.0F) // main head box
                .texOffs(0, 14)
                .addBox(-0.9F, -3.5F, -1.0F, 0.8F, 1.5F, 0.8F) // right ear
                .addBox(0.1F, -3.5F, -1.0F, 0.8F, 1.5F, 0.8F), // left ear
            PartPose.offset(0.0F, 20.0F, -2.5F)
        );

        // 3. Bushy Tail: two segments
        // tail_base (2x4x2)
        PartDefinition tailBase = body.addOrReplaceChild(
            "tail_base",
            CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 2.5F, -0.6F, 0.0F, 0.0F)
        );
        // tail_top (3x4x3) curves forward over the back
        tailBase.addOrReplaceChild(
            "tail_top",
            CubeListBuilder.create()
                .texOffs(16, 8)
                .addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -3.8F, 0.0F, -1.0F, 0.0F, 0.0F)
        );

        // 4. Legs
        CubeListBuilder frontLeg = CubeListBuilder.create()
            .texOffs(0, 20)
            .addBox(-0.4F, 0.0F, -0.4F, 0.8F, 3.0F, 0.8F);

        CubeListBuilder hindLeg = CubeListBuilder.create()
            .texOffs(8, 20)
            .addBox(-0.5F, 0.0F, -0.8F, 1.0F, 4.0F, 1.6F);

        root.addOrReplaceChild("right_front_leg", frontLeg, PartPose.offset(-0.75F, 21.0F, -1.8F));
        root.addOrReplaceChild("left_front_leg", frontLeg, PartPose.offset(0.75F, 21.0F, -1.8F));

        root.addOrReplaceChild("right_hind_leg", hindLeg, PartPose.offset(-0.9F, 20.0F, 1.5F));
        root.addOrReplaceChild("left_hind_leg", hindLeg, PartPose.offset(0.9F, 20.0F, 1.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(RabbitRenderState state) {
        super.setupAnim(state);

        // Gentle tail twitch/wag idle animation
        float age = state.ageInTicks;
        this.tailBase.xRot = -0.6F + Mth.sin(age * 0.15F) * 0.05F;
        this.tailBase.zRot = Mth.sin(age * 0.25F) * 0.08F;

        // Hopping/walking legs animation
        // Squirrels scurry and jump, so we animate legs based on the walk progress (walkAnimationPos / walkAnimationSpeed)
        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;

        this.rightFrontLeg.xRot = Mth.cos(walkPos * 0.8F) * 1.0F * walkSpeed;
        this.leftFrontLeg.xRot = -Mth.cos(walkPos * 0.8F) * 1.0F * walkSpeed;
        this.rightHindLeg.xRot = -Mth.cos(walkPos * 0.8F) * 1.0F * walkSpeed;
        this.leftHindLeg.xRot = Mth.cos(walkPos * 0.8F) * 1.0F * walkSpeed;

        // Head tilt/look animation
        this.head.xRot = state.xRot * 0.017453292F;
        this.head.yRot = state.yRot * 0.017453292F;
    }
}
