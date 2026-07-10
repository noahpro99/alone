package dev.alone.core;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    /** Per-block stamina cost of hauling up a bare wall — brutal: a full bar buys ~4 blocks. */
    private static final float WALL_DRAIN_PER_BLOCK = 25f;
    /** Only start clinging once your jump momentum is spent — below this upward speed. Lets a normal
     *  jump carry you the first block near a wall before the (slower) climb takes over. */
    private static final double CLIMB_ENGAGE_VY = 0.1;
    /** Per-block cost of climbing a tree — hard, but a full ~6 m tree stays doable on one wind. */
    private static final float LEAF_DRAIN_PER_BLOCK = 12f;
    /** Per-tick grip cost of hanging on a rope — cheap (rope is easy), so it's the smart way down a cave. */
    private static final float ROPE_HOLD_DRAIN = 0.15f;
    /** Crouch to lower yourself down a rope at a controlled speed; otherwise you hold your spot. */
    public static final double ROPE_DESCEND_SPEED = -0.16;
    /** Above this per-tick rise it's a jump/fall, not a climb — don't charge for it. */
    private static final double CLIMB_MOVE_CAP = 0.2;

    /** Last server Y per player, to measure how far they've actually climbed (movement is client-driven,
     *  so {@code deltaMovement}/{@code horizontalCollision}/{@code onClimbable} are unreliable server-side). */
    private static final Map<Player, Double> LAST_Y = new WeakHashMap<>();

    // "Live jump momentum" latch: true from the moment you jump until the jump peaks. While latched we
    // don't grab the wall, so a jump keeps its FULL upward momentum near a wall instead of being clamped
    // to climb speed. Side-separated: the client (which drives movement) has the reliable velocity.
    private static final Map<Player, Boolean> JUMP_LATCH_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Boolean> JUMP_LATCH_SERVER = new WeakHashMap<>();

    // A one-block step and the top block of a tall wall look identical (solid at foot, air above), so we
    // tell them apart by state: the "top block" is only climbable if you were gripping a full (≥2-tall)
    // wall within the last few ticks — i.e. you're finishing a climb, not fresh-jumping at a lone step.
    private static final int GRIP_GRACE = 8;
    private static final Map<Player, Integer> LAST_GRIP_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_GRIP_SERVER = new WeakHashMap<>();

    public static void init() {
        // Charge stamina off real vertical travel while against a climbable surface — the one signal the
        // server can trust for a client-driven player.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateJumpLatch(player);
                climbDrainTick(player);
                applyLeafSlowdown(player);
                // Hanging on a rope takes grip — a small steady stamina cost, far below free-climbing.
                if (onRope(player) && !player.onGround() && !player.getAbilities().flying) {
                    SurvivalMeters.exert(player, ROPE_HOLD_DRAIN);
                }
            }
        });
    }

    private static final Identifier LEAF_SLOW_MODIFIER = Identifier.fromNamespaceAndPath("alone", "leaf_slow");
    private static final double LEAF_SPEED_FACTOR = -0.5; // pushing through a canopy is half-speed, tugging work

    /** Wading through dense foliage is slow going — apply a speed penalty while you're inside leaves. */
    private static void applyLeafSlowdown(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (inLeaves(player) && !player.getAbilities().flying && !player.isSpectator()) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                LEAF_SLOW_MODIFIER, LEAF_SPEED_FACTOR, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(LEAF_SLOW_MODIFIER);
        }
    }

    /**
     * Maintain the jump-momentum latch — call once per tick per side (server loop + client tick). Latched
     * the instant you leave the ground rising, cleared once you stop rising (apex), so the whole ascent of
     * a jump is protected from the climb clamp.
     */
    public static void updateJumpLatch(Player player) {
        Map<Player, Boolean> map = player.level().isClientSide() ? JUMP_LATCH_CLIENT : JUMP_LATCH_SERVER;
        if (player.onGround()) {
            map.put(player, false);
            return;
        }
        double vy = player.getDeltaMovement().y;
        boolean latched = map.getOrDefault(player, false);
        if (vy > CLIMB_ENGAGE_VY) {
            latched = true;  // a real upward burst — you jumped
        } else if (vy <= 0.0) {
            latched = false; // you've stopped rising; the jump is spent, grabbing is fair game
        }
        map.put(player, latched);
    }

    private static boolean jumpLatched(Player player) {
        return (player.level().isClientSide() ? JUMP_LATCH_CLIENT : JUMP_LATCH_SERVER)
            .getOrDefault(player, false);
    }

    private static void climbDrainTick(ServerPlayer player) {
        double y = player.getY();
        Double prev = LAST_Y.put(player, y);
        if (prev == null || player.onGround() || player.isInWater()
            || player.getAbilities().flying || player.isSpectator()) {
            return;
        }
        double dy = y - prev;
        double ady = Math.abs(dy);
        if (ady < 1.0e-4 || ady > CLIMB_MOVE_CAP) {
            return; // standing still, or a jump/fall rather than a controlled climb
        }
        if (onRope(player)) {
            return; // rope grip is charged per-tick in the main loop, not by distance
        }
        if (inLeaves(player)) {
            if (dy > 0) { // hauling up a tree; dropping down through it is free
                SurvivalMeters.exert(player, (float) (dy * LEAF_DRAIN_PER_BLOCK));
            }
        } else if (facingFlatWall(player)) {
            SurvivalMeters.exert(player, (float) (ady * WALL_DRAIN_PER_BLOCK)); // rock burns up or down
        }
    }

    /** You're hanging on a rope line. */
    public static boolean onRope(Player player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        return level.getBlockState(feet).is(AloneBlocks.ROPE)
            || level.getBlockState(feet.above()).is(AloneBlocks.ROPE);
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
        // A wall you can actually climb: a clear ≥2-tall face, or the last block of one you're already
        // topping out. A lone 1-block step is neither, so it's ignored — you just jump it.
        if (!hasClimbableWall(player)) {
            return false;
        }
        // Only while pressed into it, airborne, and not while a jump is still carrying you up. Requiring
        // you to be off the ground is what stops the jitter: standing at the foot of a wall no longer
        // auto-grabs and creeps you upward — you must actively hop/leap to catch it, then the climb takes
        // over once you've peaked. The instantaneous velocity check kills the fast rise (no stale state);
        // the latch covers the slow approach to the apex.
        boolean canGrab = !player.onGround()
            && player.horizontalCollision
            && player.getDeltaMovement().y <= CLIMB_ENGAGE_VY
            && !jumpLatched(player);
        if (canGrab) {
            gripMap(player).put(player, player.tickCount); // remember the hold so we can finish the last block
        }
        return canGrab;
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
        return hasClimbableWall(player);
    }

    /**
     * A wall you can climb here: a clear <b>≥2-tall</b> flat face at foot+head height, OR the last block
     * of a wall you were <b>just gripping</b> (topping out). A lone 1-block step is neither — so it's left
     * to a normal jump rather than being mistaken for a climb.
     */
    private static boolean hasClimbableWall(Player player) {
        Level level = player.level();
        Direction dir = player.getDirection();
        BlockPos feet = player.blockPosition();
        boolean foot = isFlatWall(level, feet.relative(dir));
        boolean head = isFlatWall(level, feet.above().relative(dir));
        if (foot && head) {
            return true; // an unambiguous, at-least-2-tall wall
        }
        return foot && !head && recentlyGripped(player); // finishing the top block of a climb
    }

    private static boolean recentlyGripped(Player player) {
        return player.tickCount - gripMap(player).getOrDefault(player, -GRIP_GRACE - 1) <= GRIP_GRACE;
    }

    private static Map<Player, Integer> gripMap(Player player) {
        return player.level().isClientSide() ? LAST_GRIP_CLIENT : LAST_GRIP_SERVER;
    }

    /** Controlled downward speed when you crouch to lower yourself through a canopy (scaffolding-like). */
    public static final double LEAF_DESCEND_SPEED = -0.16;

    /** Crouching inside a canopy to climb down through it, like sneaking down scaffolding. */
    public static boolean isDescendingLeaves(Player player) {
        return player.isShiftKeyDown() && inLeaves(player);
    }

    /** You're inside (or your head is inside) foliage. */
    public static boolean inLeaves(Player player) {
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
        boolean foot = isFlatWall(level, feet.relative(dir));
        boolean head = isFlatWall(level, feet.above().relative(dir));
        if (!foot && !head) {
            return false;
        }
        // A one-block-tall step while standing on the ground (solid at foot height, open above it) is
        // just a hop — a normal jump handles it. Don't engage the climb for single-block verticals.
        if (player.onGround() && foot && !head) {
            return false;
        }
        return true;
    }

    /** A full solid cube presents a flat face you can grip (stairs/fences/leaves don't). */
    private static boolean isFlatWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos);
    }
}
