package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fish trap / weir (proposal §7.2 — trapping) — the top rung of the trapping ladder and the real way you
 * fish on <i>Alone</i>: a woven basket set in the water that catches fish <b>passively</b> while you do
 * everything else, no rod, no standing on the bank. Set it at the water's edge and it draws on the same
 * finite, richness-scaled {@link FishStock fish stock} the rod does — so a trap in a small pond fishes it
 * out and then sits empty, while one on a lake or shore keeps producing. Right-click a trap holding a fish
 * to take it; break it to pick the trap back up. Reuses basket/wicker textures as <b>placeholder art</b>.
 *
 * <p>Sits on the water surface like a lily pad (placed with {@link net.minecraft.world.item.PlaceOnWaterBlockItem});
 * catching runs off random ticks, so a trap keeps working across the days you fast-forward by resting.
 */
public class FishTrapBlock extends Block {
    public static final BooleanProperty CAUGHT = BooleanProperty.create("caught");
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);

    /** Chance per random tick that a fish enters the trap — better than a land snare (a weir is productive),
     *  but every catch draws the local stock down, so a worked spot slows to nothing until it recovers. */
    private static final float CATCH_CHANCE = 0.03f;

    public FishTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CAUGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAUGHT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getFluidState(pos.below()).is(FluidTags.WATER); // must sit on water
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CAUGHT)) {
            return; // holding a fish — clear it before it can take another
        }
        if (!level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            return; // out of water — a stranded trap catches nothing
        }
        if (random.nextFloat() < CATCH_CHANCE && FishStock.drawFromTrap(level, pos)) {
            level.setBlockAndUpdate(pos, state.setValue(CAUGHT, true));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(CAUGHT)) {
            return InteractionResult.PASS; // set and empty — nothing in it yet
        }
        if (!level.isClientSide()) {
            ItemStack fish = level.getRandom().nextFloat() < 0.35f
                ? new ItemStack(Items.SALMON) : new ItemStack(Items.COD);
            popResource(level, pos, fish);
            level.setBlockAndUpdate(pos, state.setValue(CAUGHT, false)); // reset for the next one
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }
}
