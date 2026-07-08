package dev.alone.core.client;

import dev.alone.core.Carry;
import dev.alone.core.Conditions;
import dev.alone.core.SurvivalMeters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

/**
 * A small corner HUD (proposal §1: small, unobtrusive). Each meter has a game icon so it reads at a
 * glance: ❤ vitality, feather stamina, phantom-membrane fatigue, water-bucket thirst, bundle volume.
 */
public final class SurvivalHud {
    private SurvivalHud() {
    }

    private static final int BAR_W = 80;
    private static final int BAR_H = 4;

    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final ItemStack ICON_STAMINA = new ItemStack(Items.FEATHER);
    private static final ItemStack ICON_FATIGUE = new ItemStack(Items.PHANTOM_MEMBRANE);
    private static final ItemStack ICON_THIRST = new ItemStack(Items.WATER_BUCKET);
    private static final ItemStack ICON_VOLUME = new ItemStack(Items.BUNDLE);

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int left = 6;                 // top-left corner (proposal: small, corner-mounted)
        int barX = left + 13;         // leave room for a ~10px icon left of each bar
        int rowH = BAR_H + 8;         // taller rows so an icon fits per meter
        int y0 = 6;
        int yVitality = y0;
        int yStamina = y0 + rowH;
        int yFatigue = y0 + rowH * 2;
        int yThirst = y0 + rowH * 3;
        int yVolume = y0 + rowH * 4;

