package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/**
 * Insect pressure (roadmap: Wildlife &amp; hunting) — biting insects near standing water, and the
 * <b>smudge fire</b> that drives them off, exactly as in real bush life. Where mosquitoes and blackflies
 * actually breed and bite:
 * <ul>
 *   <li><b>Standing water</b> nearby (a pond, a swamp — not a lone puddle) is the nursery.</li>
 *   <li><b>Warmth:</b> they're active in <b>humid wetlands and jungles at any hour</b>, and in other warm
 *       country on <b>warm nights</b>; they die back in <b>winter</b> and the cold, and leave you alone
 *       while you're <b>in the water</b>.</li>
 * </ul>
 * When they're on you it's a <b>maintenance nuisance, not a wound</b>: the constant swatting and itching
 * <b>wears down your stamina</b> and, in a heavy swarm, leaves you briefly miserable and clumsy. The
 * counter is smoke: stand near a <b>lit campfire</b> (a smudge fire) and they keep their distance.
 * (Netting is a planned second counter.)
 */
public final class Insects {
    private Insects() {
    }

    private static final int SCAN = 60;          // check each player ~every 3s
    private static final int WATER_RADIUS = 5;   // still water within this breeds them
    private static final int SMUDGE_RADIUS = 5;  // a lit fire within this smokes them off
    private static final int MIN_WATER = 4;       // need a real body of standing water, not a puddle
    private static final float STAMINA_NIBBLE = 1.0f; // the toll of restless swatting, per scan

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % SCAN != 0 || player.isCreative() || player.isSpectator()) {
                    continue;
                }
                if (player.level() instanceof ServerLevel level) {
                    tick(level, player);
                }
            }
        });
    }

    private static void tick(ServerLevel level, ServerPlayer player) {
        if (Seasons.isWinter(level) || player.isInWater()) {
            return; // insects die back in the cold, and don't bite while you're submerged
        }
        BlockPos pos = player.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        boolean humid = biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
        boolean warm = biome.value().getBaseTemperature() > 0.6f;
        // Humid wetlands/jungles bite around the clock; other warm country only after dark.
        if (!humid && !(warm && !level.isBrightOutside())) {
            return;
        }
        if (countStillWater(level, pos) < MIN_WATER) {
            return; // no breeding water close by
        }
        if (nearSmudgeFire(level, pos)) {
            return; // a smoky fire keeps them off
        }

        // Bitten: harassed and swatting — it wears you down, and you can see the cloud around you.
        SurvivalMeters.exert(player, STAMINA_NIBBLE);
        level.sendParticles(ParticleTypes.ASH, player.getX(), player.getEyeY(), player.getZ(),
            6, 0.35, 0.35, 0.35, 0.0);
        // Every so often the swarm gets the better of you — a spell of miserable clumsiness, plus a hint.
        if (player.tickCount % (SCAN * 6) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, true));
            player.sendSystemMessage(
                Component.literal("Biting insects swarm you — a smoky fire would keep them off."), true);
        }
    }

    private static int countStillWater(ServerLevel level, BlockPos center) {
        int count = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -WATER_RADIUS; dx <= WATER_RADIUS; dx++) {
            for (int dz = -WATER_RADIUS; dz <= WATER_RADIUS; dz++) {
                for (int dy = -2; dy <= 1; dy++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    FluidState fs = level.getFluidState(p);
                    if (fs.is(FluidTags.WATER) && fs.isSource()) {
                        count++;
                        if (count >= MIN_WATER * 3) {
                            return count; // plenty of breeding water — stop scanning
                        }
                    }
                }
            }
        }
        return count;
    }

    private static boolean nearSmudgeFire(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -SMUDGE_RADIUS; dx <= SMUDGE_RADIUS; dx++) {
            for (int dz = -SMUDGE_RADIUS; dz <= SMUDGE_RADIUS; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.is(Blocks.CAMPFIRE) && s.getValue(BlockStateProperties.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
