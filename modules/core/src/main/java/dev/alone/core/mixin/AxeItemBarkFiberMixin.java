package dev.alone.core.mixin;

import dev.alone.core.AloneItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bark is cordage (realism / §8.1). Real plant fibre for cordage comes from grasses and ferns (see
 * {@code Fibers}) — and from the <b>inner bark (bast) of trees</b>: basswood, cedar, willow, elm. Leaves are
 * NOT a fibre source, so there's no leaf→fibre; but <b>debarking a log</b> yields the bark fibre. When an axe
 * strips a log (vanilla debarking, log → stripped log), we drop a little plant fibre — the bast you peeled.
 * This matters most in <b>winter</b>, when the grasses are dead or snowed under and a stand of trees is your
 * cordage. No exploit: vanilla only strips a <em>raw</em> log (a stripped log has no bark left to take), so a
 * log gives its fibre exactly once.
 */
@Mixin(AxeItem.class)
public class AxeItemBarkFiberMixin {
    @Inject(method = "useOn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void alone$barkFibre(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // The block hasn't been swapped yet, so this is still the raw log being debarked. Only fires on an
        // actual strip (a stripped/non-log block never reaches this setBlock), and only for logs (not copper).
        if (context.getPlayer() instanceof ServerPlayer player && level instanceof ServerLevel serverLevel
                && level.getBlockState(pos).is(BlockTags.LOGS)) {
            int fibre = 1 + player.getRandom().nextInt(2); // 1–2 lengths of bast peeled from the bark
            Block.popResource(serverLevel, pos, new ItemStack(AloneItems.PLANT_FIBER, fibre));
        }
    }
}
