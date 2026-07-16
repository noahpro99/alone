package dev.alone.core.mixin;

import dev.alone.core.Domestic;
import dev.alone.core.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
 * <p>The same gate confines <b>domestic livestock</b> ({@link Domestic}) to villages — but livestock spawn
 * through <b>two</b> code paths and both must be plugged. The ongoing spawn loop runs through
 * {@code isValidSpawnPostitionForType} below (covered). The <b>world-generation herd pass</b>
 * ({@code spawnMobsForChunkGeneration}) is a <em>separate</em> method that never touches that check — it
 * validates with a static {@code SpawnPlacements.checkSpawnRules} call instead — so it was still seeding
 * fresh chunks with wild cows/sheep in the open, relying only on the unreliable {@code Mob.checkSpawnRules}
 * veto (see {@code MobSpawnMixin}). We redirect that static call too, position-accurately, so neither path
 * fields a barnyard animal away from a settlement.
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public class NaturalSpawnerStructureMixin {
    // Vanilla's own (typo'd) method name — the per-position, per-type validity check in the ongoing spawn loop.
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
                && Domestic.isDomesticType(data.type())
                && !Spawns.nearVillage(level, pos)) {
            cir.setReturnValue(false); // out in the wilds, far from any village — no barnyard animals
        }
    }

    /**
     * The WORLD-GEN herd pass validates each candidate with this static {@code checkSpawnRules(type, level,
     * reason, pos, random)} — the only per-position gate it has. We redirect it: a domestic type away from a
     * village is refused outright (no wild livestock seeded into new chunks), everything else defers to
     * vanilla. Position-accurate (the real {@code pos} argument), so it doesn't share the {@code Mob.checkSpawnRules}
     * holes. A village that isn't queryable this early just gets its herd from the ongoing loop at runtime.
     */
    @Redirect(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/SpawnPlacements;checkSpawnRules(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"))
    private static boolean alone$noWildLivestockAtChunkGen(EntityType<?> type, ServerLevelAccessor level,
            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (Domestic.isDomesticType(type)
                && level.getLevel() instanceof ServerLevel serverLevel
                && !Spawns.nearVillage(serverLevel, pos)) {
            return false; // farmed animals aren't seeded into the wilds at world-gen
        }
        return SpawnPlacements.checkSpawnRules(type, level, reason, pos, random);
    }
}
