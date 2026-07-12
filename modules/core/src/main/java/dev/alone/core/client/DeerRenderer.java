package dev.alone.core.client;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import dev.alone.core.Deer;

/**
 * Draws the {@link dev.alone.core.Deer deer} using our custom {@link DeerModel} which matches
 * the exact pixel sizes of the AI-generated texture grid, giving it realistic deer proportions.
 */
public class DeerRenderer extends MobRenderer<Deer, CowRenderState, DeerModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/deer.png");

    public static final ModelLayerLocation DEER_MODEL_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("alone", "deer"), "main");

    public DeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeerModel(context.bakeLayer(DEER_MODEL_LAYER)), 0.4F);
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
    public void extractRenderState(Deer entity, CowRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }
}
