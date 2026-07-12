package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Iron golems fight like the multi-ton iron constructs they are, so they can't be cheesed (progression /
 * realism — a golem should still <b>drop its iron</b> when killed, but taking it must be a real, dangerous
 * fight, not a safe farm; see the realistic-drops principle). The classic exploit is to hurt a golem from
 * behind a one-block wall or through a small hole its pathfinder can't cross. Here a golem <b>smashes
 * through weak cover</b> to reach a target it can't otherwise get at: the dirt, wood, or stone in the way
 * is pulverised, opening a path to you — you can't poke a giant iron construct through a gap it can't reach
 * through. Metal, obsidian, and deepslate are too tough for even a golem to shrug aside, so a real fortified
 * bunker still holds; a thrown-up dirt wall does not.
 *
 * <p>Only fires while the golem has a target it can't see (something's in the way) and only touches blocks
 * right next to it, so it opens a path rather than levelling the countryside. (Hitting a target standing
 * above the golem is a separate piece, still to come.)
 */
public final class Golems {
    private Golems() {
    }

    private static final float MAX_HARDNESS = 3.0f;   // earth/wood/stone tier — not metal, obsidian, deepslate
    private static final double SMASH_RANGE_SQ = 9.0;  // only cover within ~3 blocks of the golem
    private static final int SMASH_INTERVAL = 15;      // pulverise ~one block every 0.75s (tune in playtest)

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof IronGolem golem) {
                golem.getGoalSelector().addGoal(2, new SmashThroughGoal(golem));
            }
        });
    }

    /** Breaks weak blocks between the golem and a target it can't reach — the anti-cheese. Non-exclusive
     *  (no flags), so it runs alongside the golem's normal move/attack goals rather than replacing them. */
    private static class SmashThroughGoal extends Goal {
        private final IronGolem golem;
        private int cooldown;

        SmashThroughGoal(IronGolem golem) {
            this.golem = golem;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = golem.getTarget();
            return target != null && target.isAlive() && golem.distanceToSqr(target) <= 64.0
                && !golem.getSensing().hasLineOfSight(target); // can't see it — something's in the way
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            LivingEntity target = golem.getTarget();
            if (target == null) {
                return;
            }
            Level level = golem.level();
            BlockHitResult hit = level.clip(new ClipContext(golem.getEyePosition(), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return; // clear line to the target — nothing in the way, so nothing to smash
            }
            BlockPos pos = hit.getBlockPos();
            if (golem.distanceToSqr(Vec3.atCenterOf(pos)) > SMASH_RANGE_SQ) {
                return; // the obstruction is too far to be the cheese wall — leave the world alone
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0f || hardness > MAX_HARDNESS) {
                return; // air, unbreakable (bedrock), or too tough for even a golem to force through
            }
            golem.swing(InteractionHand.MAIN_HAND);
            level.destroyBlock(pos, false, golem, 512); // pulverised — plays the break effect itself
            cooldown = SMASH_INTERVAL;
        }
    }
}
