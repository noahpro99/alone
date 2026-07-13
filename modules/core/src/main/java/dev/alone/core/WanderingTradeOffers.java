package dev.alone.core;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

/**
 * The wandering trader's stock — Alone's long-distance caravan economy (proposal §9 / progression).
 *
 * <p>This is the one place emeralds mean anything. An emerald here stands in for the Mesopotamian silver
 * shekel: a portable, agreed unit of account that lets a settled farmer turn a barn full of one thing into
 * a little of everything the land around them can't grow or dig. The whole point of this trade set is
 * <em>specialisation</em> — real ancient trade wasn't "buy the best gear," it was grain and wool (which a
 * river valley makes in embarrassing surplus) going out, and the things the alluvial plain simply lacks —
 * metal, stone, timber, salt, and a few exotic foods from far-off gardens — coming in. So the trader:
 *
 * <ul>
 *   <li><b>BUYS your surplus for emeralds, at volume.</b> Grain, hay, wool, roots, eggs, leather, flour and
 *       the like are bought at a set count-for-one-emerald rate with very high {@code maxUses}, so a season's
 *       overproduction of a single abundant crop can actually be <em>offloaded</em> — you can dump hundreds of
 *       wheat across one caravan's visits rather than being capped after a handful of trades. This is how the
 *       player earns the emeralds to spend.</li>
 *   <li><b>SELLS the imports the terrain can't provide.</b> Iron and copper ingots, iron/gold nuggets, worked
 *       stone, timber, flint, salt, bone, and exotic foods (cocoa as dates, sugar, sweet/glow berries, honey,
 *       cane) — the goods a treeless, stoneless, ore-poor floodplain has to trade <em>for</em>.</li>
 * </ul>
 *
 * <p>Every offer uses a flat {@code priceMultiplier} of {@code 0} so prices don't drift with demand — a stable
 * exchange rate is what makes a surplus worth accumulating. Deliberately no emerald-block / diamond junk: this
 * is a bronze-age barter counter, not an end-game shop. The full set is installed on every trader (rather than a
 * random subset) so the player can always count on being able to sell what they grow — a dependable market is
 * the thing a farming economy is built on. Installed via {@code WanderingTraderOffersMixin}.
 */
public final class WanderingTradeOffers {
    private WanderingTradeOffers() {}

    /** Standard per-emerald "buy" volume: how many trades of one surplus good a single trader will take. */
    private static final int BUY_USES = 40;
    /** Grain and wool are the bread-and-butter surplus — bought at an even higher volume so you can truly dump them. */
    private static final int STAPLE_USES = 64;
    /** Imports are scarcer, so the trader carries a smaller stock of each before it locks. */
    private static final int SELL_USES = 16;

    /**
     * Fill a freshly-cleared {@link MerchantOffers} with Alone's full caravan stock. Called from the mixin
     * after the vanilla offers are wiped, so this list is exactly what the player sees.
     */
    public static void populate(final MerchantOffers offers) {
        // ---- The trader BUYS your surplus for emeralds (you give the goods, you get 1 emerald) ----
        // High maxUses + a whole stack-ish cost per emerald = a real outlet for one-crop overproduction.
        buy(offers, Items.WHEAT, 20, STAPLE_USES);                    // grain — the floodplain staple
        buy(offers, Items.HAY_BLOCK, 3, BUY_USES);                    // baled grain, denser to haul
        buy(offers, Items.WOOL.pick(DyeColor.WHITE), 6, STAPLE_USES); // wool — the other great export
        buy(offers, Items.CARROT, 24, BUY_USES);
        buy(offers, Items.POTATO, 26, BUY_USES);
        buy(offers, Items.BEETROOT, 15, BUY_USES);
        buy(offers, Items.PUMPKIN, 6, BUY_USES);
        buy(offers, Items.EGG, 12, 32);                               // eggs stack to 16, keep the cost under it
        buy(offers, Items.LEATHER, 4, BUY_USES);                      // surplus hide off your hunting
        buy(offers, Items.STRING, 14, 32);
        buy(offers, Items.DRIED_KELP, 32, 32);
        buy(offers, AloneItems.FLOUR, 10, BUY_USES);                  // milled grain — worked surplus is still surplus

        // ---- The trader SELLS the imports the land lacks (you give emeralds, you get the good) ----
        // Metal & minerals — the ore-poor plain's biggest gap.
        sell(offers, 4, Items.IRON_INGOT, 1, SELL_USES);
        sell(offers, 1, Items.IRON_NUGGET, 3, SELL_USES);
        sell(offers, 2, Items.COPPER_INGOT, 1, SELL_USES);
        sell(offers, 1, Items.GOLD_NUGGET, 2, 12);
        sell(offers, 1, Items.FLINT, 6, SELL_USES);
        // Stone & timber — nothing a treeless, stoneless valley can quarry or fell for itself.
        sell(offers, 1, Items.STONE, 10, 20);
        sell(offers, 2, Items.OAK_LOG, 6, 20);
        // Salt — a genuine caravan trade good, and the mod's food-preservation key (§4.2).
        sell(offers, 1, AloneItems.SALT, 2, 20);
        sell(offers, 1, Items.BONE, 3, 12);
        // Exotic foods from far-off gardens — dates (cocoa), sweeteners and honey.
        sell(offers, 1, Items.COCOA_BEANS, 3, 12);
        sell(offers, 1, Items.SUGAR, 4, 12);
        sell(offers, 1, Items.SWEET_BERRIES, 5, 12);
        sell(offers, 2, Items.GLOW_BERRIES, 4, 8);
        sell(offers, 1, Items.HONEY_BOTTLE, 2, 12);
        sell(offers, 1, Items.SUGAR_CANE, 4, 12);                     // a cutting to start your own stand
    }

    /**
     * A "buy" offer: hand over {@code price} of {@code good} to receive a single emerald. Priced flat (no demand
     * drift) with a high {@code maxUses} so a large surplus of one thing can be converted across a visit.
     */
    private static void buy(final MerchantOffers offers, final ItemLike good, final int price, final int maxUses) {
        offers.add(new MerchantOffer(new ItemCost(good, price), new ItemStack(Items.EMERALD, 1), maxUses, 1, 0.0F));
    }

    /**
     * A "sell" offer: pay {@code emeralds} to receive {@code count} of an imported {@code good}. Flat-priced, with
     * a modest stock ({@code maxUses}) since imports are scarcer than home-grown surplus.
     */
    private static void sell(final MerchantOffers offers, final int emeralds, final ItemLike good, final int count, final int maxUses) {
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(good, count), maxUses, 1, 0.0F));
    }
}
