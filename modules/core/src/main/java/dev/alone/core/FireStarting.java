package dev.alone.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.alone.core.net.FireDrillPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Friction fire (proposal §3.1) — no easy flint-and-steel. First lay an <b>unlit campfire</b> (crafted
 * from sticks + plant fibre); then hold right-click on it to drill it alight. Two tools work:
 * <ul>
 *   <li>A bare <b>stick</b> — a <i>hand drill</i>, spun between the palms: the desperate bootstrap. Slow,
 *       exhausting, and a long gamble (many strokes, low odds).</li>
 *   <li>A <b>{@link AloneItems#BOW_DRILL bow drill}</b> (two sticks + cordage) — the proper primitive fire
 *       tool. The bow spins the spindle far faster, so it catches quicker, for less stamina, more reliably.
 *       It wears with use.</li>
 * </ul>
 * Either way it's slow and <b>fails in the rain or when you're wet</b>, so keeping the fire dry matters.
 * Each held right-click is one "stroke" (the client mixin sends them); enough strokes on the same unlit
 * campfire catch its built-in fibre tinder and it takes light. (§8.1 fibre → §3.1 fire.)
 */
public final class FireStarting {
    private FireStarting() {
    }

    // Hand drill (bare stick): work up real heat, then every stroke is a long gamble — lighting takes
    // uncertain time and a lot of stamina, so a carried ember (instant) or a bow drill is precious.
    private static final int HAND_MIN_STROKES = 12;
    private static final float HAND_CATCH_CHANCE = 0.07f;
    private static final float HAND_STAMINA = 1.5f;
    // Bow drill: the bow spins the spindle far faster — it catches sooner, for less effort, more reliably.
    private static final int BOW_MIN_STROKES = 5;
    private static final float BOW_CATCH_CHANCE = 0.22f;
    private static final float BOW_STAMINA = 0.7f;

    private record Drill(BlockPos pos, int strokes, int atTick) {
    }

    private static final Map<UUID, Drill> ACTIVE = new HashMap<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(FireDrillPayload.TYPE,
            (payload, context) -> stroke(context.player()));
    }

    /** The unlit campfire you're aiming at (the thing you drill to light), or null if none. */
    public static BlockPos findUnlitCampfire(Player player, Level level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot())
            .scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        boolean campfire = state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
        return campfire && !state.getValue(BlockStateProperties.LIT) ? pos : null;
    }

    private static void stroke(ServerPlayer player) {
        Level level = player.level();
        boolean bow = player.getMainHandItem().is(AloneItems.BOW_DRILL);
        if (!bow && !player.getMainHandItem().is(Items.STICK)) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        int minStrokes = bow ? BOW_MIN_STROKES : HAND_MIN_STROKES;
        float catchChance = bow ? BOW_CATCH_CHANCE : HAND_CATCH_CHANCE;
        float staminaPerStroke = bow ? BOW_STAMINA : HAND_STAMINA;
        BlockPos fire = findUnlitCampfire(player, level);
        if (fire == null) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        // Wet wood never catches.
        if (level.isRainingAt(fire) || player.isInWaterOrRain()) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, fire.getX() + 0.5, fire.getY() + 0.5, fire.getZ() + 0.5,
                3, 0.1, 0.02, 0.1, 0.01);
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (SurvivalMeters.getStamina(player) <= 0f) {
            return; // too spent to keep drilling
        }

        Drill current = ACTIVE.get(player.getUUID());
        boolean continuing = current != null && current.pos.equals(fire) && player.tickCount - current.atTick <= 20;
        int strokes = continuing ? current.strokes + 1 : 1;

        SurvivalMeters.exert(player, staminaPerStroke);
        serverLevel.sendParticles(ParticleTypes.SMOKE, fire.getX() + 0.5, fire.getY() + 0.5, fire.getZ() + 0.5,
            2, 0.08, 0.02, 0.08, 0.005);

        if (strokes >= minStrokes && player.getRandom().nextFloat() < catchChance) {
            // It catches — a gamble that can take a few strokes or a long slog. The campfire takes light
            // (§3), a full fuel load. (A carried ember lights one instantly; see Embers.)
            serverLevel.setBlockAndUpdate(fire,
                level.getBlockState(fire).setValue(BlockStateProperties.LIT, Boolean.TRUE));
            serverLevel.sendParticles(ParticleTypes.FLAME, fire.getX() + 0.5, fire.getY() + 0.3, fire.getZ() + 0.5,
                6, 0.15, 0.05, 0.15, 0.01);
            if (bow) {
                // The spindle burns down and the cordage frays a little with every fire it makes.
                player.getMainHandItem().hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
            ACTIVE.remove(player.getUUID());
        } else {
            ACTIVE.put(player.getUUID(), new Drill(fire, strokes, player.tickCount));
        }
    }
}
