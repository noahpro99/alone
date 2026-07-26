package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

/**
 * Paddling a boat is work (§ boats). A boat doesn't glide for free — moving it under paddle costs the
 * crew stamina, and a spent paddler can't keep it up, so the boat loses way and drifts to a stop until
 * they've caught their breath. <b>Two paddlers</b> share the effort (each tires half as fast) and put more
 * into the stroke, so a crewed canoe travels farther and a touch faster than one paddled solo — the
 * two-person payoff. Done as a server-tick pass over players riding boats (cheap — players are few), so
 * there's no fragile hook into the boat's own movement code.
 */
public final class Paddling {
    private Paddling() {
    }

    private static final float PADDLE_DRAIN = 0.06f;  // steady paddling tires you over minutes (tunable)
    private static final double MOVING = 0.03;         // horizontal speed above which you're actively paddling
    private static final double CREW_BOOST = 0.006;    // a second paddler's extra push per tick…
    private static final double BOOST_CAP = 0.42;      // …capped to a cruising speed so it never runs away
    private static final double SPENT_DRAG = 0.90;     // an exhausted paddler: the boat loses way each tick

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.getVehicle() instanceof AbstractBoat boat)) {
                    continue;
                }
                Vec3 v = boat.getDeltaMovement();
                double speed = Math.sqrt(v.x * v.x + v.z * v.z);
                if (speed < MOVING) {
                    continue; // drifting or moored — no paddling effort
                }
                // Everyone aboard is paddling, so it costs each of them stamina — but two share the load,
                // so each tires at half the rate (you can spell each other on a long crossing).
                int crew = countPlayerPassengers(boat);
                SurvivalMeters.exert(player, PADDLE_DRAIN / crew);

                // Only the pilot's pass touches the boat itself, so a two-crew boat isn't adjusted twice.
                if (boat.getControllingPassenger() != player) {
                    continue;
                }
                if (SurvivalMeters.getStamina(player) <= 0f) {
                    // Too spent to paddle — the boat loses way and coasts to a stop until you recover.
                    boat.setDeltaMovement(v.x * SPENT_DRAG, v.y, v.z * SPENT_DRAG);
                } else if (crew >= 2 && speed < BOOST_CAP) {
                    // Two paddlers put more into the stroke — a gentle push along the way you're already
                    // moving, capped so a crewed boat cruises a little faster without ever running away.
                    boat.setDeltaMovement(v.x + (v.x / speed) * CREW_BOOST, v.y, v.z + (v.z / speed) * CREW_BOOST);
                }
            }
        });
    }

    private static int countPlayerPassengers(AbstractBoat boat) {
        int n = 0;
        for (Entity e : boat.getPassengers()) {
            if (e instanceof ServerPlayer) {
                n++;
            }
        }
        return Math.max(1, n);
    }
}
