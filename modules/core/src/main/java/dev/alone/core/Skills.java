package dev.alone.core;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;

/**
 * Skill by doing (proposal §8.4) — competence <b>earned through practice</b>, not spent from an XP pool.
 * Every time you work a craft (knapping flint, drilling fire) you get a little better at <b>that</b> craft:
 * quicker, surer, less waste. There's no bar to fill and nothing to spend — the odds just quietly improve,
 * the way a real hand learns its trade. Proficiency follows a <b>learning curve</b> — fast at first, then a
 * long plateau toward mastery — and it <b>stays with you through death</b> (you don't forget how to knap
 * because you drowned). It surfaces only as the occasional word that you've stepped up a tier.
 */
public final class Skills {
    private Skills() {
    }

    public static final String FLINTWORKING = "flintworking";
    public static final String FIRECRAFT = "firecraft";
    public static final String MINING = "mining";
    public static final String SMITHING = "smithing";

    /** Practice count at which you're halfway to mastery — sets the pace of the learning curve. */
    private static final int HALF_MASTERY = 30;

    private static final String[] TIERS = {"a Novice", "an Apprentice", "Skilled", "an Expert", "a Master"};

    /** Practice is persisted per player (survives death and relog) as a map of skill name to count. */
    public static final AttachmentType<Map<String, Integer>> SKILLS = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "skills"),
        Codec.unboundedMap(Codec.STRING, Codec.INT));

    /** Registers the skill-granting hooks (and forces the attachment to load) at mod init. */
    public static void init() {
        // Quarrying stone/ore builds the miner's hand — dig speed then rises (see PlayerDestroySpeedMixin).
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (!level.isClientSide() && !player.isCreative()
                && state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.getDestroySpeed(level, pos) > 0f) {
                gain(player, MINING, 1);
            }
        });
    }

    public static int practice(Player player, String skill) {
        return player.getAttachedOrElse(SKILLS, Map.of()).getOrDefault(skill, 0);
    }

    /** 0..1 mastery on a diminishing curve: {@code practice / (practice + HALF_MASTERY)}. */
    public static float proficiency(Player player, String skill) {
        int p = practice(player, skill);
        return (float) p / (p + HALF_MASTERY);
    }

    /** Log some practice at a craft; announces (once) when the work carries you into a new tier of skill. */
    public static void gain(Player player, String skill, int amount) {
        if (player.level().isClientSide() || amount <= 0) {
            return;
        }
        int before = tierIndex(proficiency(player, skill));
        Map<String, Integer> updated = new HashMap<>(player.getAttachedOrElse(SKILLS, Map.of()));
        updated.merge(skill, amount, Integer::sum);
        player.setAttached(SKILLS, updated);
        int after = tierIndex(proficiency(player, skill));
        if (after > before && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(
                "Your " + skill + " is improving — you're now " + TIERS[after] + ".")
                .withStyle(ChatFormatting.GREEN), true); // action bar
        }
    }

    private static int tierIndex(float proficiency) {
        if (proficiency < 0.15f) {
            return 0; // Novice
        }
        if (proficiency < 0.40f) {
            return 1; // Apprentice
        }
        if (proficiency < 0.65f) {
            return 2; // Skilled
        }
        if (proficiency < 0.85f) {
            return 3; // Expert
        }
        return 4; // Master
    }
}
