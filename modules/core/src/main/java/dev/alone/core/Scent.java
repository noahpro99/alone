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
    public static final Set<EntityType<?>> PREDATORS = Set.of(EntityTypes.WOLF, EntityTypes.POLAR_BEAR);

    private static final int SCAN = 40;              // sniff the air ~every 2s
    private static final double BASE_RADIUS = 14.0;  // a little carried meat carries this far
    private static final double MAX_RADIUS = 30.0;   // a heavy load, this far
    private static final double BLOOD_BONUS = 8.0;   // an open, bleeding wound carries about this far on its own
    private static final double APPROACH_SPEED = 1.15;
    private static final double GRAB_RANGE = 2.6;        // this close, a predator can snatch a piece
    private static final long RAID_COOLDOWN_TICKS = 160L; // ~8s between snatches so a pack can't strip you at once
    private static final double FLEE_SPEED = 1.45;       // it runs off with its prize

    private static final TagKey<Item> PERISHABLE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    /** Per-player "next raid allowed" world-time — transient (a session cooldown), keyed by player id. */
    private static final Map<UUID, Long> RAID_COOLDOWN = new HashMap<>();

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
        double radiusSq = radius * radius;
        AABB box = player.getBoundingBox().inflate(radius);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box,
            m -> PREDATORS.contains(m.getType()) && m.isAlive() && m.distanceToSqr(player) <= radiusSq);

        PathfinderMob raider = null;
        boolean drewOne = false;
        for (Mob mob : nearby) {
            if (!(mob instanceof PathfinderMob predator)) {
                continue;
            }
            if (predator.distanceToSqr(player) <= GRAB_RANGE * GRAB_RANGE) {
                raider = predator; // right on top of you — close enough to snatch
            } else if (predator.getTarget() == null) {
                predator.getNavigation().moveTo(player, APPROACH_SPEED); // drawn in by the smell
                drewOne = true;
            }
        }

        // A hungry raid: a predator at your side grabs a piece of raw meat and bolts with it.
        long now = level.getGameTime();
        if (raider != null && now >= RAID_COOLDOWN.getOrDefault(player.getUUID(), 0L)) {
            ItemStack stolen = takeOneFreshMeat(player);
            if (!stolen.isEmpty()) {
                RAID_COOLDOWN.put(player.getUUID(), now + RAID_COOLDOWN_TICKS);
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
