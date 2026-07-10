package dev.alone.core.mixin;

import dev.alone.core.PlacedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When you pull up a block <b>you set down yourself</b> (a loose, player-placed block — see
 * {@link PlacedBlocks}), the drop goes <b>straight into your hands/pockets</b> instead of falling on the
 * ground. It's your own stuff; you're just picking it back up. Natural terrain still drops normally (and,
 * per the no-vacuum rule, you right-click those). The inventory add respects the volume budgets, so
 * anything that doesn't fit falls at your feet. Redirects the drop call in {@code playerDestroy} — the
 * block still breaks exactly as vanilla does, only the drop's destination changes (no desync).
 */
@Mixin(Block.class)
public class BlockLootToInventoryMixin {
    @Redirect(method = "playerDestroy", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/item/ItemStack;)V"))
    private void alone$dropsToInventory(BlockState state, Level level, BlockPos pos, BlockEntity be,
                                        Entity entity, ItemStack tool) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player
            && !player.isCreative() && PlacedBlocks.isPlaced(level, pos)) {
            for (ItemStack drop : Block.getDrops(state, serverLevel, pos, be, player, tool)) {
                player.getInventory().add(drop);          // volume-capped; shrinks to whatever didn't fit
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);  // hands/pockets full → the rest falls
                }
            }
            return;
        }
        Block.dropResources(state, level, pos, be, entity, tool); // natural block — drop on the ground
    }
}
