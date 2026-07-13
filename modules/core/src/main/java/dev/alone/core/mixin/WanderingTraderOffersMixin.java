package dev.alone.core.mixin;

import dev.alone.core.WanderingTradeOffers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replace the wandering trader's stock with Alone's caravan economy (see {@link WanderingTradeOffers}).
 *
 * <p>In 26.2 a wandering trader fills its trades in {@code updateTrades(ServerLevel)} from
 * {@code TradeSets.WANDERING_TRADER_*} — a one-way list that only ever <em>sells</em> the player oddments
 * (dyes, saplings, sand, coral) and never buys anything, so vanilla gives the player no way to earn emeralds.
 * That's the wrong shape for a specialised farming economy. We let vanilla build its list, then at the tail of
 * {@code updateTrades} wipe it and install our own curated set: high-volume buys for the player's surplus
 * (grain, wool, roots, leather…) plus flat-priced imports the land can't provide (metal, stone, timber, salt,
 * exotic foods). Injecting at {@code TAIL} (after {@code getOffers()} has been populated) and clearing means the
 * final offer list is exactly ours, with none of the vanilla clutter left over.
 */
@Mixin(WanderingTrader.class)
public class WanderingTraderOffersMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void alone$installCaravanOffers(final ServerLevel level, final CallbackInfo ci) {
        MerchantOffers offers = ((WanderingTrader) (Object) this).getOffers();
        offers.clear();
        WanderingTradeOffers.populate(offers);
    }
}
