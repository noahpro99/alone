package dev.alone.core.mixin;

import dev.alone.core.Campfires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Burns a lit campfire's fuel down each tick (proposal §3). When it's spent, the fire is <b>burnt out</b>
 * — the lay has gone to charcoal, so the campfire breaks and drops a piece of charcoal, and you must lay
 * a fresh one (you can't just relight the ash). {@code cookTick} is the lit-state ticker.
 */
@Mixin(CampfireBlockEntity.class)
public class CampfireCookTickMixin {
    /** cookTick runs every tick, so gate ignition low — a touching wall catches within a handful of seconds. */
    private static final int SPREAD_INTERVAL = 160;

    /** A flammable block directly touching the fire (sides or straight above) can catch. The block the
     *  campfire rests ON is spared, so a fire on a wooden floor doesn't instantly burn its own support. */
    private static void alone$trySpreadFire(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) {
                continue;
            }
            BlockPos neighbor = pos.relative(d);
            if (level.getBlockState(neighbor).ignitedByLava() && alone$ignite(level, neighbor)) {
                return; // one catch per attempt
            }
        }
    }

    /** Set an air cell touching the flammable block alight — real fire needs air to live in, and vanilla
     *  fire then burns/spreads per the world's own rules (and can be put out). */
    private static boolean alone$ignite(ServerLevel level, BlockPos flammable) {
        for (Direction d : Direction.values()) {
            BlockPos p = flammable.relative(d);
            if (level.getBlockState(p).isAir()) {
                level.setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
                level.playSound(null, p, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.9f, 1.0f);
                return true;
            }
        }
        return false;
    }

    @Inject(method = "cookTick", at = @At("HEAD"))
    @SuppressWarnings("rawtypes")
    private static void alone$burnFuel(ServerLevel level, BlockPos pos, BlockState state,
                                       CampfireBlockEntity entity, RecipeManager.CachedCheck cache, CallbackInfo ci) {
        // An open fire can't survive the rain (proposal §3.1). isRainingAt already requires open sky and a
        // rainy biome, so a campfire under a roof (or in the desert/snow) is spared — shelter your fire.
        // It hisses and steams for a few seconds, then goes out; the lay is only doused, not spent, so the
        // fuel stays banked and you can relight it once things dry out (and drilling fails in rain anyway).
        if (level.isRainingAt(pos.above())) {
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                4, 0.16, 0.05, 0.16, 0.008);
            if (level.getRandom().nextInt(60) == 0) { // ~3s of exposure on average before it dies
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f);
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, Boolean.FALSE));
                return;
            }
        }

        int fuel = Campfires.getFuel(entity);
        if (fuel <= 0) {
            // Burnt out — the wood is spent. Drop any pot that was boiling on it, break the campfire (no
            // campfire drop), and leave charcoal.
            ItemStack boilingPot = entity.getAttached(Campfires.BOIL_ITEM);
            if (boilingPot != null) {
                // If the water had already come to a boil, don't lose it to the burnout — credit it clean
                // (or drop the salt crust) before the pot falls out, same as lifting it out by hand would.
                if (entity.getAttachedOrElse(Campfires.BOIL_LEFT, -1) <= 0) {
                    Campfires.finishBoiledPot(level, pos, boilingPot);
                }
                Block.popResource(level, pos, boilingPot);
            }
            level.destroyBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(Items.CHARCOAL));
            return;
        }
        Campfires.setFuel(entity, fuel - 1);

        // An open fire is a real hazard (proposal §5.3): a flammable block touching it — a log wall, a
        // plank ceiling, dry thatch — can catch alight. Keep your fire clear of wood (one block is enough)
        // or ring it with stone. We just set an adjacent fire; the world's own fire rules take it from there.
        if (!level.isRainingAt(pos.above()) && level.getRandom().nextInt(SPREAD_INTERVAL) == 0) {
            alone$trySpreadFire(level, pos);
        }

        // A pot standing in the fire comes to a boil over time (only while lit); steam wisps up as it works.
        int boilLeft = entity.getAttachedOrElse(Campfires.BOIL_LEFT, -1);
        if (boilLeft > 0) {
            entity.setAttached(Campfires.BOIL_LEFT, boilLeft - 1);
            entity.setChanged();
            if (level.getGameTime() % 5L == 0L) {
                level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                    2, 0.1, 0.02, 0.1, 0.01);
            }
        }

        // The fire's smoke reads its life: a well-fed blaze billows, a dying one barely wisps. Scaled by
        // how much fuel is banked (server-sent, so every nearby player sees the same living fire).
        float life = Math.min(1f, fuel / (float) Campfires.INITIAL_FUEL);
        if (life > 0.05f && level.getGameTime() % 8L == 0L) {
            int puffs = Math.round(life * 3f);
            if (puffs > 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    puffs, 0.12, 0.02 + 0.06 * life, 0.12, 0.006 + 0.02 * life);
            }
        }
    }
}
