package dev.alone.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tanning rack (proposal §7.3). Right-click with a {@link AloneItems#RAW_HIDE} (and a lump of
 * {@link AloneItems#ANIMAL_BRAINS} on hand) to stretch the green hide and begin brain-tanning it; it works
 * over a long, patient stretch of time into {@code minecraft:leather} (see {@link TanningRackBlockEntity}).
 * Right-click empty-handed to take the leather — or an unfinished hide — back. Break it to knock the hide loose.
 */
public class TanningRackBlock extends BaseEntityBlock {
    public static final MapCodec<TanningRackBlock> CODEC = simpleCodec(TanningRackBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9, 1.0);

    public TanningRackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TanningRackBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, AloneBlocks.TANNING_RACK_BLOCK_ENTITY, TanningRackBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TanningRackBlockEntity rack) {
            // Occupied: any click takes the hide/leather back. Empty: lay the held raw hide on to tan.
            if (!rack.heldHide().isEmpty()) {
                return rack.retrieve(level, player);
            }
            InteractionResult placed = rack.place(level, player, stack, hand);
            // Nothing laid on — defer to the empty-hand path rather than dead-ending on PASS (26.2 dispatch).
            return placed.consumesAction() ? placed : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TanningRackBlockEntity rack) {
            return rack.retrieve(level, player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TanningRackBlockEntity rack) {
            ItemStack hide = rack.heldHide();
            if (!hide.isEmpty()) {
                Block.popResource(level, pos, hide); // knock the stretched hide loose
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
