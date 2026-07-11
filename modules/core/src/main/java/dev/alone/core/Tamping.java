package dev.alone.core;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The tamp tool — rammed earth (roadmap: Building &amp; shelter; proposal §5.3). Loose soil caves when you
 * undercut it (see {@link dev.alone.core.mixin.DirtFallingMixin}), and timber shores it. But there's an
 * older way: <b>ram the earth firm.</b> Hold a <b>rock</b> or a <b>smithing hammer</b> and <b>sneak +
 * right-click a block of soil</b> to tamp it — hard, tiring work (it costs stamina). Tamped earth is
 * <b>packed rammed earth</b>: it <b>won't cave</b> on its own, and it <b>bears load like a post</b>, so a
 * tamped column shores the soil around it the same way a timber does. It's also <b>committed to structure</b>
 * — no longer a loose set-down block, so it breaks slow again ({@link PlacedBlocks}). A dirt-only way to
 * keep a dugout open, paid for in sweat instead of timber.
 */
public final class Tamping {
    private Tamping() {
    }

    private static final float TAMP_STAMINA = 8f; // ramming a block firm is real labour

    /** Packed {@link BlockPos#asLong()} positions a player has rammed firm (per chunk, saved). */
    public static final AttachmentType<Set<Long>> TAMPED = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "tamped_blocks"),
        Codec.LONG.listOf().xmap(HashSet::new, ArrayList::new));

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack held = player.getMainHandItem();
            if (!held.is(AloneItems.ROCK) && !held.is(AloneItems.SMITHING_HAMMER)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.DIRT) || isTamped(level, pos)) {
                return InteractionResult.PASS; // only loose earth can be rammed, and only once
            }
            if (!level.isClientSide()) {
                markTamped(level, pos);
                PlacedBlocks.reRoot(level, pos); // a rammed block is set into structure, not loose
                SurvivalMeters.exert(player, TAMP_STAMINA);
                level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1.0f, 0.6f);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal(
                        "You ram the earth firm — packed, it will hold and bear a load."), true);
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        });

        // A broken block is no longer packed — forget it, so a natural or replaced block starts fresh.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> unmark(level, pos));
    }

    public static void markTamped(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        Set<Long> set = chunk.getAttached(TAMPED);
        if (set == null) {
            set = new HashSet<>();
        }
        set.add(pos.asLong());
        chunk.setAttached(TAMPED, set);
    }

    /** Has this soil been rammed firm — so it won't cave, and bears load like a post? */
    public static boolean isTamped(BlockGetter level, BlockPos pos) {
        if (!(level instanceof Level real)) {
            return false;
        }
        LevelChunk chunk = real.getChunkAt(pos);
        Set<Long> set = chunk.getAttached(TAMPED);
        return set != null && set.contains(pos.asLong());
    }

    private static void unmark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        Set<Long> set = chunk.getAttached(TAMPED);
        if (set != null && set.remove(pos.asLong())) {
            chunk.setAttached(TAMPED, set);
        }
    }
}
