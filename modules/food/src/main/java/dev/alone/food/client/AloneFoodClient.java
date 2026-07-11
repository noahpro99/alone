package dev.alone.food.client;

import dev.alone.food.Spoilage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Puts the food's freshness on its tooltip, so you can tell at a glance how close a piece is to turning —
 * and whether it's been preserved. The state is stored as <b>components on the item itself</b> (a freshness
 * budget, plus {@code preserved}/{@code dried} flags), <em>not</em> a separate "jerky" item: a dried piece
 * is the same food, just marked, so it stacks and cooks like normal. This readout is how you see that.
 */
public class AloneFoodClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.is(Spoilage.PERISHABLE)) {
                return; // only perishables carry freshness
            }
            boolean preserved = stack.getOrDefault(Spoilage.PRESERVED, false);
            boolean dried = stack.getOrDefault(Spoilage.DRIED, false);
            if (preserved) {
                lines.add(Component.literal(dried ? "◈ Dried jerky — keeps for weeks"
                        : "◈ Salted — keeps for weeks").withStyle(ChatFormatting.GOLD));
            }

            // Freshness as a fraction of this item's own budget (preserved food starts with a much larger
            // one). Null means it hasn't been ticked yet — treat as brand-fresh.
            Long freshness = stack.get(Spoilage.FRESHNESS);
            long max = preserved ? Spoilage.PRESERVED_SHELF_TICKS : Spoilage.SPOIL_TICKS;
            double fraction = freshness == null ? 1.0 : Math.max(0.0, Math.min(1.0, (double) freshness / max));

            Component state;
            if (fraction >= 0.66) {
                state = Component.literal("Fresh").withStyle(ChatFormatting.GREEN);
            } else if (fraction >= 0.33) {
                state = Component.literal("Beginning to turn").withStyle(ChatFormatting.YELLOW);
            } else {
                state = Component.literal("Going off — eat it soon").withStyle(ChatFormatting.RED);
            }
            lines.add(Component.literal("Freshness: ").withStyle(ChatFormatting.GRAY).append(state));
        });
    }
}
