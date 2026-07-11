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
 * dirty vessel taints the next fill.
 *
 * <p><b>Boiling</b> is the cure, and it takes time over a fire (see {@link Campfires}): set a
 * <b>fire-safe</b> vessel — the iron pot — on a lit campfire and it comes to a boil and turns clean.
 * A hide waterskin can't go over the flame (you boil in the pot and carry in the skin, or fill from
 * clean rain). Boiling does <b>not</b> desalinate seawater — that needs a still (§2). Capacity varies:
 * a waterskin holds a few sips, an iron pot far more.
 */
public class WaterskinItem extends Item {
    public static final int RAW = 0;
    public static final int CLEAN = 1;
    public static final int TAINTED = 2;
    public static final int SALT = 3;

    private static final float THIRST_PER_SIP = 30f;
    private static final float SALT_DEHYDRATE_SIP = 18f; // a sip of seawater dehydrates (§1.2)

    private final int maxCharges;
    /** Metal/clay vessels can sit over a fire to boil; a hide skin or bark cup cannot (§2). */
    private final boolean fireSafe;

    public WaterskinItem(Properties properties, int maxCharges, boolean fireSafe) {
        super(properties);
        this.maxCharges = maxCharges;
        this.fireSafe = fireSafe;
    }

    public boolean isFireSafe() {
        return this.fireSafe;
    }

    public int maxCharges() {
        return this.maxCharges;
    }

    /** Right-click a cauldron of (rain/distilled) water to fill clean — a fuel-free clean source (§1.2). */
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
                stack.set(AloneItems.VESSEL_DIRTY, false); // clean water; sterilises the vessel
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState()); // drained
            }
            context.getPlayer().swing(context.getHand());
            return InteractionResult.SUCCESS;
        }
        // Set an EMPTY clay pot down under the open sky to catch rain (§2) — the early rain-catcher, before
        // you have iron for a cauldron. Only the clay pot places, and only when empty (pour or drink first).
        if (this == AloneItems.CLAY_POT && context.getPlayer() != null
            && context.getItemInHand().getOrDefault(AloneItems.WATER_CHARGES, 0) == 0) {
            BlockPos placePos = pos.relative(context.getClickedFace());
            BlockState target = AloneBlocks.CLAY_POT.defaultBlockState();
            if (level.getBlockState(placePos).canBeReplaced() && target.canSurvive(level, placePos)) {
                if (!level.isClientSide()) {
                    level.setBlockAndUpdate(placePos, target);
                    PlacedBlocks.markPlaced(level, placePos); // you set it down — loose, quick to pick back up
                    if (!context.getPlayer().isCreative()) {
                        context.getItemInHand().shrink(1);
                    }
                }
                context.getPlayer().swing(context.getHand());
                return InteractionResult.SUCCESS;
            }
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
                // Salt water fills briny; a dirty vessel OR warm stagnant swamp/jungle water fills murky
                // (tainted — higher sickness until boiled); clear fresh water fills merely raw.
                int quality;
                if (Drinking.isSaltWater(level, water)) {
                    quality = SALT;
                } else if (stack.getOrDefault(AloneItems.VESSEL_DIRTY, false)
                    || Drinking.isStagnantWater(level, water)) {
                    quality = TAINTED;
                } else {
                    quality = RAW;
                }
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
            // seawater dehydrates instead of quenching — you'd have to distil it to drink (§1.2)
            SurvivalMeters.drink(player, -SALT_DEHYDRATE_SIP);
            stack.set(AloneItems.VESSEL_DIRTY, true); // salt residue
            player.sendSystemMessage(Component.literal("Salty — it only dries you out. Distil it, or find fresh water."));
        } else {
            SurvivalMeters.drink(player, THIRST_PER_SIP);
            SurvivalMeters.cool(player, 8f); // a sip eases the heat when you're overheated
            float sicknessChance = quality == CLEAN ? 0f : (quality == TAINTED ? 0.45f : 0.15f);
            if (quality != CLEAN) {
                stack.set(AloneItems.VESSEL_DIRTY, true); // raw/tainted water leaves residue
            }
            if (player.getRandom().nextFloat() < sicknessChance) {
                // Murky (tainted) water is the dysentery kind; plain raw water a milder upset.
                Conditions.contractWaterIllness(player, quality == TAINTED);
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
