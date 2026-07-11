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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A clay pot set down in the open (proposal §2 — rain catchment). Rainwater is clean to begin with, so
 * a pot left under open sky slowly fills with <b>clean</b> water while it rains — the early-game answer
 * to fresh water before you have iron for a cauldron. Right-click it with a vessel to pour the caught
 * water across (already sterile — no boiling needed); right-click empty-handed to drink straight from it.
 * The same fired clay pot you boil with (see {@link WaterskinItem}); place an <em>empty</em> one to catch.
 */
public class ClayPotBlock extends BaseEntityBlock {
    public static final MapCodec<ClayPotBlock> CODEC = simpleCodec(ClayPotBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0.1875, 0.0, 0.1875, 0.8125, 0.625, 0.8125);

    /**
     * How much water is standing in the pot, 0..{@link ClayPotBlockEntity#CAPACITY} — mirrored into the
     * blockstate (not just the block entity) so the client can see it and draw the water surface. The
     * blockstate's {@code multipart} overlays a water plane at the matching height per level.
     */
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, ClayPotBlockEntity.CAPACITY);

    public ClayPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATER, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATER);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClayPotBlockEntity(pos, state);
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
            : createTickerHelper(type, AloneBlocks.CLAY_POT_BLOCK_ENTITY, ClayPotBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ClayPotBlockEntity pot
            && stack.getItem() instanceof WaterskinItem vessel) {
            return pot.fillVessel(level, player, stack, vessel);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ClayPotBlockEntity pot) {
            return pot.drink(level, player);
        }
        return InteractionResult.PASS;
    }
}
