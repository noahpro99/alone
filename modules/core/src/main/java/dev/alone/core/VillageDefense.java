package dev.alone.core;

import java.util.EnumSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Village defence (proposal §7.2). A settlement is not a free pantry: wrong it and it turns on you. Commit a
 * <b>crime against a village</b> — anywhere inside its bounds (detected via {@link Spawns#nearVillage}) — and
 * the village goes <b>hostile against you personally</b> for a while, then cools off. While it's hostile it
 * <b>musters armed {@link VillageGuard guards}</b> (a bow-and-sword humanoid, see that class) who hunt you,
 * and its {@link Golems iron golems} turn on you too.
 *
 * <p><b>What counts as a crime</b> (any one flags the village):
 * <ul>
 *   <li><b>Looting</b> — opening a village-owned container (a chest/barrel/any container within village
 *       bounds). Taking a settlement's stores is theft; even cracking the lid counts.</li>
 *   <li><b>Assault</b> — hitting a villager or a village animal (a farm animal near the village). Killing is
 *       just the hardest case of hitting, so the attack hook covers it.</li>
 *   <li><b>Rustling</b> — leading a village animal away on a lead.</li>
 *   <li><b>Vandalism</b> — breaking their hay bales or crops within village bounds.</li>
 * </ul>
 *
 * <p><b>The hostile state is a per-player, decaying timer</b> ({@link #VILLAGE_HOSTILE_UNTIL}, a game-time
 * tick): each fresh crime refreshes it to {@code now + }{@link #HOSTILE_DURATION}, and once the clock passes
 * it the village forgets and stands its guards down. So a raid is a consequence you can outlast or flee, not
 * a permanent black mark. Guards are mustered in a wave the moment you first offend and reinforced if you
 * keep offending (throttled by {@link #REINFORCE_COOLDOWN}); they're marked persistent so they see it through.
 */
public final class VillageDefense {
    private VillageDefense() {
    }

    /** Game-time tick until which the village stays hostile to this player. Absent/past ⇒ at peace. */
    public static final AttachmentType<Long> VILLAGE_HOSTILE_UNTIL =
        AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("alone", "village_hostile_until"), Codec.LONG);

    /** Game-time tick of the last guard muster, so repeated crimes don't spawn an endless horde. */
    public static final AttachmentType<Long> GUARDS_MUSTERED_AT =
        AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("alone", "guards_mustered_at"), Codec.LONG);

    /** How long a single crime keeps the village hostile — ~5 minutes; refreshed on each fresh crime. */
    private static final long HOSTILE_DURATION = 6000L;
    /** Minimum gap between guard waves while you keep offending — ~1 minute. */
    private static final long REINFORCE_COOLDOWN = 1200L;
    /** How many guards muster per wave. */
    private static final int WAVE_SIZE = 3;
    /** Ring the wave spawns in around you (min..max blocks) — close enough to close in, not on your head. */
    private static final int SPAWN_MIN = 8;
    private static final int SPAWN_MAX = 16;
    /** How far a guard/golem will look for the flagged player it's hunting. */
    static final double SEARCH_RANGE = 48.0;

    public static void init() {
        // ASSAULT — hitting a villager or a village animal flags the village (killing is the hardest hit, so
        // it's covered here too). We never cancel the strike; the blow lands, it just has consequences.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender
                && isVillageProperty(entity) && Spawns.nearVillage(server, entity.blockPosition())) {
                flag(offender, server);
            }
            return InteractionResult.PASS;
        });

        // RUSTLING — leading a village animal away on a lead is theft of livestock.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender
                && player.getItemInHand(hand).is(Items.LEAD)
                && entity instanceof Animal && Spawns.nearVillage(server, entity.blockPosition())) {
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

        // VANDALISM — smashing the village's hay bales or crops. Fires before the break so we read the block.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel server && player instanceof ServerPlayer offender
                && isVillageCrop(state) && Spawns.nearVillage(server, pos)) {
                flag(offender, server);
            }
            return true; // never block the break itself — it's a crime, not an impossibility
        });
    }

    /** A villager or a farmed animal — the living property a settlement will defend. */
    private static boolean isVillageProperty(Entity entity) {
        return entity instanceof AbstractVillager || entity instanceof Animal;
    }

    /** Hay bales and crops — the harvest a settlement will defend. */
    private static boolean isVillageCrop(BlockState state) {
        return state.is(Blocks.HAY_BLOCK) || state.is(BlockTags.CROPS);
    }

    /** Is the village currently hostile to this player (timer set and not yet lapsed)? */
    public static boolean isHostile(Player player) {
        Long until = player.getAttached(VILLAGE_HOSTILE_UNTIL);
        return until != null && player.level().getGameTime() < until;
    }

    /**
     * Register a crime: turn (or keep) the village hostile to this player and, on the first offence or after
     * the reinforcement cooldown, muster a wave of guards. The timer refresh is what makes the state decay —
     * stop offending and it simply runs out.
     */
    private static void flag(ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        boolean wasHostile = isHostile(player);
        player.setAttached(VILLAGE_HOSTILE_UNTIL, now + HOSTILE_DURATION);

        long lastMuster = player.getAttachedOrElse(GUARDS_MUSTERED_AT, 0L);
        if (!wasHostile || now - lastMuster > REINFORCE_COOLDOWN) {
            musterGuards(level, player);
            player.setAttached(GUARDS_MUSTERED_AT, now);
        }
        if (!wasHostile) {
            player.sendSystemMessage(Component.literal("The village turns against you — its guards are coming.")
                .withStyle(ChatFormatting.RED));
        }
    }

    /** Spawn a wave of guards in a ring around the offender, each already hunting them and marked persistent. */
    private static void musterGuards(ServerLevel level, ServerPlayer player) {
        var rng = player.getRandom();
        BlockPos centre = player.blockPosition();
        for (int i = 0; i < WAVE_SIZE; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double radius = SPAWN_MIN + rng.nextDouble() * (SPAWN_MAX - SPAWN_MIN);
            int x = centre.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = centre.getZ() + (int) Math.round(Math.sin(angle) * radius);
            // Drop the guard onto the local surface so it never spawns buried or floating.
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            VillageGuard guard = new VillageGuard(AloneEntities.GUARD, level);
            guard.snapTo(x + 0.5, y, z + 0.5, rng.nextFloat() * 360f, 0f);
            guard.setPersistenceRequired(); // a mustered guard sees the reprisal through — no despawn
            guard.setTarget(player);
            level.addFreshEntity(guard);
        }
    }

    /**
     * Sets a defending mob onto the nearest player the village has been turned hostile against. Shared by the
     * {@link VillageGuard guards} and the {@link Golems iron golems} so one wrong turns the whole settlement's
     * defenders on you. Drops the target the instant the hostile timer lapses (see {@link #isHostile}), so a
     * guard stands down when the village forgets — it doesn't hound you forever.
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
