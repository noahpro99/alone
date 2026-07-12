package dev.alone.core.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Villager trading is disabled for now (progression / roadmap: the emerald economy is to be replaced by a
 * food-barter, reputation-based one — until then vanilla trading is a hole that lets you buy iron/diamond
 * tools, armour, and food outright, skipping the whole metal chain and farming). We simply stop the trade
 * screen from opening: right-clicking a villager or wandering trader does nothing. Villagers otherwise carry
 * on as they are (professions, breeding, iron golems) — only the player-facing trade is switched off, so the
 * planned barter economy can take its place cleanly. Name-tagging still works (that path runs before this).
 */
@Mixin({Villager.class, WanderingTrader.class})
public class VillagerTradeDisableMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void alone$noTrading(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        cir.setReturnValue(InteractionResult.PASS);
    }
}
