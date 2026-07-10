package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A water vessel (proposal §2 vessel ladder) — carry water, with real quality. Right-click a water
 * source to fill; right-click elsewhere to sip (restores thirst, spends a charge). Untreated water
 * risks sickness, and sipping raw/tainted water leaves residue that <b>dirties the vessel</b> — a
 * dirty vessel taints the next fill. <b>Boil it over a campfire</b> to clean the water and sterilise
 * the vessel. Capacity varies by vessel: a waterskin holds a few sips, an iron pot far more.
 */
public class WaterskinItem extends Item {
    public static final int RAW = 0;
    public static final int CLEAN = 1;
    public static final int TAINTED = 2;
    public static final int SALT = 3;

    private static final float THIRST_PER_SIP = 30f;
    private static final float SALT_DEHYDRATE_SIP = 18f; // a sip of seawater dehydrates (§1.2)

    private final int maxCharges;

    public WaterskinItem(Properties properties, int maxCharges) {
        super(properties);
        this.maxCharges = maxCharges;
    }

    /** Right-click a cauldron of (rain) water to fill clean — a fuel-free clean source (§1.2). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER_CAULDRON) && context.getPlayer() != null) {
            if (!level.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                int amount = Math.min(this.maxCharges, state.getValue(LayeredCauldronBlock.LEVEL));
                stack.set(AloneItems.WATER_CHARGES, amount);
                stack.set(AloneItems.WATER_QUALITY, CLEAN);
                stack.set(AloneItems.VESSEL_DIRTY, false); // clean rain water; sterilises the vessel
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState()); // drained
            }
            context.getPlayer().swing(context.getHand());
            return InteractionResult.SUCCESS;
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

    /** Drinking a full draught takes ~1.6s (a bit longer than a potion) — you can't chug on the run. */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    /** The potion-style glug while you sip from the vessel. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!level.isClientSide() && remainingUseTicks % 5 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS,
                0.5f, 0.9f + entity.getRandom().nextFloat() * 0.2f);
        }
    }

    /** The sip lands when the drinking animation completes. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return stack;
        }
        int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
        if (charges <= 0) {
            return stack;
        }
        int quality = stack.getOrDefault(AloneItems.WATER_QUALITY, RAW);
        if (quality == SALT) {
            // seawater dehydrates instead of quenching — boil it to make it drinkable (§1.2)
            SurvivalMeters.drink(player, -SALT_DEHYDRATE_SIP);
            stack.set(AloneItems.VESSEL_DIRTY, true); // salt residue
            player.sendSystemMessage(Component.literal("Salty — boil it first, or it just dries you out."));
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
