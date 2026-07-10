package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A water vessel (proposal §2 vessel ladder) — carry water, with real quality. Right-click a water
 * source to fill; right-click elsewhere to sip (restores thirst, spends a charge). Untreated water
 * risks sickness, and sipping raw/tainted water leaves residue that <b>dirties the vessel</b> — a
 * dirty vessel taints the next fill.
 *
 * <p><b>Boiling</b> is the cure, and it's the real one (§2, deferring to how the show handles water):
 * a <b>fire-safe</b> vessel — the iron pot — set over a <b>lit campfire</b> (sneak+hold right-click on
 * it) comes to a rolling boil after a few seconds and turns its whole load <b>clean and safe</b>,
 * sterilising the vessel too. A hide waterskin can't go over the flame — you boil in the pot and carry
 * in the skin, or fill it from clean rain. Boiling does <b>not</b> desalinate seawater; that needs
 * distillation, so salt water stays salt. Capacity varies: a waterskin holds a few sips, an iron pot far
 * more.
 */
public class WaterskinItem extends Item {
    public static final int RAW = 0;
    public static final int CLEAN = 1;
    public static final int TAINTED = 2;
    public static final int SALT = 3;

    private static final float THIRST_PER_SIP = 30f;
    private static final float SALT_DEHYDRATE_SIP = 18f; // a sip of seawater dehydrates (§1.2)
    private static final int SIP_TICKS = 32;             // ~1.6 s to drink a draught
    private static final int BOIL_TICKS = 80;            // ~4 s to bring a pot to a rolling boil

    private final int maxCharges;
    /** Metal/clay vessels can sit over a fire; a hide skin or bark cup cannot (§2). */
    private final boolean fireSafe;

