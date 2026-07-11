package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Deploys a {@link TravoisEntity} (proposal §6). Right-click the ground to set the sled down; hit the
 * sled to pack it back into this item. Crafted from poles (sticks) and a hide (leather) with cordage.
 */
public class TravoisItem extends Item {
    public TravoisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            TravoisEntity travois = new TravoisEntity(AloneEntities.TRAVOIS, level);
            travois.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (context.getPlayer() != null) {
                travois.setYRot(context.getPlayer().getYRot());
            }
            level.addFreshEntity(travois);
            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
