package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

        // Otherwise → drink a sip if there's water in it.
        int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
        if (charges > 0) {
            if (!level.isClientSide()) {
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
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
