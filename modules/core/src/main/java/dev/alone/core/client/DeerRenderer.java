package dev.alone.core.client;

import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.Deer deer} with the cow model and animations as <b>placeholder art</b>,
 * pointing the texture at {@code alone:textures/entity/deer.png} — swap that (and later a real deer model)
 * for the finished look. Reuses everything from {@link CowRenderer}; only the skin is redirected.
 */
public class DeerRenderer extends CowRenderer {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/deer.png");

    public DeerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(CowRenderState state) {
        return TEXTURE;
    }
}
