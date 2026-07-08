package dev.alone.core.mixin;

import dev.alone.core.Carry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * You can't <em>throw</em> something heavy (proposal §5.1). Vanilla flings a dropped item forward at a
 * fixed speed; we scale that horizontal throw down by the item's weight, so a full block of dirt just
 * drops at your feet while light things still toss normally. Vertical motion is left alone so it still
 * clears your hitbox. Players only — mob death drops are untouched.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDropMixin {
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
