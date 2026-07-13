package dev.alone.core;

import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Village defence (proposal §7.2). A settlement is not a free pantry: it keeps armed men. In this redesign the
 * {@link VillageGuard guards} <b>live in the village permanently</b> — every village that a player draws near
 * is topped up to a small standing garrison (see {@link #GARRISON_SIZE}), each guard posted to the village and
 * marked persistent. In peacetime they patrol and wander it. Its {@link Golems iron golems} stand alongside
 * them, ignoring the guards entirely (a guard is a neutral pathfinder, not an {@code Enemy} — see
 * {@link VillageGuard}).
 *
 * <p>Wrong the village and both turn on <b>you</b>. Commit a <b>crime against a village</b> — anywhere inside
 * its bounds (detected via {@link Spawns#nearVillage}) — and it goes <b>hostile against you personally</b> for
 * a while, then cools off. While it's hostile, the guards <b>and</b> the golems hunt you (both via
 * {@link FlaggedPlayerTargetGoal}); the golems attack the flagged player but never the guards.
 *
 * <p><b>What counts as a crime</b> (any one flags the village):
 * <ul>
 *   <li><b>Looting</b> — opening a village-owned container (a chest/barrel/any container within village
 *       bounds). Taking a settlement's stores is theft; even cracking the lid counts.</li>
 *   <li><b>Assault</b> — hitting a villager, a village animal (a farm animal near the village), one of the
 *       settlement's <b>guards</b>, or one of its <b>iron golems</b>. Killing is just the hardest case of
 *       hitting, so the attack hook covers it — and striking any of the village's defenders picks a fight with
 *       the whole village, not just the one you hit.</li>
 *   <li><b>Rustling</b> — leading a village animal away on a lead.</li>
 *   <li><b>Vandalism</b> — breaking their hay bales or crops, or a villager's <b>workstation</b> (its job-site
 *       block — the composter, lectern, loom, smithing table…) or a bookshelf, within village bounds. A job
 *       site is a villager's livelihood; smashing it is a crime against the settlement.</li>
 * </ul>
 *
 * <p><b>The hostile state is a per-player, decaying timer</b> ({@link #VILLAGE_HOSTILE_UNTIL}, a game-time
 * tick): each fresh crime refreshes it to {@code now + }{@link #HOSTILE_DURATION}, and once the clock passes
 * it the village forgets and stands its guards down. So a raid is a consequence you can outlast or flee, not
 * a permanent black mark — and the garrison never leaves regardless (it just goes back to patrolling).
 */
public final class VillageDefense {
    private VillageDefense() {
    }

    /** Game-time tick until which the village stays hostile to this player. Absent/past ⇒ at peace. */
    public static final AttachmentType<Long> VILLAGE_HOSTILE_UNTIL =
        AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("alone", "village_hostile_until"), Codec.LONG);

    /** How long a single crime keeps the village hostile — ~5 minutes; refreshed on each fresh crime. */
    private static final long HOSTILE_DURATION = 6000L;
    /** How far a guard/golem will look for the flagged player it's hunting. */
    static final double SEARCH_RANGE = 64.0; // guards notice a flagged wrongdoer village-wide, out to the fields

    /** How often (ticks) the garrison top-up scan runs — cheap, so a low cadence is plenty. */
    private static final int GARRISON_SCAN = 100;
    /** How many guards a village keeps posted. */
    private static final int GARRISON_SIZE = 3;
    /** The guards' home radius around the village centre — their patrol leash and the count area for top-up. */
    private static final int GARRISON_RADIUS = 40;
    /** How far from the village centre a freshly-posted guard is placed. */
    private static final int POST_SPREAD = 12;

    public static void init() {
        // PERMANENT GARRISON — keep every village a player is near topped up to a standing guard force. Keyed
        // off nearby players (a village only matters, and is only loaded, when someone's about); the count
        // check keeps it from ever over-spawning. Guards are posted to the village and marked persistent, so
        // they stay and patrol it rather than despawning or wandering off.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)
                    || level.getGameTime() % GARRISON_SCAN != 0L) {
                    continue;
                }
                BlockPos centre = Spawns.villageCenter(level, player.blockPosition());
                if (centre != null) {
                    garrison(level, centre);
                }
                // DE-ESCALATION — a wanted player who has broken clear of the village AND shaken every guard
                // has gotten away: the settlement loses the trail and stands down, instead of staying hostile
                // for the whole timer no matter how far you run. Lingering near the village or within a guard's
                // reach keeps the heat on; only genuinely escaping (out of the village bounds, no guard within
                // their own give-up range) drops it. The timer is still the fallback if you hole up nearby.
                if (centre == null && isHostile(player)
                    && !guardWithin(level, player.blockPosition(), SEARCH_RANGE)) {
                    player.removeAttached(VILLAGE_HOSTILE_UNTIL);
                }
            }
        });

        // ASSAULT — hitting a villager or a village animal flags the village (killing is the hardest hit, so
        // it's covered here too). We never cancel the strike; the blow lands, it just has consequences.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender
                && isVillageProperty(entity) && crimeNearVillage(server, offender, entity)) {
                flag(offender, server);
            }
            return InteractionResult.PASS;
        });

        // RUSTLING — leading a village animal away on a lead is theft of livestock.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender
                && player.getItemInHand(hand).is(Items.LEAD)
                && entity instanceof Mob mob && Domestic.isDomestic(mob)
                && crimeNearVillage(server, offender, entity)) {
                flag(offender, server);
            }
            return InteractionResult.PASS;
        });

        // LOOTING — opening a container that sits within village bounds. Even cracking the lid is theft.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender) {
                BlockPos pos = hit.getBlockPos();
                if (server.getBlockEntity(pos) instanceof Container && Spawns.nearVillage(server, pos)) {
                    flag(offender, server);
                }
            }
            return InteractionResult.PASS; // don't consume the interaction — the container still opens
        });

        // VANDALISM — smashing the village's hay bales, crops, or a villager's workstation. Fires before the
        // break so we read the block.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender
                && isVillageBlock(state) && Spawns.nearVillage(server, pos)) {
                flag(offender, server);
            }
            return true; // never block the break itself — it's a crime, not an impossibility
        });
    }

    /** Top the village's garrison back up to {@link #GARRISON_SIZE}: count the guards already posted within
     *  the home radius and spawn any shortfall on the local surface, each posted to the centre and persistent. */
    private static void garrison(ServerLevel level, BlockPos centre) {
        AABB area = new AABB(centre).inflate(GARRISON_RADIUS);
        int missing = GARRISON_SIZE - level.getEntitiesOfClass(VillageGuard.class, area).size();
        if (missing <= 0) {
            return;
        }
        RandomSource rng = level.getRandom();
        for (int i = 0; i < missing; i++) {
            int x = centre.getX() + rng.nextInt(POST_SPREAD * 2 + 1) - POST_SPREAD;
            int z = centre.getZ() + rng.nextInt(POST_SPREAD * 2 + 1) - POST_SPREAD;
            // Drop the guard onto the local surface so it never spawns buried or floating.
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            VillageGuard guard = new VillageGuard(AloneEntities.GUARD, level);
            guard.snapTo(x + 0.5, y, z + 0.5, rng.nextFloat() * 360f, 0f);
            guard.setHomeTo(centre, GARRISON_RADIUS); // patrol leash — it stays in its village
            guard.setPersistenceRequired();           // a posted guard doesn't despawn
            level.addFreshEntity(guard);
        }
    }

    /** Is any village guard within {@code range} of this position? Used by de-escalation: a wanted player is
     *  only considered to have escaped once no guard is still on them (and they're clear of the village). */
    private static boolean guardWithin(ServerLevel level, BlockPos pos, double range) {
        return !level.getEntitiesOfClass(VillageGuard.class, new AABB(pos).inflate(range)).isEmpty();
    }

    /** A villager, one of the village's own guards, its iron golem, or a FARMED (domestic) animal — the living
     *  property a settlement defends. Wild game (a boar, a deer that wandered near) is fair to hunt and never
     *  counts, even right by the village. A guard or golem counts because striking a village's defenders is an
     *  attack on the village itself: hit one and the WHOLE settlement turns on you — every guard and golem, not
     *  just the one you struck (which alone would only retaliate via its own hurt-by goal). */
    private static boolean isVillageProperty(Entity entity) {
        return entity instanceof AbstractVillager
            || entity instanceof VillageGuard
            || entity instanceof IronGolem
            || (entity instanceof Mob mob && Domestic.isDomestic(mob));
    }

    /** A crime counts if the target OR the offender is near the village — so hitting a cow that has drifted to
     *  the OUTSKIRTS still rouses the garrison, and standing at the edge doesn't put you out of its reach. */
    private static boolean crimeNearVillage(ServerLevel level, Player offender, Entity target) {
        return Spawns.nearVillage(level, target.blockPosition())
            || Spawns.nearVillage(level, offender.blockPosition());
    }

    /** The villager job-site workstations — every block that assigns a profession — plus the bookshelf (the
     *  library's stock). Breaking one is destroying a villager's livelihood, so it counts as vandalism. */
    private static final Set<Block> VILLAGE_WORKSTATIONS = Set.of(
        Blocks.COMPOSTER,        // farmer
        Blocks.BARREL,           // fisherman
        Blocks.BLAST_FURNACE,    // armorer
        Blocks.SMOKER,           // butcher
        Blocks.CARTOGRAPHY_TABLE,// cartographer
        Blocks.BREWING_STAND,    // cleric
        Blocks.CAULDRON,         // leatherworker
        Blocks.FLETCHING_TABLE,  // fletcher
        Blocks.GRINDSTONE,       // weaponsmith
        Blocks.LECTERN,          // librarian
        Blocks.LOOM,             // shepherd
        Blocks.SMITHING_TABLE,   // toolsmith
        Blocks.STONECUTTER,      // mason
        Blocks.BOOKSHELF);       // the library's stock

    /** Blocks a settlement will defend: its harvest (hay, crops) and its people's workstations/bookshelves. */
    private static boolean isVillageBlock(BlockState state) {
        return state.is(Blocks.HAY_BLOCK)
            || state.is(BlockTags.CROPS)
            || VILLAGE_WORKSTATIONS.contains(state.getBlock());
    }

    /** Is the village currently hostile to this player (timer set and not yet lapsed)? */
    public static boolean isHostile(Player player) {
        Long until = player.getAttached(VILLAGE_HOSTILE_UNTIL);
        return until != null && player.level().getGameTime() < until;
    }

    /**
     * Register a crime: turn (or keep) the village hostile to this player. The timer refresh is what makes the
     * state decay — stop offending and it simply runs out, and the standing garrison (which never left) goes
     * back to patrolling. No wave is spawned here: the guards are already posted; they just switch targets to
     * you via {@link FlaggedPlayerTargetGoal}, as do the iron golems.
     */
    private static void flag(ServerPlayer player, ServerLevel level) {
        // No chat warning — the village turning on you is felt in the action (the guards and golems closing in),
        // not announced. Just set/refresh the hostile timer.
        player.setAttached(VILLAGE_HOSTILE_UNTIL, level.getGameTime() + HOSTILE_DURATION);
    }

    /**
     * Sets a defending mob onto the nearest player the village has been turned hostile against. Shared by the
     * {@link VillageGuard guards} and the {@link Golems iron golems} so one wrong turns the whole settlement's
     * defenders on you. Drops the target the instant the hostile timer lapses (see {@link #isHostile}), so a
     * defender stands down when the village forgets — it doesn't hound you forever.
     */
    public static class FlaggedPlayerTargetGoal extends Goal {
        private final Mob mob;
        private Player found;

        public FlaggedPlayerTargetGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player p = nearestFlagged();
            if (p == null) {
                return false;
            }
            this.found = p;
            return true;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.found);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target instanceof Player p && p.isAlive() && isHostile(p)
                && this.mob.distanceToSqr(p) <= SEARCH_RANGE * SEARCH_RANGE;
        }

        @Override
        public void stop() {
            this.mob.setTarget(null);
            this.found = null;
        }

        private Player nearestFlagged() {
            Level level = this.mob.level();
            Vec3 at = this.mob.position();
            Player best = null;
            double bestSq = SEARCH_RANGE * SEARCH_RANGE;
            for (Player p : level.players()) {
                if (!p.isAlive() || p.isCreative() || p.isSpectator() || !isHostile(p)) {
                    continue;
                }
                double d = p.distanceToSqr(at);
                if (d < bestSq) {
                    bestSq = d;
                    best = p;
                }
            }
            return best;
        }
    }
}
