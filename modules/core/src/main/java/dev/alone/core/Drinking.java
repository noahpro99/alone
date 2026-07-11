package dev.alone.core;

import dev.alone.core.net.DrinkRequestPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
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
    private static final float SALT_DEHYDRATE = 15f; // seawater pulls water OUT of you (§1.2)
    private static final float SNOW_THIRST = 6f;     // a handful of snow melts to only a little water
    private static final float SNOW_CHILL = 10f;     // ...and chills your core as your body melts it

    // Ring of horizontal samples (~32 and ~64 blocks out, 8 compass points) to detect nearby open ocean.
    private static final int[][] COAST_SAMPLES = {
        {32, 0}, {-32, 0}, {0, 32}, {0, -32}, {23, 23}, {-23, 23}, {23, -23}, {-23, -23},
        {64, 0}, {-64, 0}, {0, 64}, {0, -64}, {45, 45}, {-45, 45}, {45, -45}, {-45, -45}
    };

    /**
     * Salt water — drinking it dehydrates unless boiled first. Open ocean/beach/shore is salt outright;
     * and because Minecraft labels shallow coastal water with the neighbouring land/river biome, any
     * water at sea level with open ocean nearby is treated as sea too. Genuinely inland water stays fresh.
     */
    public static boolean isSaltWater(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || biome.is(Biomes.STONY_SHORE)) {
            return true;
        }
        if (Math.abs(pos.getY() - level.getSeaLevel()) <= 2) {
            for (int[] offset : COAST_SAMPLES) {
                if (level.getBiome(pos.offset(offset[0], 0, offset[1])).is(BiomeTags.IS_OCEAN)) {
                    return true; // coastal sea water mislabelled as land/river
                }
            }
        }
        return false;
    }

    /**
     * Warm, stagnant water — swamp, mangrove, jungle pools. Still and warm, it's a broth for pathogens, so
     * it's the worst thing to drink untreated and it fills a vessel <b>murky</b> (see {@link WaterskinItem}).
     */
    public static boolean isStagnantWater(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    /**
     * How risky a raw mouthful is, by where it's drawn (§1.2 / §2). The real rule: <b>cold, clear, moving
     * water is far safer than warm, still water</b> — but none of it is truly safe (giardia doesn't care how
     * pretty the stream is), so there's always a floor. Boiling remains the only sure cure.
     */
    public static float rawSicknessChance(Level level, BlockPos pos) {
        if (isStagnantWater(level, pos)) {
            return 0.45f; // warm swamp/jungle water — drinking it raw is asking for dysentery
        }
        float temp = level.getBiome(pos).value().getBaseTemperature();
        if (temp < 0.2f) {
            return 0.06f; // cold mountain/tundra water — clear and cold, much safer (never zero)
        }
        if (temp > 0.9f) {
            return 0.25f; // warm, dry country — the water sits warm and breeds bugs
        }
        return SICKNESS_CHANCE; // temperate — the ~15% baseline
    }

    public static void init() {
        // Fabric invokes play payload receivers on the server thread, so we can apply directly.
        ServerPlayNetworking.registerGlobalReceiver(DrinkRequestPayload.TYPE,
            (payload, context) -> handleDrink(context.player()));

        // Rain catch (§1.2): a cauldron collects clean rainwater. Bare-hand right-click to drink clean
        // (no sickness), spending a level. (Filling a waterskin from it lives in WaterskinItem.)
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!level.getBlockState(pos).is(Blocks.WATER_CAULDRON)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                Hygiene.wash(player);
                if (SurvivalMeters.getThirst(player) < SurvivalMeters.MAX_THIRST) {
                    SurvivalMeters.drink(player, DRINK_AMOUNT); // rain water is clean — no sickness roll
                }
                LayeredCauldronBlock.lowerFillLevel(level.getBlockState(pos), level, pos);
            }
            return InteractionResult.SUCCESS;
        });

        // Eating snow (§1.2/§1.3): bare-hand right-click a snow layer or block to eat a handful. Frozen
        // precipitation is clean, so it slakes a little thirst with no sickness — but your body spends heat
        // melting it, so it CHILLS you, badly if you're already cold. The real lesson: melt snow over a
        // fire, don't eat it. Snow's abundant, so a handful doesn't consume the block; the chill self-limits.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            var block = level.getBlockState(hit.getBlockPos());
            if (!block.is(Blocks.SNOW) && !block.is(Blocks.SNOW_BLOCK)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                if (SurvivalMeters.getThirst(player) < SurvivalMeters.MAX_THIRST) {
                    SurvivalMeters.drink(player, SNOW_THIRST);
                }
                SurvivalMeters.chill(player, SNOW_CHILL);
                if (player instanceof net.minecraft.server.level.ServerPlayer served) {
                    served.sendSystemMessage(Component.literal(
                        "You eat a handful of snow — a little water, but it chills you through. Better melted."), true);
                }
            }
            return InteractionResult.SUCCESS;
        });
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
        BlockPos water = findWaterSource(player, player.level());
        if (!player.getMainHandItem().isEmpty() || water == null) {
            return;
        }
        Hygiene.wash(player); // cupping water also rinses your hands (§5.6)
        if (isSaltWater(player.level(), water)) {
            SurvivalMeters.drink(player, -SALT_DEHYDRATE); // seawater only makes you thirstier (§1.2)
            player.sendSystemMessage(Component.literal(
                "The seawater is salty — it only makes you thirstier. Boiling won't help; you'd need to distil it, or find fresh water."));
            return;
        }
        SurvivalMeters.cool(player, 10f); // a mouthful of water eases the heat if you're overheated
        if (SurvivalMeters.getThirst(player) < SurvivalMeters.MAX_THIRST) {
            SurvivalMeters.drink(player, DRINK_AMOUNT);
            if (player.getRandom().nextFloat() < rawSicknessChance(player.level(), water)) {
                // Warm stagnant water is the dysentery kind; a passing upset otherwise.
                Conditions.contractWaterIllness(player, isStagnantWater(player.level(), water));
            }
        }
    }
}
