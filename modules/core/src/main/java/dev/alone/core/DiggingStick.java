package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Wear on the {@link AloneItems#DIGGING_STICK digging stick} (§5.4). The point blunts and the shaft splits
 * with the hard levering of packed earth, so each block of soil you turn with it costs a point of its life;
 * when it's spent it snaps. Its dig SPEED lives in {@code PlayerDestroySpeedMixin}; this only spends its
 * durability, and only on actual earth (not on the free plants/other blocks it can also break).
 */
public final class DiggingStick {
    private DiggingStick() {
    }

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || player.isCreative()) {
                return;
            }
            ItemStack tool = player.getMainHandItem();
            if (!tool.is(AloneItems.DIGGING_STICK)) {
                return;
            }
            // Only earth wears it — the shovel-mineable soils it's actually made for (dirt, grass, sand…).
            if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                return;
            }
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            if (tool.isEmpty()) {
                level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.7f, 1.1f);
            }
        });
    }
}
