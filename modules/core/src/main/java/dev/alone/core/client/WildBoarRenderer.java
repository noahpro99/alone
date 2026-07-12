package dev.alone.core.client;

import dev.alone.core.WildBoar;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the {@link dev.alone.core.WildBoar wild boar} using our custom {@link WildBoarModel}
 * and a dedicated shaggy wild boar skin.
 */
public class WildBoarRenderer extends MobRenderer<WildBoar, LivingEntityRenderState, WildBoarModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("alone", "textures/entity/wild_boar.png");

    public static final ModelLayerLocation WILD_BOAR_MODEL_LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath("alone", "wild_boar"), "main");

    public WildBoarRenderer(EntityRendererProvider.Context context) {
        super(context, new WildBoarModel(context.bakeLayer(WILD_BOAR_MODEL_LAYER)), 0.7F);
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

