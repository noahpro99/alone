package dev.alone.core;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;

/**
 * The placeable lit torch. Placing plants an ordinary torch/wall-torch block, but we remember how much
 * fuel the torch had at that spot ({@link Torches#registerPlaced}) so breaking it later hands back a
 * torch with the <em>same</em> remaining fuel — you can't refill a torch by planting and re-mining it.
 */
public class TorchBlockItem extends StandingAndWallBlockItem {
    public TorchBlockItem(Block block, Block wallBlock, Direction attachmentDirection, Properties properties) {
        super(block, wallBlock, attachmentDirection, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        int fuelUsed = context.getItemInHand().getDamageValue(); // remaining fuel = maxDamage - this
        InteractionResult result = super.place(context);
        if (!context.getLevel().isClientSide()
            && context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof TorchBlock) {
            Torches.registerPlaced(context.getLevel(), context.getClickedPos(), fuelUsed);
        }
        return result;
    }
}
