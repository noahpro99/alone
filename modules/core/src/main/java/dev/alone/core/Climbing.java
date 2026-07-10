package dev.alone.core;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
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
    /** Steady upward pace while cresting a wall's lip, so the climb lifts you up and over the edge. */
    public static final double TOP_OUT_LIFT = 0.16;
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
    private static final int GRIP_GRACE = 20;
    private static final Map<Player, Integer> LAST_GRIP_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_GRIP_SERVER = new WeakHashMap<>();

    // The climb latch: once you've caught a wall you STAY caught while a climbable face is beside you,
    // instead of re-earning the grab every tick off the flickery horizontalCollision flag. This is what
    // stops the jitter — the grab decision is made once (engage), then held on a stable block scan.
    private static final Map<Player, Boolean> GRIP_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Boolean> GRIP_SERVER = new WeakHashMap<>();

    /** Surfaces rough enough to free-climb: raw rock, tree trunks, earth — never smooth/finished faces.
     *  Datapack-defined ({@code data/alone/tags/block/climbable.json}), so the pack can tune it. */
    private static final TagKey<Block> CLIMBABLE_ROUGH =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("alone", "climbable"));

    private static boolean isGripping(Player player) {
        return (player.level().isClientSide() ? GRIP_CLIENT : GRIP_SERVER).getOrDefault(player, false);
    }

    private static void setGripping(Player player, boolean gripping) {
        (player.level().isClientSide() ? GRIP_CLIENT : GRIP_SERVER).put(player, gripping);
    }

    // A slip: even a skilled climber occasionally loses a hold. Rolled per tick on a bare-rock climb,
    // rare when fresh and more likely as you tire. On a slip you come off the wall and fall — which is
    // exactly why a rope or ladder is the safe way up. Server rolls it (so the fall counts for damage)
    // and tells the client (so its predicted fall matches — no rubber-band).
    private static final float SLIP_BASE = 0.0006f;   // fresh & rested: ~1.2% per second of clinging
    private static final float SLIP_TIRED = 0.0034f;  // the more spent you are, the more your grip fails
    private static final int SLIP_RECOVER = 12;       // ticks you can't re-grab after a slip — you fall first
    private static final Map<Player, Integer> SLIP_UNTIL_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Integer> SLIP_UNTIL_SERVER = new WeakHashMap<>();

    private static boolean slipping(Player player) {
        Map<Player, Integer> map = player.level().isClientSide() ? SLIP_UNTIL_CLIENT : SLIP_UNTIL_SERVER;
        return player.tickCount < map.getOrDefault(player, Integer.MIN_VALUE);
    }

    /** Roll for a grip slip while free-climbing bare rock; on a slip, drop off the wall and fall. */
    private static void maybeSlip(ServerPlayer player) {
        if (slipping(player)) {
            return; // already coming off the wall
        }
        float frac = Math.max(0f, Math.min(1f,
            SurvivalMeters.getStamina(player) / SurvivalMeters.MAX_STAMINA));
        float chance = SLIP_BASE + SLIP_TIRED * (1f - frac);
        if (player.getRandom().nextFloat() >= chance) {
            return;
        }
        setGripping(player, false);
        SLIP_UNTIL_SERVER.put(player, player.tickCount + SLIP_RECOVER);
        player.level().playSound(null, player.blockPosition(),
            net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS,
            0.5f, 1.4f);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Your grip slips!"), true);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
            player, dev.alone.core.net.ClimbSlipPayload.INSTANCE);
    }

    /** Client side of a slip: drop the local grip and start falling in step with the server. */
    public static void clientSlip(Player player) {
        setGripping(player, false);
        SLIP_UNTIL_CLIENT.put(player, player.tickCount + SLIP_RECOVER);
    }

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
            maybeSlip(player); // even a skilled climber occasionally loses a hold and falls
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
            setGripping(player, false);
            return false;
        }
        // Too heavily loaded to pull yourself up anything — leaves or rock.
        if (Carry.totalWeight(player) > CLIMB_MAX_WEIGHT) {
            setGripping(player, false);
            return false;
        }
        // Both a canopy and a bare wall need stamina — run out and you lose your grip and fall. (The
        // client mirrors synced stamina onto the player, so both sides gate on the real value.)
        if (SurvivalMeters.getStamina(player) <= CLIMB_MIN_STAMINA) {
            setGripping(player, false);
            return false;
        }
        // Just slipped — you're falling for a moment and can't catch anything until you recover.
        if (slipping(player)) {
            return false;
        }
        if (inLeaves(player)) {
            return true; // a tree you can haul up through (costs stamina; see the tick + speed factor)
        }
        // A wall you can actually climb: a clear ≥2-tall rough face, or the last block of one you're
        // topping out. A lone 1-block step is neither, so it's ignored — you just jump it. No face → let
        // go of any grip.
        if (!hasClimbableWall(player)) {
            setGripping(player, false);
            return false;
        }
        // Already latched onto this wall? Stay latched. This is the anti-jitter core: we DON'T re-check
        // horizontalCollision every tick (it blinks off whenever you're not driving into the face), so the
        // grip no longer flickers. It holds until the wall runs out, stamina fails, or you climb clear.
        if (isGripping(player)) {
            gripMap(player).put(player, player.tickCount); // keep the top-out grace fresh while climbing
            return true;
        }
        // Not yet on the wall — engage when you press into it and aren't mid-jump. Standing beside a wall
        // without pushing into it does nothing (no auto-climb); a live jump keeps its momentum.
        boolean engage = player.horizontalCollision
            && player.getDeltaMovement().y <= CLIMB_ENGAGE_VY
            && !jumpLatched(player);
        if (engage) {
            setGripping(player, true);
            gripMap(player).put(player, player.tickCount);
            return true;
        }
        return false;
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
        if (!recentlyGripped(player)) {
            return false; // a lone step you weren't already climbing — just jump it
        }
        // Finishing a climb: the top block of the wall (foot hold, clear above), OR the block just
        // above the lip (nothing at your feet yet, but the wall you climbed is right below) — so the
        // climb carries you all the way up and over the edge instead of stranding you under it.
        boolean belowFoot = isFlatWall(level, feet.below().relative(dir));
        return (foot && !head) || (!foot && belowFoot);
    }

    /** In the finishing zone of a climb — cresting the wall's lip — where the climb should lift you
     *  steadily up and over rather than clamp you to the wall face. */
    public static boolean isToppingOut(Player player) {
        return !inLeaves(player) && recentlyGripped(player) && hasClimbableWall(player) && isCresting(player);
    }

    private static boolean isCresting(Player player) {
        Level level = player.level();
        Direction dir = player.getDirection();
        BlockPos feet = player.blockPosition();
        boolean head = isFlatWall(level, feet.above().relative(dir));
        return !head; // no wall at head height → you're at/over the top, not mid-face
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

    /** A grippable face: a full solid cube (stairs/fences/leaves don't count) that's also <b>rough</b>
     *  enough to find holds on — raw rock, tree trunks, earth. You can't free-climb a smooth, finished
     *  face (planks, bricks, glass, metal, polished stone); for those you need a rope or ladder. */
    private static boolean isFlatWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos) && state.is(CLIMBABLE_ROUGH);
    }
}
