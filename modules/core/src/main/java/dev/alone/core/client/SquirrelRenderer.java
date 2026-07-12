package dev.alone.core.client;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import dev.alone.core.Squirrel;

/**
 * Draws the {@link dev.alone.core.Squirrel squirrel} using our custom {@link SquirrelModel} which matches
 * its small, agile body and fluffy, curved tail proportions.
 */
public class SquirrelRenderer extends MobRenderer<Squirrel, RabbitRenderState, SquirrelModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/squirrel.png");

    public static final ModelLayerLocation SQUIRREL_MODEL_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("alone", "squirrel"), "main");

    public SquirrelRenderer(EntityRendererProvider.Context context) {
        super(context, new SquirrelModel(context.bakeLayer(SQUIRREL_MODEL_LAYER)), 0.25F);
    }

    @Override
    public Identifier getTextureLocation(RabbitRenderState state) {
        return TEXTURE;
    }

    @Override
    public RabbitRenderState createRenderState() {
        return new RabbitRenderState();
    }

    @Override
    public void extractRenderState(Squirrel entity, RabbitRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }
}

