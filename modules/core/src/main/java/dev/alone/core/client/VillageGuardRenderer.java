package dev.alone.core.client;

import dev.alone.core.VillageGuard;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the {@link VillageGuard} as an armoured biped. The guard's <em>entity</em> is a neutral
 * {@link net.minecraft.world.entity.PathfinderMob} (so iron golems ignore it — see {@link VillageGuard}); its
 * <em>look</em> is borrowed wholesale from the vanilla player: the {@link ModelLayers#PLAYER standard player
 * model} skinned with a plain human texture, plus the {@link HumanoidArmorLayer standard armour layer} so its
 * chainmail renders, and the {@link HumanoidMobRenderer base renderer}'s own item-in-hand layer so its bow
 * shows. No new art — a wide-model human in armour reads exactly as a villager-in-arms.
 */
public class VillageGuardRenderer
    extends HumanoidMobRenderer<VillageGuard, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    /** A plain human skin (vanilla's default wide "steve") — the guard is a person, not an illager or undead. */
    private static final Identifier TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public VillageGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        ArmorModelSet<HumanoidModel<HumanoidRenderState>> armor =
            ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
