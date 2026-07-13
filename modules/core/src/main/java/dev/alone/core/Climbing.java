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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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
    /** Wall-climbing pace as a fraction of ladder speed — slow, deliberate bare-rock climbing. The lip is
     *  climbed at this same pace; we never add an upward push of our own (that used to launch you over). */
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
    private static final int GRIP_GRACE = 20;
    private static final Map<Player, Integer> LAST_GRIP_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_GRIP_SERVER = new WeakHashMap<>();

    // The climb latch: once you've caught a wall you STAY caught while a climbable face is beside you,
    // instead of re-earning the grab every tick off the flickery horizontalCollision flag. This is what
    // stops the jitter — the grab decision is made once (engage), then held on a stable block scan.
    private static final Map<Player, Boolean> GRIP_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Boolean> GRIP_SERVER = new WeakHashMap<>();

    // Deliberate initiation: a free-climb is STARTED by pressing JUMP against the rock, never automatically
    // by brushing into it. We stamp the tick of the last jump-key press per side; a grab may only engage
    // within JUMP_GRACE ticks of it (long enough that the jump can carry you a block, peak, and then the
    // slower climb takes over — see the engage logic in canClimb). Client edge-detects the key and mirrors
    // it to the server (movement is client-authoritative), so both sides gate initiation on the same intent.
    private static final int JUMP_GRACE = 10;
    private static final Map<Player, Integer> JUMP_PRESS_CLIENT = new WeakHashMap<>();
    private static final Map<Player, Integer> JUMP_PRESS_SERVER = new WeakHashMap<>();

    /** How straight-on you must be looking at the rock to grab or hold it: the view vector must lie within
     *  ~40° of the wall's inward normal (i.e. of the direction into the face). {@code cos(40°) ≈ 0.766}.
     *  Real free-climbing, you face the rock — you can't cling to a wall you're looking 90° away from, or
     *  steeply up/down past. This is the pitch-and-yaw gate the old cardinal-only check was missing. */
    private static final double FACE_MIN_DOT = 0.77;

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
        // Deliberate initiation (§5.4): the client tells us the instant JUMP is pressed, so the server's
        // climb grant agrees with the client that actually moves the player up the wall.
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            dev.alone.core.net.ClimbJumpPayload.TYPE,
            (payload, context) -> noteJumpPressed(context.player()));

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

    /**
     * Record that the player just pressed the JUMP key — the deliberate trigger that lets a free-climb
     * <em>start</em>. Called client-side on the key's press edge (see {@code AloneCoreClient}) and again
     * server-side when that intent arrives over {@link dev.alone.core.net.ClimbJumpPayload}, so each side
     * gates initiation on the same press.
     */
    public static void noteJumpPressed(Player player) {
        (player.level().isClientSide() ? JUMP_PRESS_CLIENT : JUMP_PRESS_SERVER)
            .put(player, player.tickCount);
    }

    /** True within {@link #JUMP_GRACE} ticks of the last jump press — the window in which a wall grab may
     *  engage. A stale stamp from a respawned player reads as a negative age (the clock restarts at 0), so
     *  it correctly counts as "not recent" and never phantom-grabs. */
    private static boolean jumpedRecently(Player player) {
        Integer at = (player.level().isClientSide() ? JUMP_PRESS_CLIENT : JUMP_PRESS_SERVER).get(player);
        if (at == null) {
            return false;
        }
        int age = player.tickCount - at;
        return age >= 0 && age <= JUMP_GRACE;
    }

    /**
     * Are you looking roughly straight at the rock? The view vector (pitch <em>and</em> yaw) must lie
     * within ~40° of the direction into the wall — {@link #FACE_MIN_DOT}. This is what stops a climb while
     * you're looking 90° away or steeply up/down; the old check only compared cardinal facing and ignored
     * where the camera actually pointed.
     */
    private static boolean facingWall(Player player, Direction dir) {
        Vec3 look = player.calculateViewVector(player.getXRot(), player.getYRot());
        Vec3 into = dir.getUnitVec3();
        return look.dot(into) >= FACE_MIN_DOT;
    }

    /**
     * Are you pressed against the wall, walking into it? You have to drive into the rock to climb it —
     * standing beside it does nothing. {@code horizontalCollision} means something is blocking you in the
     * direction you're moving (both sides can see it); where the raw movement input is also available
     * (client-authoritative — the server sees ~0 for a player), we additionally require the wish direction
     * to point <em>into</em> the face, so backing away or strafing along it won't hold a grip.
     */
    private static boolean pressingIntoWall(Player player, Direction dir) {
        if (!player.horizontalCollision) {
            return false;
        }
        double strafe = player.xxa;
        double forward = player.zza;
        if (Math.abs(strafe) < 1.0e-3 && Math.abs(forward) < 1.0e-3) {
            return true; // no input signal (server side) — the collision alone stands in for "walking into it"
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        // World-space wish direction from (strafe, forward), mirroring Entity.getInputVector's basis.
        double wishX = strafe * cos - forward * sin;
        double wishZ = forward * cos + strafe * sin;
        Vec3 into = dir.getUnitVec3();
        return wishX * into.x + wishZ * into.z > 0.0; // you're pushing toward the face, not away/along it
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
        } else if (isWallClimbing(player)) {
            SurvivalMeters.exert(player, (float) (ady * WALL_DRAIN_PER_BLOCK)); // rock burns up or down (any side)
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
        // You free-climb with your hands — you can't get a grip holding something. The selected slot must be
        // empty (put your tool away to climb). Vanilla ladders/vines are untouched; this is only our climbs.
        if (!player.getMainHandItem().isEmpty()) {
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
        // ── Bare-wall free-climbing (§5.4). The strict gates below govern GRABBING ON — a real ≥2-tall face,
        //    looking straight at it, pressed into it, and a deliberate jump. But once you've caught the wall
        //    you STAY caught as long as a block face is beside you on ANY side: so you can climb sideways,
        //    round a corner, and look around freely. The start-gates don't keep re-applying mid-climb. ──
        Direction dir = player.getDirection();

        // CONTINUE — already latched on. Hold the grip while a climbable face is beside you on any horizontal
        // side; let go only when you've climbed clear of every wall (the universal gates above already drop
        // you for empty stamina, a filled hand, etc.).
        if (isGripping(player)) {
            if (!nearAnyClimbableWall(player)) {
                setGripping(player, false); // no face on any side — you've topped out or stepped off
                return false;
            }
            gripMap(player).put(player, player.tickCount); // keep the top-out grace fresh while climbing
            return true;
        }

        // START — every gate must hold to INITIATE a climb from standing:
        //   (1) a genuine ≥2-tall rough face in front of you (a lone 1-block step is just a jump);
        //   (5) looking roughly straight at the rock (kills the old "climb while looking 90° away" looseness);
        //   (3) pressed against the face, driving into it.
        if (!hasClimbableWall(player) || !facingWall(player, dir) || !pressingIntoWall(player, dir)) {
            setGripping(player, false);
            return false;
        }
        // (4) A climb is INITIATED by a deliberate JUMP, engaged once the jump's own upward momentum is spent
        //     (at/after the peak), so a normal jump first carries you a block, then the slower climb takes over.
        boolean engage = jumpedRecently(player)
            && player.getDeltaMovement().y <= CLIMB_ENGAGE_VY
            && !jumpLatched(player);
        if (engage) {
            setGripping(player, true);
            gripMap(player).put(player, player.tickCount);
            return true;
        }
        return false;
    }

    /** A climbable flat face beside you on ANY of the four horizontal sides, at foot or head height — what
     *  keeps a climb going once you've caught on, so you can move sideways, round a corner, and look about
     *  without dropping the grip. (Grabbing ON is stricter; see {@link #canClimb}.) */
    private static boolean nearAnyClimbableWall(Player player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (isFlatWall(level, feet.relative(d)) || isFlatWall(level, feet.above().relative(d))) {
                return true;
            }
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
        // Facing a ≥2-tall face, OR mid-climb with a face on any side (so a sideways/traversing climb still
        // runs at the slow bare-rock pace, not vanilla ladder speed).
        return hasClimbableWall(player) || (isGripping(player) && nearAnyClimbableWall(player));
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
        // Finishing a climb: the top block of the wall still counts (foot hold, clear above) so you can climb
        // it to crest. But we do NOT extend the climb ABOVE the wall's top — that "block below the lip" path
        // kept you gripping while standing over the edge, which read as floating and let the crest launch you.
        // Once your feet clear the top block there's no face here, so the climb ends and you walk onto the ledge.
        return foot && !head;
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
