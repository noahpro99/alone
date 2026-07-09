package dev.alone.core.mixin;

import dev.alone.core.Carry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two rules on dropping items (proposal §5.1):
 * <ul>
 *   <li><b>You can't throw block-type items out of your inventory</b> — place them or store them in a
 *       chest. A player-initiated throw of a {@link BlockItem} is refused and the item goes back.</li>
 *   <li>You can't <em>throw</em> anything heavy far — the horizontal toss scales down by weight, so a
 *       full block just drops at your feet.</li>
 * </ul>
 * Both apply to player throws only ({@code thrownFromHand}); mob and death drops are untouched.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDropMixin {
    /** Refuse to throw block items out of the inventory — hand them back instead. */
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("HEAD"), cancellable = true)
    private void alone$noThrowingBlocks(ItemStack stack, boolean randomly, boolean thrownFromHand,
                                        CallbackInfoReturnable<ItemEntity> cir) {
        if (thrownFromHand && (Object) this instanceof Player player && stack.getItem() instanceof BlockItem) {
            player.getInventory().add(stack); // put it back; you place blocks, you don't chuck them
            if (stack.isEmpty()) {
                cir.setReturnValue(null); // nothing dropped
            }
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("RETURN"))
    private void alone$heavyDropsDontThrow(ItemStack stack, boolean randomly, boolean thrownFromHand,
                                           CallbackInfoReturnable<ItemEntity> cir) {
        if (!((Object) this instanceof Player)) {
            return;
        }
        ItemEntity entity = cir.getReturnValue();
        if (entity == null) {
            return;
        }
        float weight = Carry.itemWeight(entity.getItem());
        float factor = Math.max(0f, Math.min(1f, (20f - weight) / 15f)); // full toss <5 kg → no toss ≥20 kg
        if (factor < 1f) {
            Vec3 m = entity.getDeltaMovement();
            entity.setDeltaMovement(m.x * factor, m.y, m.z * factor);
        }
    }
}
