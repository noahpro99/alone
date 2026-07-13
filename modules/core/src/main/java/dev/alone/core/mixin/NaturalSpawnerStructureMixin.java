package dev.alone.core.mixin;

import dev.alone.core.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Monsters only near loot structures (proposal §7.1) — the authoritative gate. This vetoes hostile
 * ({@link MobCategory#MONSTER}) natural spawns right at the spawn-position check, using the position and
 * category passed <b>as parameters</b>, so it doesn't depend on the mob being positioned yet or on which
 * spawn-reason enum the caller uses (the two failings of the earlier {@code Mob.checkSpawnRules} veto that
 * let field spawns slip through). If the spot isn't on or near a curated danger structure (see
 * {@link Spawns#nearStructure}), the whole category is refused there — so the open wilderness stays lonely
 * and monsters cluster at the ruins/dungeons/mineshafts.
 *
 * <p>The same gate confines <b>domestic livestock</b> ({@link dev.alone.core.Domestic}) to villages. The
 * livestock veto lived in {@code Mob.checkSpawnRules} (see {@code MobSpawnMixin}), but that check has the
 * very reliability holes this position-accurate mixin was built to close — so the worldgen herd pass was
 * still seeding plains with flocks in the open. Moving it here, keyed off the spawn-list {@link EntityType}
 * and the real spawn position, actually holds the line: no barnyard animals away from a settlement.
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public class NaturalSpawnerStructureMixin {
    // Vanilla's own (typo'd) method name — the per-position, per-type validity check in the spawn loop.
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void alone$structureConfinedSpawns(ServerLevel level, MobCategory category,
            StructureManager structures, ChunkGenerator generator, MobSpawnSettings.SpawnerData data,
            BlockPos.MutableBlockPos pos, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (category == MobCategory.MONSTER && !Spawns.nearStructure(level, pos)) {
            cir.setReturnValue(false); // out in the wilds, far from any ruin — no hostiles here
            return;
        }
        // Livestock are farmed, not wild — they only spawn at/around a village (breeding, spawn eggs and
        // structure placement go through other paths, so a herd you took home still works).
        if (category == MobCategory.CREATURE
                && dev.alone.core.Domestic.isDomesticType(data.type())
                && !Spawns.nearVillage(level, pos)) {
            cir.setReturnValue(false); // out in the wilds, far from any village — no barnyard animals
        }
    }
}
