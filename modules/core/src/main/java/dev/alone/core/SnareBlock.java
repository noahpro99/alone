package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * A snare (proposal §7.2 — trapping) — a cordage noose set on a game trail, the first rung of the trapping
 * ladder and the way people actually eat on <i>Alone</i>: passive food that works while you're off doing
 * something else. Set it on the ground near cover and, over time, small game blunders into it. It catches
 * <b>rarely</b> and only draws from the local {@link GameStock game population}, so a snare in stripped
 * country catches little — you set a <b>line</b> of them and walk it, the way a real trapper does, rather
 * than relying on one. Right-click a sprung snare to take the catch (a scrap of small-game meat, sometimes
 * a hide) and it resets itself. Break it to take the snare back.
 *
 * <p>Catching runs off {@linkplain #randomTick random ticks}, which fire on loaded ground and scale with
 * the passage of game-time — so a snare near camp keeps working across the days you {@linkplain GradualSleep
 * fast-forward by resting}. Reuses string/tripwire textures as <b>placeholder art</b> for now.
 */
public class SnareBlock extends Block {
    public static final BooleanProperty CAUGHT = BooleanProperty.create("caught");
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 1.0, 14.0);

    /** Chance per random tick, at full local abundance, that game blunders in. Low on purpose — trapping
     *  is patient, mostly-empty work; you run several snares. Scaled down as the ground gets hunted out. */
    private static final float CATCH_CHANCE = 0.02f;
    private static final float HIDE_CHANCE = 0.25f; // and sometimes the catch is worth a small pelt

    public SnareBlock(Properties properties) {
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
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CAUGHT)) {
            return; // already sprung — nothing more wanders in until you clear it
        }
        float abundance = GameStock.abundance(level, pos);
        if (random.nextFloat() < CATCH_CHANCE * abundance) {
            level.setBlockAndUpdate(pos, state.setValue(CAUGHT, true));
            GameStock.takeSmallGame(level, pos); // a head of small game off the local population
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(CAUGHT)) {
            return InteractionResult.PASS; // set and empty — leave it be
        }
        if (!level.isClientSide()) {
            popResource(level, pos, new ItemStack(Items.RABBIT)); // a scrap of small-game meat
            if (level.getRandom().nextFloat() < HIDE_CHANCE) {
                popResource(level, pos, new ItemStack(Items.RABBIT_HIDE));
            }
            level.setBlockAndUpdate(pos, state.setValue(CAUGHT, false)); // reset the snare for the next one
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }
}
