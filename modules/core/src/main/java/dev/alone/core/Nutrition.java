package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Nutrition variety / "food fatigue" (proposal §1.1): you can't live on bread. Foods belong to groups
 * (protein, vegetable, grain, fruit — datapack tags). Eating one group over and over raises fatigue
 * for that group, which <b>shrinks your usable max hunger</b>; a varied diet lets it recover. No
 * vitamins spreadsheet — just a nudge toward eating a bit of everything.
 */
public final class Nutrition {
    private Nutrition() {
    }

    public static final int MAX_FOOD = 20;
    private static final float EAT_GAIN = 0.25f;     // fatigue added per meal of a group
    private static final float DECAY = 0.00003f;     // fatigue shed per tick (all groups)
    private static final float PENALTY = 8f;         // max-hunger cut at full monotony (→ cap 12)

    private static final TagKey<Item> PROTEIN = tag("nutrient_protein");
    private static final TagKey<Item> VEGETABLE = tag("nutrient_vegetable");
    private static final TagKey<Item> GRAIN = tag("nutrient_grain");
    private static final TagKey<Item> FRUIT = tag("nutrient_fruit");

    public static final AttachmentType<Float> FAT_PROTEIN = fatigue("diet_protein");
    public static final AttachmentType<Float> FAT_VEGETABLE = fatigue("diet_vegetable");
    public static final AttachmentType<Float> FAT_GRAIN = fatigue("diet_grain");
    public static final AttachmentType<Float> FAT_FRUIT = fatigue("diet_fruit");

    // Scurvy (§1.1): fresh food — fruit and vegetables, your vitamin C — staves it off. Go long enough
    // without any and you sicken: weakness first, then bleeding gums and reopening wounds. The classic
    // survival killer, and the classic cure: a little fresh food reverses it fast.
    public static final AttachmentType<Long> LAST_VITAMIN =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "last_vitamin"), Codec.LONG);
    private static final long SCURVY_ONSET = 144000L;  // ~6 in-game days with no fresh food → symptoms begin
    private static final long SCURVY_SEVERE = 240000L; // ~10 in-game days → it turns deadly

    /** Touching this class registers its attachments. */
    public static void init() {
    }

    /** Call when a player finishes eating a food. Server-side. */
    public static void onEat(Player player, ItemStack food) {
        if (food.is(FRUIT) || food.is(VEGETABLE)) {
            player.setAttached(LAST_VITAMIN, player.level().getGameTime()); // fresh food resets the scurvy clock
        }
        AttachmentType<Float> group = groupOf(food);
        if (group == null) {
            return; // untagged food — neutral
        }
        float value = Math.min(1f, player.getAttachedOrElse(group, 0f) + EAT_GAIN);
        player.setAttached(group, value);
        if (value >= 0.7f && player.getRandom().nextFloat() < 0.34f) {
            player.sendSystemMessage(Component.literal("You're tired of eating the same thing — you need variety."));
        }
    }

    /** Per-tick: fatigue decays, and a monotonous diet caps how high hunger can fill. */
    public static void tick(Player player) {
        decay(player, FAT_PROTEIN);
        decay(player, FAT_VEGETABLE);
        decay(player, FAT_GRAIN);
        decay(player, FAT_FRUIT);
        float worst = Math.max(
            Math.max(get(player, FAT_PROTEIN), get(player, FAT_VEGETABLE)),
            Math.max(get(player, FAT_GRAIN), get(player, FAT_FRUIT)));
        int cap = MAX_FOOD - Math.round(worst * PENALTY);
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() > cap) {
            food.setFoodLevel(cap);
        }
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

    private static AttachmentType<Float> groupOf(ItemStack stack) {
        if (stack.is(PROTEIN)) {
            return FAT_PROTEIN;
        }
        if (stack.is(VEGETABLE)) {
            return FAT_VEGETABLE;
        }
        if (stack.is(GRAIN)) {
            return FAT_GRAIN;
        }
        if (stack.is(FRUIT)) {
            return FAT_FRUIT;
        }
        return null;
    }

    private static void decay(Player player, AttachmentType<Float> group) {
        float value = get(player, group);
        if (value > 0f) {
            player.setAttached(group, Math.max(0f, value - DECAY));
        }
    }

    private static float get(Player player, AttachmentType<Float> group) {
        return player.getAttachedOrElse(group, 0f);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", path));
    }

    private static AttachmentType<Float> fatigue(String path) {
        return AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", path), Codec.FLOAT);
    }
}
