package dev.alone.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Food security by scent (roadmap: Food, farming &amp; preservation). <b>Fresh meat you carry reeks</b>,
 * and the wilderness has a nose for it: a bold predator (a <b>wolf</b> or a <b>polar bear</b>) within
 * smelling range is <b>drawn toward you</b>, the further the more you're carrying. This is the other side
 * of preservation — <b>salted or dried food doesn't reek</b>, and food left in a chest/pack instead of on
 * your person isn't on you to smell — so the trade is real: <b>haul raw meat and attract danger, or
 * preserve/cache it and travel clean.</b> (Foxes stay skittish and keep their distance; they don't hunt you.)
 *
 * <p>And it escalates: a predator that reaches your side doesn't just watch — it <b>snatches a piece of raw
 * meat and bolts with it</b>, a hungry raid that costs you food (not blood). Preserve it, cache it, or lose
 * it to the wild.
 *
 * <p>The same nose finds <b>carrion</b>: fresh meat <b>dropped on the ground</b> — a kill left behind, or
 * the pile where you died — draws predators that <b>eat it where it lies</b>, so don't dawdle recovering a
 * meat-laden death pile. (See {@link #scavenge}.)
 *
 * <p>And it smells <b>blood</b>: while you're <b>bleeding</b> (an open wound, see {@link Conditions}) you
 * reek like a fresh kill, so a wound in predator country draws them to you — bind it fast, for more than
 * just the health.
 */
public final class Scent {
    private Scent() {
    }

    /** Bold predators that will track a meat-carrier by smell (and, in {@link Wildlife}, aren't skittish game). */
    public static final Set<EntityType<?>> PREDATORS =
        Set.of(EntityTypes.WOLF, EntityTypes.POLAR_BEAR, AloneEntities.BROWN_BEAR);

    private static final int SCAN = 40;              // sniff the air ~every 2s
    private static final double BASE_RADIUS = 14.0;  // a little carried meat carries this far
    private static final double MAX_RADIUS = 30.0;   // a heavy load, this far
    private static final double BLOOD_BONUS = 8.0;   // an open, bleeding wound carries about this far on its own
    private static final double APPROACH_SPEED = 1.15;
    private static final double GRAB_RANGE = 2.6;        // this close, a predator can snatch a piece
    private static final long RAID_COOLDOWN_TICKS = 160L; // ~8s between snatches so a pack can't strip you at once
    private static final double FLEE_SPEED = 1.45;       // it runs off with its prize
    private static final double WIND_STRENGTH = 0.6;     // scent on the wind: downwind ~1.6x reach, upwind ~0.4x

    private static final TagKey<Item> PERISHABLE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    /** Per-player "next raid allowed" world-time — transient (a session cooldown), keyed by player id. */
    private static final Map<UUID, Long> RAID_COOLDOWN = new HashMap<>();
    /** Per-player last in-game day we noted the wind, so the message fires only when the wind SHIFTS (once a
     *  day at most, while carrying meat) instead of on a nagging timer. A HUD wind indicator will replace it. */
    private static final Map<UUID, Long> WIND_NOTE_DAY = new HashMap<>();
    /** Per-predator world-time until which it won't be drawn to your scent again — set once it's raided
     *  successfully or been driven off (hurt). Stops the same animals pestering you in an endless stream. */
    private static final Map<UUID, Long> PREDATOR_GIVEUP = new HashMap<>();
    private static final long GIVEUP_TICKS = 3000L; // ~3 in-game hours of leaving you alone after a raid/rebuff

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % SCAN != 0 || player.isCreative() || player.isSpectator()) {
                    continue;
                }
                if (player.level() instanceof ServerLevel level) {
                    tick(level, player);
                    scavenge(level, player);
                }
            }
        });
    }

    private static void tick(ServerLevel level, ServerPlayer player) {
        int freshMeat = countFreshMeat(player);
        boolean bleeding = Conditions.isBleeding(player); // an open wound reeks of blood, same as raw meat
        if (freshMeat <= 0 && !bleeding) {
            return;
        }
        double radius = Math.min(MAX_RADIUS,
            BASE_RADIUS + freshMeat * 2.0 + (bleeding ? BLOOD_BONUS : 0.0)); // meat and fresh blood both carry
        // Scent rides the wind (§7.3): a predator downwind smells you from far off, one upwind barely at all.
        Vec3 wind = Wind.direction(level);
        float windStrength = Wind.strength(level);
        AABB box = player.getBoundingBox().inflate(radius * (1.0 + WIND_STRENGTH * windStrength)); // as far as downwind allows
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box,
            m -> PREDATORS.contains(m.getType()) && m.isAlive() && smellsAcross(player, m, radius, wind, windStrength));
        // A hunter reads the wind — while you carry meat, you feel where it's blowing from and can keep
        // predators upwind of you. Only spoken when the wind SHIFTS (once a day at most); a HUD indicator
        // will show it at a glance, and then this note goes away.
        long windDay = level.getGameTime() / 24000L;
        if (freshMeat > 0 && WIND_NOTE_DAY.getOrDefault(player.getUUID(), -1L) != windDay) {
            WIND_NOTE_DAY.put(player.getUUID(), windDay);
            player.sendSystemMessage(Component.literal(
                "The wind has shifted — it's out of the " + Wind.comingFrom(level) + " now. Keep predators upwind."), true);
        }

        long now = level.getGameTime();
        PathfinderMob raider = null;
        boolean drewOne = false;
        for (Mob mob : nearby) {
            if (!(mob instanceof PathfinderMob predator)) {
                continue;
            }
            // Hurting a raider drives it off (a hungry animal isn't suicidal); a completed raid satisfies it
            // (below). Either way it then gives up on you for a good while, instead of an endless stream.
            if (predator.getLastHurtByMob() == player) {
                PREDATOR_GIVEUP.put(predator.getUUID(), now + GIVEUP_TICKS);
            }
            if (now < PREDATOR_GIVEUP.getOrDefault(predator.getUUID(), 0L)) {
                continue; // had enough of you for now — left to its own devices
            }
            if (predator.distanceToSqr(player) <= GRAB_RANGE * GRAB_RANGE) {
                raider = predator; // right on top of you — close enough to snatch
            } else {
                // Drawn in by the smell — and it comes for YOU. A hungry predator is a real threat you have
                // to fend off or flee, not a passive walk-up you can club for free.
                predator.setTarget(player);
                drewOne = true;
            }
        }

        // A hungry raid: a predator at your side grabs a piece of raw meat and bolts with it, then leaves
        // you be for a while (it got what it came for).
        if (raider != null && now >= RAID_COOLDOWN.getOrDefault(player.getUUID(), 0L)) {
            ItemStack stolen = takeOneFreshMeat(player);
            if (!stolen.isEmpty()) {
                RAID_COOLDOWN.put(player.getUUID(), now + RAID_COOLDOWN_TICKS);
                PREDATOR_GIVEUP.put(raider.getUUID(), now + GIVEUP_TICKS);
                fleeWithPrize(raider, player);
                player.sendSystemMessage(Component.literal(
                    "A predator snatches raw meat from your pack and bolts with it!"));
                return;
            }
        }
        if (drewOne && player.tickCount % (SCAN * 8) == 0) {
            player.sendSystemMessage(Component.literal(freshMeat > 0
                ? "The raw meat you're carrying has caught something's nose…"
                : "Something has caught the scent of your blood…"), true); // bleeding, no meat to blame
        }
    }

    /** Pull one piece of fresh (unpreserved) perishable food off the player — what the raider makes off with. */
    private static ItemStack takeOneFreshMeat(ServerPlayer player) {
        DataComponentType<Boolean> preserved = booleanComponent("preserved");
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(PERISHABLE)) {
                continue;
            }
            if (preserved != null && stack.getOrDefault(preserved, false)) {
                continue; // sealed — no smell, and the raider can't find it
            }
            ItemStack one = stack.copyWithCount(1);
            stack.shrink(1);
            return one;
        }
        return ItemStack.EMPTY;
    }

    /** Send the raider sprinting away from the player, prize in its jaws. */
    private static void fleeWithPrize(PathfinderMob raider, ServerPlayer player) {
        raider.setTarget(null);
        Vec3 away = raider.position().subtract(player.position());
        if (away.lengthSqr() < 1.0e-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 dest = raider.position().add(away.normalize().scale(14.0));
        raider.getNavigation().moveTo(dest.x, dest.y, dest.z, FLEE_SPEED);
    }

    /** Fresh (unpreserved) perishable food carried loose on your body — a full stack reeks more than one piece. */
    /** Does your scent reach this predator, given the wind? Downwind (it lies where the wind blows) stretches
     *  your reach toward it; upwind shrinks it. {@code wind} is a horizontal unit vector. */
    private static boolean smellsAcross(ServerPlayer player, Mob mob, double radius, Vec3 wind, float windStrength) {
        Vec3 toMob = mob.position().subtract(player.position());
        double horizontal = Math.sqrt(toMob.x * toMob.x + toMob.z * toMob.z);
        double align = horizontal < 1.0e-3 ? 0.0 : (toMob.x * wind.x + toMob.z * wind.z) / horizontal;
        double effective = radius * (1.0 + WIND_STRENGTH * windStrength * align); // calm day → a plain circle
        return mob.distanceToSqr(player) <= effective * effective;
    }

    private static int countFreshMeat(ServerPlayer player) {
        DataComponentType<Boolean> preserved = booleanComponent("preserved");
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isFreshMeat(stack, preserved)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Raw, unpreserved meat/fish — the stuff that reeks (salted/dried is sealed). */
    private static boolean isFreshMeat(ItemStack stack, DataComponentType<Boolean> preserved) {
        if (stack.isEmpty() || !stack.is(PERISHABLE)) {
            return false;
        }
        return preserved == null || !stack.getOrDefault(preserved, false);
    }

    /**
     * The wilderness cleans up carrion. Fresh meat <b>dropped on the ground</b> (a kill left behind, or the
     * pile where you died) reeks the same as meat in hand — nearby predators are drawn to it and <b>eat it
     * where it lies</b>, a piece at a time. So a carcass or a death-pile of raw meat won't sit forever; the
     * scavengers find it, which is the counter to items never despawning. Preserved food doesn't draw them.
     */
    private static void scavenge(ServerLevel level, ServerPlayer player) {
        DataComponentType<Boolean> preserved = booleanComponent("preserved");
        AABB box = player.getBoundingBox().inflate(BASE_RADIUS);
        List<ItemEntity> piles = level.getEntitiesOfClass(ItemEntity.class, box,
            e -> e.isAlive() && isFreshMeat(e.getItem(), preserved));
        if (piles.isEmpty()) {
            return;
        }
        List<Mob> predators = level.getEntitiesOfClass(Mob.class, box,
            m -> PREDATORS.contains(m.getType()) && m.isAlive());
        for (Mob mob : predators) {
            if (!(mob instanceof PathfinderMob predator) || predator.getTarget() != null) {
                continue;
            }
            ItemEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (ItemEntity pile : piles) {
                double d = predator.distanceToSqr(pile);
                if (d < best) {
                    best = d;
                    nearest = pile;
                }
            }
            if (nearest == null) {
                continue;
            }
            if (best <= GRAB_RANGE * GRAB_RANGE) {
                ItemStack stack = nearest.getItem();
                stack.shrink(1); // gulps a piece each pass
                if (stack.isEmpty()) {
                    nearest.discard();
                } else {
                    nearest.setItem(stack);
                }
            } else {
                predator.getNavigation().moveTo(nearest, APPROACH_SPEED); // drawn to the carcass
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Boolean> booleanComponent(String path) {
        return (DataComponentType<Boolean>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(
            Identifier.fromNamespaceAndPath("alone", path));
    }
}
