package dev.alone.core.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.BrownBear brown bear} with the vanilla polar bear model and animations —
 * only the coat changes. Reuses everything from {@link PolarBearRenderer} and just points the texture at
 * the brown skin, so a recolour of the polar bear art is all it takes.
 */
public class BrownBearRenderer extends PolarBearRenderer {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/brown_bear.png");

    public BrownBearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(PolarBearRenderState state) {
        return TEXTURE;
    }
}
