package dev.alone.core;

import dev.alone.core.net.DrinkRequestPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Drink straight from water by cupping it in your hands — right-click a water source bare-handed
 * (proposal §1.2/§2).
 *
 * <p>Bare-hand right-clicks don't fire Fabric's use events ({@code Minecraft.startUseItem} skips
 * {@code useItem} for empty hands, and {@code useItemOn} needs a block crosshair target). So the
 * client mixin detects the intent and sends {@link DrinkRequestPayload}; the server re-validates
 * here with the bucket's own water-targeting and applies the drink. Untreated surface water
 * carries a real chance of sickness; boiling and vessels arrive with the water ladder (§2).
 */
public final class Drinking {
    private Drinking() {
    }

    private static final float DRINK_AMOUNT = 25f;
    private static final float SICKNESS_CHANCE = 0.15f;

    public static void init() {
        // Fabric invokes play payload receivers on the server thread, so we can apply directly.
        ServerPlayNetworking.registerGlobalReceiver(DrinkRequestPayload.TYPE,
            (payload, context) -> handleDrink(context.player()));
    }

    /** The bucket's exact water-targeting: POV raycast clipping to SOURCE_ONLY fluids, at block reach. */
    public static BlockPos findWaterSource(Player player, Level level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot())
            .scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        FluidState fluid = level.getFluidState(pos);
        return fluid.isSource() && fluid.getType() == Fluids.WATER ? pos : null;
    }

    private static void handleDrink(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty() || findWaterSource(player, player.level()) == null) {
            return;
        }
        Hygiene.wash(player); // cupping water also rinses your hands (§5.6)
        if (SurvivalMeters.getThirst(player) < SurvivalMeters.MAX_THIRST) {
            SurvivalMeters.drink(player, DRINK_AMOUNT);
            if (player.getRandom().nextFloat() < SICKNESS_CHANCE) {
                Conditions.addSickness(player, Conditions.FOODBORNE_ILLNESS_TICKS / 4);
            }
        }
    }
}
