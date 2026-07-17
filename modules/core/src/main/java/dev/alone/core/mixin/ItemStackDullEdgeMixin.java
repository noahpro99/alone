package dev.alone.core.mixin;

import dev.alone.core.AloneItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * An edge dulls; it doesn't vanish (proposal §8.5, honing). In this pack a tool's durability bar IS its edge
 * sharpness — you hone it back up on a whetstone ({@code Sharpening}). So an axe or knife worn to the end
 * shouldn't <b>disappear</b> like a snapped vanilla tool: the head is fine, the edge is just gone, and you
 * grind it sharp again. We clamp a cutting tool's damage to one shy of breaking, so it bottoms out <b>dull</b>
 * (a sliver of bar left) and stays in your hand instead of shattering into nothing.
 *
 * <p>Scoped to <b>edged cutting tools</b> — axes, pickaxes, swords, shovels, hoes, the flint knife — NOT the
 * consumables that genuinely wear away and are gone (a ferro rod sheds its ferrocerium, a bow-drill spindle
 * burns down, pyrite crumbles, a shield is battered apart): those still break normally.
 */
@Mixin(ItemStack.class)
public class ItemStackDullEdgeMixin {
    // applyDamage(newDamage, player, onBreak) breaks the item when newDamage reaches maxDamage; cap it below
    // that for edged tools so isBroken() never trips and the tool is kept, merely dull.
    @ModifyVariable(method = "applyDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int alone$dullNotBreak(int newDamage) {
        ItemStack self = (ItemStack) (Object) this;
        if (alone$isEdgedTool(self)) {
            return Math.min(newDamage, self.getMaxDamage() - 1); // dull to the nub, never gone
        }
        return newDamage;
    }

    @Unique
    private static boolean alone$isEdgedTool(ItemStack stack) {
        return stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(AloneItems.FLINT_KNIFE);
    }
}
