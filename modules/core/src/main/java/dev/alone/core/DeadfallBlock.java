package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A deadfall (proposal §7.2 — trapping) — the second rung of the trapping ladder above the {@link SnareBlock
 * snare}: a heavy stone propped over bait, the Paiute trap. Where the snare is free but passive and low-odds,
 * the deadfall <b>costs bait</b> (any food) and is <b>one-shot</b> — but a baited stone pulls game in far
 * better, so it's the trade a trapper makes: spend a scrap of food to bring the odds up, then re-bait after
 * each catch. Set it, right-click with food to arm it, and the propped stone drops when small game takes the
 * bait. Right-click the sprung trap to take the catch (more than a snare gives) and it collapses back to
 * <b>needing bait</b> again. Draws on the local {@link GameStock} like all hunting; reuses stone/stick
 * textures as <b>placeholder art</b>.
 */
public class DeadfallBlock extends Block {
    /** SET = propped but empty (needs bait); BAITED = armed and fishing for game; SPRUNG = dropped, holding a catch. */
    public enum Stage implements StringRepresentable {
        SET("set"), BAITED("baited"), SPRUNG("sprung");

        private final String name;

        Stage(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

    /** Chance per random tick, at full abundance, that a baited trap takes game — better than a bare snare
     *  because the bait draws them in, which is what the food cost buys you. Scaled by local abundance. */
    private static final float CATCH_CHANCE = 0.04f;
    private static final float HIDE_CHANCE = 0.35f;

    public DeadfallBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STAGE, Stage.SET));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        // Bait a set trap with any food to arm it.
        if (state.getValue(STAGE) == Stage.SET && stack.has(DataComponents.FOOD)) {
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.BAITED));
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(STAGE) != Stage.SPRUNG) {
            return InteractionResult.PASS; // set/baited and empty — nothing to take yet
        }
        if (!level.isClientSide()) {
            int meat = 1 + level.getRandom().nextInt(2); // 1–2 — a better haul than the snare
            popResource(level, pos, new ItemStack(Items.RABBIT, meat));
            if (level.getRandom().nextFloat() < HIDE_CHANCE) {
                popResource(level, pos, new ItemStack(Items.RABBIT_HIDE));
            }
            level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.SET)); // collapsed — re-bait to use again
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) != Stage.BAITED) {
            return; // only an armed, baited trap can catch
        }
        float abundance = GameStock.abundance(level, pos);
        if (random.nextFloat() < CATCH_CHANCE * abundance) {
            level.setBlockAndUpdate(pos, state.setValue(STAGE, Stage.SPRUNG));
            level.playSound(null, pos, SoundEvents.STONE_FALL, SoundSource.BLOCKS, 0.8f, 0.8f);
            GameStock.takeSmallGame(level, pos);
        }
    }
}
