package dev.alone.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alone.core.Bison;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.Bison bison} with the vanilla cow model — reused the way the
 * {@link BrownBearRenderer brown bear} reuses the polar bear — but <b>scaled up</b> so it reads as the
 * ton-heavy wild bovine it is, not a milk cow (the {@link dev.alone.core.Deer deer} and brown bear are
 * likewise sized by the model, not just the hitbox). It points at the shaggy "cold cow" texture as
 * <b>placeholder art</b> until a dedicated bison skin lands.
 */
public class BisonRenderer extends MobRenderer<Bison, LivingEntityRenderState, CowModel> {
    // Placeholder: the cold-climate cow skin is dark and woolly — a passable bison until real art exists.
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/entity/cow/cow_cold.png");

    // A bison stands head and shoulders over a cow — bulk it up so the model matches the big, dangerous hitbox.
    private static final float SCALE = 1.5F;

    public BisonRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel(context.bakeLayer(ModelLayers.COW)), 0.9F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}
