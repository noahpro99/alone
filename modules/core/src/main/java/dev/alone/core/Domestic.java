package dev.alone.core;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/**
 * Barnyard livestock are <b>farmed</b> animals, not wildlife (proposal: livestock belong to
 * settlements). Real cows, pigs, sheep and chickens don't roam the wilds as free game — humans keep
 * them. So their natural spawns are vetoed away from villages (see {@code MobSpawnMixin}), making a
 * village the economy source you visit to acquire livestock, which you can then breed and farm at home.
 *
 * <p>The roster is the settlement herd: cow, pig, sheep, chicken, <b>goat</b> and <b>donkey</b> —
 * the Mesopotamian barnyard, staple flocks (sheep/goat) plus draught and yard animals. The
 * <b>horse</b> is deliberately NOT here: horses stay WILD on the plains (they represent a different
 * region and are never vetoed — see {@code MobSpawnMixin}). Matching is by <b>exact
 * {@link EntityType}</b>, never {@code instanceof}: custom WILD animals (bison extends Cow, wild boar
 * extends Pig) are genuine wildlife with their own types and must NOT be treated as domestic.
 */
public final class Domestic {
    private Domestic() {
    }

    /** True only for the settlement roster (cow, pig, sheep, chicken, goat, donkey) — exact type, so
     *  wild subclasses (and the wild horse) are ignored. */
    public static boolean isDomestic(Mob mob) {
        EntityType<?> type = mob.getType();
        return type == EntityTypes.COW
            || type == EntityTypes.PIG
            || type == EntityTypes.SHEEP
            || type == EntityTypes.CHICKEN
            || type == EntityTypes.GOAT
            || type == EntityTypes.DONKEY;
    }
}
