package dev.alone.core.mixin;

import dev.alone.core.Seasons;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Seasonal breeding (proposal §7.2/§10). Animals don't birth young in the dead of winter — the same season
 * that stops crops growing stops new life too. So you can't grow a herd, or let hunted game repopulate,
 * until the cold breaks; breeding resumes in spring. Winter is for surviving on what you stored, not for
 * increase. (Gates {@code canFallInLove}, so feeding a pair simply won't start them breeding while it's
 * winter.)
 */
@Mixin(Animal.class)
public class AnimalBreedingMixin {
    @Inject(method = "canFallInLove", at = @At("RETURN"), cancellable = true)
    private void alone$noWinterBreeding(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && Seasons.isWinter(((Animal) (Object) this).level())) {
            cir.setReturnValue(false);
        }
    }
}
