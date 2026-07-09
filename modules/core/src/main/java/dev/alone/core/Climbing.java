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
 *   <li><b>Leaves</b> — a canopy is soft enough to haul yourself up through (passable + climbable,
 *       no stamina cost); pushing/holding up inside foliage lets you ascend a tree.</li>
 *   <li><b>Sheer walls</b> — you can scrabble up a <em>flat</em> full-block face, but only about
 *       three blocks (a wall that tops out within reach); it's strenuous, draining stamina as you
 *       haul yourself up, and you can't do it once you're spent.</li>
 * </ul>
 * The climbable state is granted by {@code LivingEntityClimbMixin} (which flips vanilla
 * {@code onClimbable}); this class holds the predicate and the stamina cost.
 */
public final class Climbing {
    private Climbing() {
    }

    /** How high a bare wall you can free-climb — a flat face that ends within this many blocks. */
    public static final int MAX_WALL_CLIMB = 3;
    private static final float CLIMB_MIN_STAMINA = 5f;      // too spent to pull yourself up
    private static final float CLIMB_STAMINA_DRAIN = 0.7f;  // hauling up a wall is hard work (~3 blocks ≈ 20 stamina)

    public static void init() {
        // Stamina cost is applied server-side while actually hauling up a wall (not on leaves/ladders).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.onClimbable() && player.horizontalCollision
                    && player.getDeltaMovement().y > 0.0 && !inLeaves(player)) {
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
        if (inLeaves(player)) {
            return true; // soft canopy — free to climb through
        }
        // Free-climbing a wall: only while pressed into it, with stamina to spare, and only walls ≤3 tall.
        if (!player.horizontalCollision || SurvivalMeters.getStamina(player) <= CLIMB_MIN_STAMINA) {
            return false;
        }
        return wallClimbable(player);
    }

    /** You're inside (or your head is inside) foliage. */
    private static boolean inLeaves(Player player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        return level.getBlockState(feet).is(BlockTags.LEAVES)
            || level.getBlockState(feet.above()).is(BlockTags.LEAVES);
    }

    /**
     * A flat full-block wall you're facing that tops out within {@link #MAX_WALL_CLIMB} blocks — so you
     * can reach the lip and mantle over. Taller cliffs can't be free-climbed.
     */
    private static boolean wallClimbable(Player player) {
        Level level = player.level();
        Direction dir = player.getDirection(); // the wall you're facing into
        BlockPos feet = player.blockPosition();
        boolean grip = isFlatWall(level, feet.relative(dir)) || isFlatWall(level, feet.above().relative(dir));
        if (!grip) {
            return false;
        }
        for (int h = 1; h <= MAX_WALL_CLIMB; h++) {
            if (!isFlatWall(level, feet.above(h).relative(dir))) {
                return true; // found the top within reach
            }
        }
        return false; // wall keeps going past what you can scale
    }

    /** A full solid cube presents a flat face you can get purchase on (stairs/fences/leaves don't). */
    private static boolean isFlatWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos);
    }
}
