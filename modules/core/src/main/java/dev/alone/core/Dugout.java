package dev.alone.core;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Starting a dugout canoe (§ boats). You don't craft a canoe — you take a felled log and begin hollowing
 * the real thing. <b>Sneak + right-click a placed log with an axe</b> to begin a dugout: the log becomes a
 * {@link AloneBlocks#DUGOUT_BLANK dugout blank} in place, which you then char and scrape hollow (see
 * {@link DugoutBlock}). Sneak is what tells this apart from ordinary axe-stripping (strip a log the usual
 * way, un-sneaked) — and the char/scrape that follow are plain, un-sneaked right-clicks on the blank, so
 * they never collide with this. No crafting recipe: the log itself is the canoe.
 */
public final class Dugout {
    private Dugout() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack held = player.getMainHandItem();
            if (!held.is(ItemTags.AXES)) {
                return InteractionResult.PASS; // only an axe begins a dugout
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS)) {
                return InteractionResult.PASS; // must be a real log to hollow
            }
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, AloneBlocks.DUGOUT_BLANK.defaultBlockState());
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.8f, 0.7f);
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("You start hollowing the log into a canoe — "
                        + "char the inside with a flame, then scrape the burnt wood out with an axe."), true);
                }
            }
            return InteractionResult.SUCCESS; // consume the click so the axe doesn't just strip the log
        });
    }
}
