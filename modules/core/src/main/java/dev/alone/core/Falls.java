package dev.alone.core;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Water only breaks a fall if it's <b>deep enough</b> (proposal §1.5 / realism). Vanilla negates any
 * fall the moment you touch water — even one block deep — which is nonsense: shallow water just means
 * you hit the bottom. We track the height you actually fell through air and, when you splash down,
 * check the water column: each block of depth absorbs a few metres of fall; whatever's left over is
 * applied through the normal injury system ({@link Conditions#applyFall}). Cliff-diving into a deep
 * pool is safe; a belly-flop into a puddle from the cliff top is not.
 */
public final class Falls {
    private Falls() {
    }

    private static final double SAFE_FALL = 3.5;      // matches the injury system's safe height
    private static final double WATER_ABSORB = 4.0;   // metres of fall each block of water depth cancels
    private static final int MAX_DEPTH = 32;

    private static final Map<Player, Double> AIR_FALL = new WeakHashMap<>();
    private static final Map<Player, Double> LAST_Y = new WeakHashMap<>();
    private static final Map<Player, Boolean> WAS_IN_WATER = new WeakHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
    }

    private static void tick(ServerPlayer player) {
        boolean inWater = player.isInWater();
        boolean wasInWater = WAS_IN_WATER.getOrDefault(player, false);
        double y = player.getY();
        Double prevY = LAST_Y.put(player, y);

        if (player.isCreative() || player.isSpectator() || player.getAbilities().flying) {
            AIR_FALL.put(player, 0.0);
            WAS_IN_WATER.put(player, inWater);
            return;
        }

        double fall = AIR_FALL.getOrDefault(player, 0.0);
        boolean caught = player.onGround() || player.onClimbable();
        if (prevY != null && !inWater && !caught) {
            double dy = y - prevY;
            if (dy < 0) {
                fall += -dy; // accumulate the distance fallen through open air
            }
        }

        // Splashed into water after a fall — did the pool actually break it?
        if (inWater && !wasInWater && fall > SAFE_FALL) {
            double effective = fall - waterDepth(player) * WATER_ABSORB;
            if (effective > SAFE_FALL) {
                Conditions.applyFall(player, effective, 1.0f, player.damageSources().fall());
            }
        }

        if (inWater || caught) {
            fall = 0.0; // landed or caught hold — start the next fall fresh
        }
        AIR_FALL.put(player, fall);
        WAS_IN_WATER.put(player, inWater);
    }

    /** How many blocks of water are stacked below the entry point. */
    private static int waterDepth(ServerPlayer player) {
        Level level = player.level();
        BlockPos.MutableBlockPos cursor = player.blockPosition().mutable();
        int depth = 0;
        while (depth < MAX_DEPTH && level.getFluidState(cursor).is(FluidTags.WATER)) {
            depth++;
            cursor.move(Direction.DOWN);
        }
        return depth;
    }
}