        // Vitality (§1.5) — replaces the hearts bar; drops from bleeding/injury. A real heart icon.
        float vitality = mc.player.getHealth() / Math.max(1f, mc.player.getMaxHealth());
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, left, yVitality - 2, 9, 9);
        drawBar(g, barX, yVitality, vitality, vitalityColor(vitality));

        drawItemIcon(g, ICON_STAMINA, left, yStamina - 3);
        drawBar(g, barX, yStamina, ClientSurvivalState.stamina / SurvivalMeters.MAX_STAMINA, 0xFF3AA655);

        drawItemIcon(g, ICON_FATIGUE, left, yFatigue - 3);
        // Endurance reserve — full when rested, drains as fatigue builds (so it reads like the rest:
        // full = good). Low reserve = you're worn down and your stamina can't refill as high.
        drawBar(g, barX, yFatigue, 1f - ClientSurvivalState.fatigue / 100f, 0xFFB5793A);

        drawItemIcon(g, ICON_THIRST, left, yThirst - 3);
        drawBar(g, barX, yThirst, ClientSurvivalState.thirst / SurvivalMeters.MAX_THIRST, 0xFF4A90D9);

        drawItemIcon(g, ICON_VOLUME, left, yVolume - 3);
        float volumePct = Carry.totalVolume(mc.player) / Carry.volumeLimit(mc.player);
        drawBar(g, barX, yVolume, volumePct, volumeColor(volumePct));

        // Body temperature — a small square to the right of the bars, blue (cold) → green → red (hot).
        int tx = barX + BAR_W + 6;
        g.fill(tx - 1, yVitality - 1, tx + 9, yVitality + 9, 0xAA000000);
        g.fill(tx, yVitality, tx + 8, yVitality + 8, temperatureColor(ClientSurvivalState.temperature));
        drawTrendArrow(g, tx + 11, yVitality, ClientSurvivalState.temperatureTrend);

        // A little figure — a paper-doll of your body: bleeding torso, sprained legs, dirty hands, illness.
        drawPerson(g, tx + 24, yVitality, ClientSurvivalState.conditions);
    }

    /** Draw a game item as a ~10px icon (item icons are 16px, so scale them down). */
    private static void drawItemIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(0.625f, 0.625f);
        g.item(stack, 0, 0);
        pose.popMatrix();
    }

    /** Vitality reads like blood: healthy deep red, amber when hurt, bright alarm red when critical. */
    private static int vitalityColor(float pct) {
        if (pct <= 0.25f) {
            return 0xFFE23B2E; // critical
        }
        if (pct <= 0.5f) {
            return 0xFFCC6633; // hurt
        }
        return 0xFFB23A3A;     // healthy
    }

    private static void drawPerson(GuiGraphicsExtractor g, int x, int y, int conditions) {
        boolean sick = (conditions & Conditions.FLAG_SICK) != 0;
        boolean bleeding = (conditions & Conditions.FLAG_BLEEDING) != 0;
        boolean sprained = (conditions & Conditions.FLAG_SPRAINED) != 0;
        boolean dirty = (conditions & Conditions.FLAG_DIRTY_HANDS) != 0;

        int body = sick ? 0xFF7BA05B : 0xFFCBB89A; // sickly green, else skin tone
        int hands = dirty ? 0xFF6B4A2B : body;      // brown when dirty
        int torso = bleeding ? 0xFFB53030 : body;   // red when bleeding
        int legs = sprained ? 0xFFCC7A33 : body;    // orange when sprained

        g.fill(x - 1, y - 1, x + 11, y + 19, 0xAA000000); // backdrop
        g.fill(x + 3, y, x + 8, y + 5, body);             // head
        g.fill(x + 3, y + 5, x + 8, y + 12, torso);       // torso
        g.fill(x + 1, y + 5, x + 3, y + 11, body);        // left arm
        g.fill(x + 8, y + 5, x + 10, y + 11, body);       // right arm
        g.fill(x + 1, y + 11, x + 3, y + 13, hands);      // left hand
        g.fill(x + 8, y + 11, x + 10, y + 13, hands);     // right hand
        g.fill(x + 3, y + 12, x + 5, y + 18, legs);       // left leg
        g.fill(x + 6, y + 12, x + 8, y + 18, legs);       // right leg
    }

    private static void drawBar(GuiGraphicsExtractor g, int x, int y, float pct, int color) {
        pct = Math.max(0f, Math.min(1f, pct));
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xAA000000); // border/background
        g.fill(x, y, x + (int) (BAR_W * pct), y + BAR_H, color);        // fill
    }

    private static int volumeColor(float pct) {
        if (pct >= 0.95f) {
            return 0xFFD9534A; // nearly full — you can't pick much more up
        }
        if (pct >= 0.75f) {
            return 0xFFD9A441; // getting full
        }
        return 0xFFB0B0B0;     // room to spare
    }

    /** A little triangle: pointing up + orange = warming, down + blue = cooling, nothing = steady. */
    private static void drawTrendArrow(GuiGraphicsExtractor g, int x, int y, int trend) {
        if (trend > 0) {
            int c = 0xFFE0873A; // warming
            g.fill(x + 3, y, x + 5, y + 1, c);
            g.fill(x + 2, y + 1, x + 6, y + 2, c);
            g.fill(x + 1, y + 2, x + 7, y + 3, c);
            g.fill(x, y + 3, x + 8, y + 4, c);
        } else if (trend < 0) {
            int c = 0xFF6FA8D9; // cooling
            g.fill(x, y + 4, x + 8, y + 5, c);
            g.fill(x + 1, y + 5, x + 7, y + 6, c);
            g.fill(x + 2, y + 6, x + 6, y + 7, c);
            g.fill(x + 3, y + 7, x + 5, y + 8, c);
        }
    }

    private static int temperatureColor(float bodyTemp) {
        if (bodyTemp <= -60f) {
            return 0xFF1F5FA8; // severe cold — hypothermia
        }
        if (bodyTemp <= -20f) {
            return 0xFF4A90D9; // cold
        }
        if (bodyTemp >= 60f) {
            return 0xFFA83226; // severe heat — heatstroke
        }
        if (bodyTemp >= 20f) {
            return 0xFFD9534A; // hot
        }
        return 0xFF3AA655;     // comfortable
    }
}