    public WaterskinItem(Properties properties, int maxCharges, boolean fireSafe) {
        super(properties);
        this.maxCharges = maxCharges;
        this.fireSafe = fireSafe;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        // Right-click a cauldron of (rain) water to fill clean — a fuel-free clean source (§1.2).
        if (state.is(Blocks.WATER_CAULDRON)) {
            if (!level.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                int amount = Math.min(this.maxCharges, state.getValue(LayeredCauldronBlock.LEVEL));
                stack.set(AloneItems.WATER_CHARGES, amount);
                stack.set(AloneItems.WATER_QUALITY, CLEAN);
                stack.set(AloneItems.VESSEL_DIRTY, false); // clean rain water; sterilises the vessel
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState()); // drained
            }
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        // Set a fire-safe vessel over a lit campfire to boil its water clean (§2).
        if (isLitCampfire(state)) {
            ItemStack stack = context.getItemInHand();
            int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
            int quality = stack.getOrDefault(AloneItems.WATER_QUALITY, RAW);
            if (charges <= 0) {
                return InteractionResult.PASS; // nothing to boil
            }
            if (!this.fireSafe) {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal(
                        "A hide waterskin can't go over the fire — boil water in a pot, or fill from clean rain."));
                }
                return InteractionResult.SUCCESS;
            }
            if (quality == CLEAN) {
                return InteractionResult.PASS; // already safe
            }
            if (quality == SALT) {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal(
                        "Boiling won't take the salt out — you'd need to distil it. Find fresh water."));
                }
                return InteractionResult.SUCCESS;
            }
            player.startUsingItem(context.getHand()); // a rolling boil takes a few seconds
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Aiming at water → fill. Ocean water fills as salt; a dirty vessel taints the fill (§2).
        BlockPos water = Drinking.findWaterSource(player, level);
        if (water != null) {
            if (!level.isClientSide()) {
                stack.set(AloneItems.WATER_CHARGES, this.maxCharges);
                int quality = Drinking.isSaltWater(level, water) ? SALT
                    : (stack.getOrDefault(AloneItems.VESSEL_DIRTY, false) ? TAINTED : RAW);
                stack.set(AloneItems.WATER_QUALITY, quality);
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        // Otherwise → drink, if there's water in it. Drinking takes a moment (a real pull from the
        // vessel), so we start a timed use and apply the sip when it finishes.
        if (stack.getOrDefault(AloneItems.WATER_CHARGES, 0) > 0) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** A boil over the fire takes longer than a sip; drinking a draught is ~1.6 s. */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isBoiling(stack, entity) ? BOIL_TICKS : SIP_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    /** Bubbling over the fire while boiling; the potion-style glug while you sip. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide()) {
            return;
        }
        if (isBoiling(stack, entity)) {
            if (remainingUseTicks % 4 == 0) {
                level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                    0.4f, 1.6f + entity.getRandom().nextFloat() * 0.2f);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getEyeY(), entity.getZ(),
                        2, 0.1, 0.05, 0.1, 0.01);
                }
            }
        } else if (remainingUseTicks % 5 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS,
                0.5f, 0.9f + entity.getRandom().nextFloat() * 0.2f);
        }
    }

    /** The boil purifies the whole vessel, or the sip lands, when the use completes. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return stack;
        }

        // Finished a boil over the fire → the whole load is clean and the vessel sterilised.
        if (isBoiling(stack, entity)) {
            stack.set(AloneItems.WATER_QUALITY, CLEAN);
            stack.set(AloneItems.VESSEL_DIRTY, false);
            level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.7f, 1.3f);
            player.sendSystemMessage(Component.literal("The water rolls to a boil — clean and safe to drink."));
            return stack;
        }

        int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
        if (charges <= 0) {
            return stack;
        }
        int quality = stack.getOrDefault(AloneItems.WATER_QUALITY, RAW);
        if (quality == SALT) {
            // seawater dehydrates instead of quenching — you'd have to distil it to drink (§1.2)
            SurvivalMeters.drink(player, -SALT_DEHYDRATE_SIP);
            stack.set(AloneItems.VESSEL_DIRTY, true); // salt residue
            player.sendSystemMessage(Component.literal("Salty — it only dries you out. Distil it, or find fresh water."));
        } else {
            SurvivalMeters.drink(player, THIRST_PER_SIP);
            float sicknessChance = quality == CLEAN ? 0f : (quality == TAINTED ? 0.45f : 0.15f);
            if (quality != CLEAN) {
                stack.set(AloneItems.VESSEL_DIRTY, true); // raw/tainted water leaves residue
            }
            if (player.getRandom().nextFloat() < sicknessChance) {
                Conditions.addSickness(player, Conditions.FOODBORNE_ILLNESS_TICKS / 4);
            }
        }
        stack.set(AloneItems.WATER_CHARGES, charges - 1);
        return stack;
    }

    /** We're mid-boil when a fire-safe vessel with un-clean water is held while aimed at a lit campfire. */
    private boolean isBoiling(ItemStack stack, LivingEntity entity) {
        if (!this.fireSafe || !(entity instanceof Player player)) {
            return false;
        }
        int quality = stack.getOrDefault(AloneItems.WATER_QUALITY, RAW);
        if (quality == CLEAN || quality == SALT || stack.getOrDefault(AloneItems.WATER_CHARGES, 0) <= 0) {
            return false;
        }
        return aimingAtLitCampfire(player);
    }

    private static boolean isLitCampfire(BlockState state) {
        return (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
            && state.getValue(BlockStateProperties.LIT);
    }

    private static boolean aimingAtLitCampfire(Player player) {
        Level level = player.level();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot())
            .scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && isLitCampfire(level.getBlockState(hit.getBlockPos()));
    }

    // A water gauge on the vessel (§2): the durability-style bar shows how many sips are left, tinted by
    // quality — blue clean, murky raw, sickly tainted, teal salt — so you can read your water at a glance.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(AloneItems.WATER_CHARGES, 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
        return Math.round(13f * charges / this.maxCharges);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return switch (stack.getOrDefault(AloneItems.WATER_QUALITY, RAW)) {
            case CLEAN -> 0x3C8CFF; // clean blue
            case TAINTED -> 0x7A8A38; // sickly green
            case SALT -> 0x2FB6B6; // brine teal
            default -> 0x6F88A0;   // murky raw
        };
    }
}
