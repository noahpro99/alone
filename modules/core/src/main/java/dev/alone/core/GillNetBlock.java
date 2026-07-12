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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A gill net (§7.2 — trapping/fishing) — the portable, open-water counterpart to the fixed {@link FishTrapBlock
 * weir}. A weir is a woven basket that funnels fish at <em>any</em> shoreline; a gill net is a mesh of cordage
 * you string across <b>open water</b>. It fishes <b>faster</b> and holds a small <b>batch</b> (up to three)
 * before you haul it up — but it only works where there's a real span of deep water (see
 * {@link FishStock#isOpenWater}): set it on a puddle or a narrow creek and the fish just go around it. So the
 * weir is your shore/pond method and the net is your lake/coast method, and both draw on the same finite,
 * richness-scaled {@link FishStock}. Right-click to haul up the whole catch; break it to pick the net back
 * up. Sits on the water surface like a lily pad. Placeholder cobweb (mesh) art until real net art lands.
 */
public class GillNetBlock extends Block {
    public static final IntegerProperty FISH = IntegerProperty.create("fish", 0, 3);
    private static final int MAX_HELD = 3;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    /** Faster than the weir (0.03) — a net strung across open water is efficient — but gated to open water. */
    private static final float CATCH_CHANCE = 0.05f;

    public GillNetBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FISH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FISH);
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
        int held = state.getValue(FISH);
        if (held >= MAX_HELD) {
            return; // full — haul it up before it can take more
        }
        if (!level.getFluidState(pos.below()).is(FluidTags.WATER) || !FishStock.isOpenWater(level, pos)) {
            return; // stranded, or not a wide enough span of water to fish a net
        }
        if (random.nextFloat() < CATCH_CHANCE && FishStock.drawFromTrap(level, pos)) {
            level.setBlockAndUpdate(pos, state.setValue(FISH, held + 1));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        int held = state.getValue(FISH);
        if (held <= 0) {
            return InteractionResult.PASS; // set and empty — nothing in it yet
        }
        if (!level.isClientSide()) {
            for (int i = 0; i < held; i++) {
                ItemStack fish = level.getRandom().nextFloat() < 0.35f
                    ? new ItemStack(Items.SALMON) : new ItemStack(Items.COD);
                popResource(level, pos, fish);
            }
            level.setBlockAndUpdate(pos, state.setValue(FISH, 0)); // hauled up — reset for the next batch
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
