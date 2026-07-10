package dev.alone.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A bloomery (proposal §3.2/§8.2) — the primitive iron furnace, built of heat-resistant refractory clay.
 * Right-click with iron (raw iron) to charge it, with <b>charcoal</b> to fuel it, or empty-handed to take
 * the finished bloom. It works the ore into a bloom over a long, hot, charcoal-hungry burn (see
 * {@link BloomeryBlockEntity}).
 */
public class BloomeryBlock extends BaseEntityBlock {
    public static final MapCodec<BloomeryBlock> CODEC = simpleCodec(BloomeryBlock::new);

    public BloomeryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloomeryBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, AloneBlocks.BLOOMERY_BLOCK_ENTITY, BloomeryBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return take(level, bloomery, player);
        }
        // Charge it with charcoal/coal.
        int fuel = BloomeryBlockEntity.fuelValue(stack);
        if (fuel > 0) {
            if (!level.isClientSide()) {
                bloomery.setAttached(BloomeryBlockEntity.FUEL, bloomery.getAttachedOrElse(BloomeryBlockEntity.FUEL, 0) + fuel);
                bloomery.setChanged();
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Load iron ore (only if empty).
        if (BloomeryBlockEntity.smeltResult(stack.getItem()) != null) {
            if (!bloomery.getAttachedOrElse(BloomeryBlockEntity.LOADED, ItemStack.EMPTY).isEmpty()) {
                return InteractionResult.PASS; // already charged — take it first
            }
            if (!level.isClientSide()) {
                bloomery.setAttached(BloomeryBlockEntity.LOADED, stack.copy());
                bloomery.setAttached(BloomeryBlockEntity.PROGRESS, 0);
                bloomery.setChanged();
                stack.setCount(0);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) {
            return InteractionResult.PASS;
        }
        return take(level, bloomery, player);
    }

    private static InteractionResult take(Level level, BloomeryBlockEntity bloomery, Player player) {
        ItemStack loaded = bloomery.getAttachedOrElse(BloomeryBlockEntity.LOADED, ItemStack.EMPTY);
        if (loaded.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            bloomery.removeAttached(BloomeryBlockEntity.LOADED);
            bloomery.removeAttached(BloomeryBlockEntity.PROGRESS);
            bloomery.setChanged();
            if (!player.getInventory().add(loaded)) {
                player.drop(loaded, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
