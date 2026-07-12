package dev.alone.core.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.Squirrel squirrel} with the rabbit model and animations as
 * <b>placeholder art</b>, pointing the texture at {@code alone:textures/entity/squirrel.png} — swap that
 * (and later a real squirrel model) for the finished look. Reuses everything from {@link RabbitRenderer};
 * only the skin is redirected.
 */
public class SquirrelRenderer extends RabbitRenderer {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/squirrel.png");

    public SquirrelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(RabbitRenderState state) {
        return TEXTURE;
    }
}
