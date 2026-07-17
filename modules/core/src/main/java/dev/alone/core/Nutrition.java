package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Deficiency, not variety (proposal §1.1). The old <b>food-fatigue</b> mechanic — eating one food group over
 * and over shrank your usable hunger — was pulled: it played harsher than real life, where a monotonous diet
 * is a morale/appetite problem, not a same-week collapse in how much you can eat. A proper model (calories,
 * fats, vitamins) is planned to replace it wholesale; until then diet realism is carried by the one
 * deficiency that genuinely kills on a survival timescale: <b>scurvy</b>.
 *
 * <p><b>Scurvy</b> — fresh food (fruit and vegetables, your vitamin C) staves it off. Go long enough with
 * none and you sicken: weakness first, then bleeding gums and reopening wounds. The classic survival killer,
 * and the classic cure — a little fresh food reverses it fast.
 */
public final class Nutrition {
    private Nutrition() {
    }

    // Fresh food (vitamin C) — fruit and vegetables. The only diet tracking kept for now (scurvy).
    private static final TagKey<Item> VEGETABLE = tag("nutrient_vegetable");
    private static final TagKey<Item> FRUIT = tag("nutrient_fruit");

    // Scurvy (§1.1): fresh food staves it off. Go long enough without any and you sicken: weakness first,
    // then bleeding gums and reopening wounds — reversed fast by a little fresh food.
    public static final AttachmentType<Long> LAST_VITAMIN =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "last_vitamin"), Codec.LONG);
    private static final long SCURVY_ONSET = 144000L;  // ~6 in-game days with no fresh food → symptoms begin
    private static final long SCURVY_SEVERE = 240000L; // ~10 in-game days → it turns deadly

    /** Touching this class registers its attachment. */
    public static void init() {
    }

    /** True once a player has gone long enough without fresh food to have scurvy (§1.1) — for the HUD. */
    public static boolean hasScurvy(Player player) {
        Long last = player.getAttached(LAST_VITAMIN);
        return last != null && player.level().getGameTime() - last >= SCURVY_ONSET;
    }

    /** Call when a player finishes eating a food. Server-side. Fresh food (fruit/veg) resets the scurvy clock. */
    public static void onEat(Player player, ItemStack food) {
        if (food.is(FRUIT) || food.is(VEGETABLE)) {
            player.setAttached(LAST_VITAMIN, player.level().getGameTime());
        }
    }

    /** Per-tick: only the scurvy clock now (the variety penalty was removed). */
    public static void tick(Player player) {
        tickScurvy(player);
    }

    /** Deficiency: too long without fresh food brings on scurvy — weakness, then it kills. */
    private static void tickScurvy(Player player) {
        if (player.tickCount % 20 != 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Long last = player.getAttached(LAST_VITAMIN);
        if (last == null) {
            player.setAttached(LAST_VITAMIN, now); // first seen — start the clock, no instant scurvy
            return;
        }
        long without = now - last;
        if (without < SCURVY_ONSET) {
            return;
        }
        boolean severe = without >= SCURVY_SEVERE;
        // Weakness and lethargy set in; a severe case adds sluggishness and reopens wounds.
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        if (severe) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, false, false, true));
            if (player.tickCount % 100 == 0) {
                player.hurtServer(level, level.damageSources().generic(), 1.0f); // bleeding gums, failing body
            }
        }
        if (player.tickCount % 600 == 0) {
            player.sendSystemMessage(Component.literal(severe
                ? "Your gums bleed and old wounds reopen — scurvy. You need fresh food, now."
                : "You feel weak and listless — you've had no fresh fruit or vegetables in days."));
        }
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", path));
    }
}
