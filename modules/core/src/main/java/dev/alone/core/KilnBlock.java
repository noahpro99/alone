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
 * A kiln (proposal §3.2) — the first purpose-built heat structure, above the crude campfire pit-fire.
 * Built of fired bricks (real effort: dig clay, fire bricks, lay the kiln), it takes a load of unfired
 * ware and <b>fuel</b>, then works it slowly over a couple of minutes into hardened pottery/brick. Right-
 * click with unfired ware to load it, with fuel to feed it, or empty-handed to take the finished ware.
 */
public class KilnBlock extends BaseEntityBlock {
    public static final MapCodec<KilnBlock> CODEC = simpleCodec(KilnBlock::new);

    public KilnBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KilnBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, AloneBlocks.KILN_BLOCK_ENTITY, KilnBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof KilnBlockEntity kiln)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return take(level, kiln, player); // empty hand also works to take the ware out
        }
        // Feed it fuel.
        int fuel = Campfires.fuelValue(stack);
        if (fuel > 0) {
            if (!level.isClientSide()) {
                kiln.setAttached(KilnBlockEntity.FUEL, kiln.getAttachedOrElse(KilnBlockEntity.FUEL, 0) + fuel);
                kiln.setChanged();
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Load unfired ware (only if the kiln is empty).
        if (KilnBlockEntity.firedResult(stack.getItem()) != null) {
            if (!kiln.getAttachedOrElse(KilnBlockEntity.LOADED, ItemStack.EMPTY).isEmpty()) {
                return InteractionResult.PASS; // already loaded — take it first
            }
            if (!level.isClientSide()) {
                kiln.setAttached(KilnBlockEntity.LOADED, stack.copy());
                kiln.setAttached(KilnBlockEntity.PROGRESS, 0);
                kiln.setChanged();
                stack.setCount(0); // the whole stack goes in
            }
            return InteractionResult.SUCCESS;
        }
        // Held item does nothing here — defer to the empty-hand take so a click still pulls the ware
        // (26.2 dispatch: a bare PASS would never reach useWithoutItem).
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof KilnBlockEntity kiln)) {
            return InteractionResult.PASS;
        }
        return take(level, kiln, player);
    }

    private static InteractionResult take(Level level, KilnBlockEntity kiln, Player player) {
        ItemStack loaded = kiln.getAttachedOrElse(KilnBlockEntity.LOADED, ItemStack.EMPTY);
        if (loaded.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            kiln.removeAttached(KilnBlockEntity.LOADED);
            kiln.removeAttached(KilnBlockEntity.PROGRESS);
            kiln.setChanged();
            if (!player.getInventory().add(loaded)) {
                player.drop(loaded, false);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
