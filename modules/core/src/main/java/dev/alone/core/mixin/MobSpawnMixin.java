package dev.alone.core.mixin;

import dev.alone.core.Spawns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Monsters only near loot structures (proposal §7.1). We veto <b>natural</b> spawns of hostile mobs
 * (anything implementing {@link Enemy}) that aren't within a structure's footprint, so the open
 * wilderness stays lonely and danger concentrates at ruins/dungeons/villages. Only {@code NATURAL}
 * spawns are touched — spawners, structure placement, breeding, reinforcements, and passive wildlife
 * all behave normally. {@code Monster}/its subclasses don't override {@code checkSpawnRules}, so this
 * single mixin covers every vanilla hostile.
 */
@Mixin(Mob.class)
public class MobSpawnMixin {
    @Inject(method = "checkSpawnRules", at = @At("RETURN"), cancellable = true)
    private void alone$structureOnlySpawns(LevelAccessor levelAccessor, EntitySpawnReason reason,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        boolean natural = reason == EntitySpawnReason.NATURAL;
        // Passive animals are placed mostly by the WORLD-GEN herd pass (CHUNK_GENERATION), not the ongoing
        // NATURAL loop — so the livestock veto has to catch both, or fresh chunks fill with wild cows/pigs.
        boolean worldgen = reason == EntitySpawnReason.CHUNK_GENERATION;
        if (!natural && !worldgen) {
            return; // leave breeding, spawners, spawn eggs, structure/village placement, patrols… untouched
        }
        Mob self = (Mob) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        // Domestic livestock are FARMED, not wild — real cows/pigs/sheep/chickens don't roam the
        // wilderness, humans keep them. So their spawns are only allowed at/around a village, on BOTH the
        // worldgen herd pass and the natural loop: the settlement is the economy source you visit to acquire
        // livestock, then breed and farm at home (breeding, spawners, structure/village placement are
        // untouched, so a herd you took home still works).
        if (dev.alone.core.Domestic.isDomestic(self)) {
            if (!Spawns.nearVillage(level, self.blockPosition())) {
                cir.setReturnValue(false); // out in the wilds, far from any village — no barnyard animals
            }
            return;
        }
        // Hostiles and wild-game thinning below only concern the ongoing NATURAL spawn loop, not worldgen.
        if (!natural) {
            return;
        }
        // Hostiles only spawn near loot structures — the open wilds stay lonely.
        if (self instanceof Enemy) {
            if (!Spawns.nearStructure(level, self.blockPosition())) {
                cir.setReturnValue(false); // out in the wilds, far from any ruin — nothing spawns
            }
            return;
        }
        // Wild game thins out where it's been hunted hard — overhunt a patch and it stops spawning there
        // until the population recovers (see GameStock), so you work a range instead of stripping one valley.
        if (dev.alone.core.GameStock.isGame(self)
            && !dev.alone.core.GameStock.spawnAllowed(level, self.blockPosition(), self.getRandom())) {
            cir.setReturnValue(false);
        }
    }
}
