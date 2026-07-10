package dev.alone.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A backpack set down as a block (proposal §6) — right-click it to open its 27 slots like a chest;
 * break it to pick the pack (and its contents) back up. Placed by {@link BackpackItem} on a sneak-use,
 * and the contents are handed back on break by {@link Backpacks}.
 */
public class BackpackBlock extends BaseEntityBlock {
    public static final MapCodec<BackpackBlock> CODEC = simpleCodec(BackpackBlock::new);

    public BackpackBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // render from the blockstate model, not a block-entity renderer
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
            player.openMenu(backpack);
        }
        return InteractionResult.SUCCESS;
    }
}
