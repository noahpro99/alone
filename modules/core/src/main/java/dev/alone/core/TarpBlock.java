package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tarp (§5.5) — a sewn, oiled hide <b>sheet</b>, not a solid block. Like the {@link RopeBlock rope}, it's
 * a thin, no-collision thing you drape rather than a 1&nbsp;m³ cube: you place it overhead and <b>walk
 * straight under it</b>. What makes it a shelter and not decoration is that it still <b>blocks the sky</b>
 * (and therefore rain): {@link #propagatesSkylightDown} is overridden to {@code false}, exactly as leaves
 * do, so the space beneath a pitched tarp reads as roofed and stays dry even though the sheet is thin and
 * you can pass through it. It doesn't burn, so you can pitch it over a fire; break it to pick it back up.
 * Placeholder canvas art until real tarp art lands.
 */
public class TarpBlock extends Block {
    /** A thin sheet at the top of the block — an overhead fly. Collision is off (see the block Properties),
     *  so this is just the selection outline; you stand and move freely in the space below it. */
    private static final VoxelShape SHAPE = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);

    public TarpBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return false; // a pitched tarp blocks the sky (and rain) even though it's a thin, walk-through sheet
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
