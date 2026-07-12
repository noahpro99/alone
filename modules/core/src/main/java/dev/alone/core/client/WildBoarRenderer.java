package dev.alone.core.client;

import dev.alone.core.WildBoar;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.WildBoar wild boar} with the vanilla pig model — only the coat changes, the
 * way the {@link BrownBearRenderer brown bear} reuses the polar bear. It points at the dark, bristly
 * "cold pig" texture as <b>placeholder art</b> (a far wilder, hairier look than the pink barnyard pig) until
 * a dedicated boar skin lands. Modelled on the vanilla pig, so it walks and animates like one.
 */
public class WildBoarRenderer extends MobRenderer<WildBoar, LivingEntityRenderState, PigModel> {
    // Placeholder: the cold-climate pig skin is dark and shaggy — a passable wild boar until real art exists.
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/entity/pig/pig_cold.png");

    public WildBoarRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel(context.bakeLayer(ModelLayers.PIG)), 0.7F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
