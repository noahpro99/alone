package dev.alone.core;

import java.util.List;
import java.util.Set;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Food security by scent (roadmap: Food, farming &amp; preservation). <b>Fresh meat you carry reeks</b>,
 * and the wilderness has a nose for it: a bold predator (a <b>wolf</b> or a <b>polar bear</b>) within
 * smelling range is <b>drawn toward you</b>, the further the more you're carrying. This is the other side
 * of preservation — <b>salted or dried food doesn't reek</b>, and food left in a chest/pack instead of on
 * your person isn't on you to smell — so the trade is real: <b>haul raw meat and attract danger, or
 * preserve/cache it and travel clean.</b> (Foxes stay skittish and keep their distance; they don't hunt you.)
 *
 * <p>Scoped for v1: the smell <b>draws predators in to investigate</b>; it doesn't yet make them attack
 * outright for the food (that escalation — a hungry raid — is future). Their approach is the warning.
 */
public final class Scent {
    private Scent() {
    }

    /** Bold predators that will track a meat-carrier by smell (and, in {@link Wildlife}, aren't skittish game). */
    public static final Set<EntityType<?>> PREDATORS = Set.of(EntityTypes.WOLF, EntityTypes.POLAR_BEAR);

    private static final int SCAN = 40;              // sniff the air ~every 2s
    private static final double BASE_RADIUS = 14.0;  // a little carried meat carries this far
    private static final double MAX_RADIUS = 30.0;   // a heavy load, this far
    private static final double APPROACH_SPEED = 1.15;

    private static final TagKey<Item> PERISHABLE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % SCAN != 0 || player.isCreative() || player.isSpectator()) {
                    continue;
                }
                if (player.level() instanceof ServerLevel level) {
                    tick(level, player);
                }
            }
        });
    }

    private static void tick(ServerLevel level, ServerPlayer player) {
        int freshMeat = countFreshMeat(player);
        if (freshMeat <= 0) {
            return;
        }
        double radius = Math.min(MAX_RADIUS, BASE_RADIUS + freshMeat * 2.0); // more meat, stronger scent
        double radiusSq = radius * radius;
        AABB box = player.getBoundingBox().inflate(radius);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box,
            m -> PREDATORS.contains(m.getType()) && m.isAlive() && m.distanceToSqr(player) <= radiusSq);

        boolean drewOne = false;
        for (Mob mob : nearby) {
            // Only nudge one that isn't already busy with a target of its own — let the smell lead it in.
            if (mob instanceof PathfinderMob predator && predator.getTarget() == null) {
                predator.getNavigation().moveTo(player, APPROACH_SPEED);
                drewOne = true;
            }
        }
        if (drewOne && player.tickCount % (SCAN * 8) == 0) {
            player.sendSystemMessage(
                Component.literal("The raw meat you're carrying has caught something's nose…"), true);
        }
    }

    /** Fresh (unpreserved) perishable food carried loose on your body — a full stack reeks more than one piece. */
    private static int countFreshMeat(ServerPlayer player) {
        DataComponentType<Boolean> preserved = booleanComponent("preserved");
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(PERISHABLE)) {
                continue;
            }
            if (preserved != null && stack.getOrDefault(preserved, false)) {
                continue; // salted/dried — sealed against the smell
            }
            count += stack.getCount();
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<Boolean> booleanComponent(String path) {
        return (DataComponentType<Boolean>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(
            Identifier.fromNamespaceAndPath("alone", path));
    }
}
