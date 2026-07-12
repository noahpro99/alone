package dev.alone.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alone.core.Bison;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.Bison bison} with our custom {@link BisonModel} —
 * scaled up and textured shaggy to look like a real wild bison.
 */
public class BisonRenderer extends MobRenderer<Bison, CowRenderState, BisonModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/bison.png");

    public static final ModelLayerLocation BISON_MODEL_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("alone", "bison"), "main");

    // A bison stands head and shoulders over a cow — bulk it up slightly on top of the custom model.
    private static final float SCALE = 1.2F;

    public BisonRenderer(EntityRendererProvider.Context context) {
        super(context, new BisonModel(context.bakeLayer(BISON_MODEL_LAYER)), 0.9F);
    }

    @Override
    public Identifier getTextureLocation(CowRenderState state) {
        return TEXTURE;
    }

    @Override
    public CowRenderState createRenderState() {
        return new CowRenderState();
    }

    @Override
    public void extractRenderState(Bison entity, CowRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }

    @Override
    protected void scale(CowRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}


