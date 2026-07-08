package dev.alone.core;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Waterskin (proposal §2) — carry water, with real quality. Right-click a water source to fill;
 * right-click elsewhere to sip (restores thirst, spends a charge). Untreated water risks sickness,
 * and sipping raw/tainted water leaves residue that <b>dirties the vessel</b> — a dirty vessel taints
 * the next fill. <b>Boil it over a campfire</b> to make the water clean and sterilise the vessel.
 */
public class WaterskinItem extends Item {
    public static final int RAW = 0;
    public static final int CLEAN = 1;
    public static final int TAINTED = 2;

    public static final int MAX_CHARGES = 3;
    private static final float THIRST_PER_SIP = 30f;

    public WaterskinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Aiming at water → fill. A dirty vessel taints the fill (§2 contamination).
        if (Drinking.findWaterSource(player, level) != null) {
            if (!level.isClientSide()) {
                stack.set(AloneItems.WATER_CHARGES, MAX_CHARGES);
                stack.set(AloneItems.WATER_QUALITY, stack.getOrDefault(AloneItems.VESSEL_DIRTY, false) ? TAINTED : RAW);
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        // Otherwise → drink a sip if there's water in it.
        int charges = stack.getOrDefault(AloneItems.WATER_CHARGES, 0);
        if (charges > 0) {
            if (!level.isClientSide()) {
                SurvivalMeters.drink(player, THIRST_PER_SIP);
                int quality = stack.getOrDefault(AloneItems.WATER_QUALITY, RAW);
                float sicknessChance = quality == CLEAN ? 0f : (quality == TAINTED ? 0.45f : 0.15f);
                if (quality != CLEAN) {
                    stack.set(AloneItems.VESSEL_DIRTY, true); // raw/tainted water leaves residue
                }
                if (player.getRandom().nextFloat() < sicknessChance) {
                    Conditions.addSickness(player, Conditions.FOODBORNE_ILLNESS_TICKS / 4);
                }
                stack.set(AloneItems.WATER_CHARGES, charges - 1);
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
