package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Free-climbing (proposal §5.4 / realism). Two ways up the world beyond ladders:
 * <ul>
 *   <li><b>Leaves</b> — a canopy is soft enough to haul yourself up through (scaffolding-like:
 *       stand on top, climb up/crouch down through it); no stamina cost.</li>
 *   <li><b>Sheer walls</b> — you can scrabble up or down a <em>flat</em> full-block face of any
 *       height. It's slow, rock-climbing-pace work that drains stamina every tick you cling; when
 *       you run out of stamina you lose your grip and fall. Haul too much weight and you can't climb
 *       at all.</li>
 * </ul>
 * The climbable state is granted by {@code LivingEntityClimbMixin} (which flips vanilla
 * {@code onClimbable}) and slowed by its {@code handleOnClimbable} hook; this class holds the
 * predicate, the stamina cost, and the weight limit.
 */
public final class Climbing {
    private Climbing() {
    }

    private static final float CLIMB_MIN_STAMINA = 5f;      // too spent to hold your grip → you fall
    private static final float CLIMB_STAMINA_DRAIN = 2.0f;  // clinging to bare rock is brutal, every tick
    private static final float LEAF_CLIMB_DRAIN = 0.6f;     // hauling up a tree is hard work too (a ~6 m tree ≈ most of your wind)
    private static final float CLIMB_MAX_WEIGHT = 25f;      // more than ~one block's mass and you can't haul up at all
    /** Wall-climbing pace as a fraction of ladder speed — slow, deliberate bare-rock climbing. */
    public static final double WALL_CLIMB_SPEED = 0.3;
    /** Tree/canopy pace — slow, but a touch quicker than bare rock since branches give you holds. */
    public static final double LEAF_CLIMB_SPEED = 0.4;

    public static void init() {
        // Stamina cost is applied server-side while clinging to a bare wall (leaves/ladders are free),
        // in either direction — hauling up or lowering yourself down both burn.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.onClimbable()) {
                    continue;
                }
                if (inLeaves(player)) {
                    // Hauling yourself up a tree is tiring; drifting/dropping down through it is free.
                    if (player.getDeltaMovement().y > 0.0) {
                        SurvivalMeters.exert(player, LEAF_CLIMB_DRAIN);
                    }
                } else if (player.horizontalCollision) {
                    // Clinging to bare rock burns in either direction.
                    SurvivalMeters.exert(player, CLIMB_STAMINA_DRAIN);
                }
            }
        });
    }

    /** Can this player cling here right now? Called from the {@code onClimbable} mixin. */
    public static boolean canClimb(Player player) {
        if (player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        // Too heavily loaded to pull yourself up anything — leaves or rock.
        if (Carry.totalWeight(player) > CLIMB_MAX_WEIGHT) {
            return false;
        }
        // Both a canopy and a bare wall need stamina — run out and you lose your grip and fall.
        if (SurvivalMeters.getStamina(player) <= CLIMB_MIN_STAMINA) {
            return false;
        }
        if (inLeaves(player)) {
            return true; // a tree you can haul up through (costs stamina; see the tick + speed factor)
        }
        // Free-climbing a wall: only while pressed into it. No height limit — you climb as far as your
        // stamina lasts, and fall when it's gone.
        return player.horizontalCollision && facingFlatWall(player);
    }

    /** Our climb speed as a fraction of ladder speed: slow for bare rock, a touch quicker for a tree,
     *  full (unchanged) for a real ladder/vine. Used by {@code handleOnClimbable} to slow the climb. */
    public static double climbSpeedFactor(Player player) {
        if (inLeaves(player)) {
            return LEAF_CLIMB_SPEED;
        }
        if (isWallClimbing(player)) {
            return WALL_CLIMB_SPEED;
        }
        return 1.0; // a real ladder/vine/scaffolding keeps vanilla speed
    }

    /** True when the player's current climb is a bare wall (not leaves, not a real ladder). */
    public static boolean isWallClimbing(Player player) {
        if (inLeaves(player) || !player.horizontalCollision) {
            return false;
        }
        // A real ladder/vine/scaffolding runs at its own vanilla speed — don't touch those.
        if (player.level().getBlockState(player.blockPosition()).is(BlockTags.CLIMBABLE)) {
            return false;
        }
        return facingFlatWall(player);
    }

    /** You're inside (or your head is inside) foliage. */
    private static boolean inLeaves(Player player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        return level.getBlockState(feet).is(BlockTags.LEAVES)
            || level.getBlockState(feet.above()).is(BlockTags.LEAVES);
    }

    /** There's a flat full-block face to get purchase on at hand or head height, in the direction you face. */
    private static boolean facingFlatWall(Player player) {
        Level level = player.level();
        Direction dir = player.getDirection();
        BlockPos feet = player.blockPosition();
        return isFlatWall(level, feet.relative(dir)) || isFlatWall(level, feet.above().relative(dir));
    }

    /** A full solid cube presents a flat face you can grip (stairs/fences/leaves don't). */
    private static boolean isFlatWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos);
    }
}
