package dev.alone.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.alone.core.net.FireDrillPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Friction fire (proposal §3.1) — no easy flint-and-steel. Sneak and hold right-click with a stick
 * on solid, dry ground to drill: it's slow, drains stamina, and <b>fails in the rain or when
 * you're wet</b>, so keeping tinder dry matters. Each held right-click is one "stroke" (the client
 * mixin sends them); enough strokes on the same spot lights a fire on top.
 */
public final class FireStarting {
    private FireStarting() {
    }

    private static final int STROKES_TO_LIGHT = 15; // ~3 seconds of drilling
    private static final float STAMINA_PER_STROKE = 1.5f;

    private record Drill(BlockPos pos, int strokes, int atTick) {
    }

    private static final Map<UUID, Drill> ACTIVE = new HashMap<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(FireDrillPayload.TYPE,
            (payload, context) -> stroke(context.player()));
    }

    /** The solid block you'd drill on (fire goes on top), or null if you're not aimed at a valid spot. */
    public static BlockPos findDrillSpot(Player player, Level level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot())
            .scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            return null;
        }
        BlockPos base = hit.getBlockPos();
        if (!level.getBlockState(base.above()).isAir()) {
            return null;
        }
        if (!level.getBlockState(base).isFaceSturdy(level, base, Direction.UP)) {
            return null;
        }
        return base;
    }

    private static void stroke(ServerPlayer player) {
        Level level = player.level();
        if (!player.getMainHandItem().is(Items.STICK) || !player.isShiftKeyDown()) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        BlockPos base = findDrillSpot(player, level);
        if (base == null) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos fire = base.above();

        // Wet tinder never catches.
        if (level.isRainingAt(fire) || player.isInWaterOrRain()) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5,
                3, 0.1, 0.02, 0.1, 0.01);
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (SurvivalMeters.getStamina(player) <= 0f) {
            return; // too spent to keep drilling
        }

        Drill current = ACTIVE.get(player.getUUID());
        boolean continuing = current != null && current.pos.equals(base) && player.tickCount - current.atTick <= 20;
        int strokes = continuing ? current.strokes + 1 : 1;

        SurvivalMeters.exert(player, STAMINA_PER_STROKE);
        serverLevel.sendParticles(ParticleTypes.SMOKE, fire.getX() + 0.5, fire.getY() + 0.05, fire.getZ() + 0.5,
            2, 0.08, 0.02, 0.08, 0.005);

        if (strokes >= STROKES_TO_LIGHT) {
            // Light a campfire — the focus of the fire system (§3). It starts with a full fuel load.
            serverLevel.setBlockAndUpdate(fire,
                Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
            serverLevel.sendParticles(ParticleTypes.FLAME, fire.getX() + 0.5, fire.getY() + 0.1, fire.getZ() + 0.5,
                6, 0.15, 0.05, 0.15, 0.01);
            ACTIVE.remove(player.getUUID());
        } else {
            ACTIVE.put(player.getUUID(), new Drill(base, strokes, player.tickCount));
        }
    }
}
