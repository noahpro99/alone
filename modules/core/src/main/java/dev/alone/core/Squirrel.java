package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;

/**
 * The squirrel — small game to round out the woods alongside the rabbit and chicken. On the show
 * <i>Alone</i> the little animals, not big game, are what people actually live on: caught in numbers,
 * a scrap of meat each. It's built for now on the rabbit's hop-and-flee brain (and made <b>skittish</b>
 * by {@link Wildlife}, so it bolts), and slots into the wildlife systems: it winds <b>fast</b> under
 * {@link Tracking persistence} thanks to its tiny body, counts toward {@link GameStock overhunting}, and
 * a blade kill gives only a scrap of pelt (butcher salvage scales with body size). Reuses the rabbit model
 * as <b>placeholder art</b> until a real squirrel model lands.
 */
public class Squirrel extends Rabbit {
    public Squirrel(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
    }

    @Override
    public Rabbit getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return AloneEntities.SQUIRREL.create(level, EntitySpawnReason.BREEDING); // squirrels breed squirrels
    }
}
