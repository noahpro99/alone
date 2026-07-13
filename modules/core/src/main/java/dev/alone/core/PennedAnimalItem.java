package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A "penned" farm animal bought from a village herder (the Mesopotamian livestock economy — see the
 * curated villager trades under {@code data/alone/villager_trade/farmer/}). Villager trades can only hand
 * out <b>items</b>, never live entities, so a purchased animal rides home as this small token: right-click
 * the ground with it and the animal steps out of the pen (the token is spent). This is the clean 26.2 way
 * to "buy livestock" — a spawn-item, not a trade-completion hook — and it means the beast is a real,
 * persistent, breedable animal from the moment you release it, exactly as if you'd led it home on a rope.
 *
 * <p>It is deliberately a tiny, textureless item (renders as the missing-texture placeholder until art is
 * added, per the pack's no-placeholder-PNG rule): flavour lives in the mechanic, not the model.
 */
public class PennedAnimalItem extends Item {
    /** The animal this token releases. Vanilla {@code EntityTypes} constants are bound before item init, so
     *  referencing one here (unlike an item's data components) is safe at class-load. */
    private final EntityType<?> animal;

    public PennedAnimalItem(EntityType<?> animal, Properties properties) {
        super(properties);
        this.animal = animal;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // Release the animal into the cell against the clicked face (on top of the ground when you click a
        // floor), the same placement a spawn egg uses. Server-authoritative: the client just plays along.
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            var spawned = animal.spawn(serverLevel, pos, EntitySpawnReason.SPAWN_ITEM_USE);
            if (spawned == null) {
                return InteractionResult.PASS; // no room / spawn refused — keep the token
            }
            if (context.getPlayer() != null) {
                spawned.setYRot(context.getPlayer().getYRot());
            }
            level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
