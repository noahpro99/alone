package dev.alone.cinematic.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.biome.Biome;

/**
 * Alone: Cinematic — a small, separable atmosphere mod. When you <b>cross into a new biome</b> or a
 * <b>new day breaks</b>, a dramatic swell plays and the biome's name fades in at the bottom-centre of the
 * screen. A <b>long cooldown</b> keeps it from spamming when you hop back and forth across a biome border
 * (a new day always plays — it happens once a day, so it can't spam).
 *
 * <p>Pure client flavour: no server side, no dependency on the survival core. The "music" is a vanilla
 * sound stand-in — swap {@link #SWELL} for a custom track by registering your own {@code SoundEvent}.
 */
public class AloneCinematicClient implements ClientModInitializer {
    private static final long COOLDOWN_TICKS = 2400; // ~2 min real: a border-hop can't re-trigger the swell
    private static final int SHOW_TICKS = 90;        // the name lingers ~4.5 s
    private static final int FADE_TICKS = 18;        // fade in and out over ~0.9 s each end

    private long clientTick = 0;
    private String lastBiome = null;
    private long lastDay = Long.MIN_VALUE;
    private long lastTriggerTick = Long.MIN_VALUE;

    private String shownName = "";
    private long shownStartTick = 0;
    private long shownUntilTick = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("alone-cinematic", "biome_reveal"),
            (graphics, deltaTracker) -> render(graphics));
    }

    private void tick(Minecraft mc) {
        clientTick++;
        if (mc.player == null || mc.level == null) {
            return;
        }
        BlockPos pos = mc.player.blockPosition();
        Holder<Biome> biome = mc.level.getBiome(pos);
        String biomeId = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        long day = mc.level.getGameTime() / 24000L; // day counter — increments once per MC day

        boolean firstSeen = lastBiome == null;
        boolean newBiome = !firstSeen && !biomeId.equals(lastBiome);
        boolean newDay = lastDay != Long.MIN_VALUE && day != lastDay;
        lastBiome = biomeId;
        lastDay = day;
        if (firstSeen) {
            return; // don't fire on the first tick after joining — just seed the baseline
        }

        boolean cooled = clientTick - lastTriggerTick >= COOLDOWN_TICKS;
        // A new day always plays (once a day, never spammy); a biome change only once the cooldown has passed.
        if (newDay || (newBiome && cooled)) {
            trigger(mc, biomeId);
        }
    }

    private void trigger(Minecraft mc, String biomeId) {
        lastTriggerTick = clientTick;
        shownName = prettyBiome(biomeId);
        shownStartTick = clientTick;
        shownUntilTick = clientTick + SHOW_TICKS;
        // A dramatic swell — a vanilla stand-in (a UI sound, non-positional). Swap the SoundEvent for a
        // custom music track by registering your own and referencing it here.
        mc.getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f));
    }

    private void render(GuiGraphicsExtractor g) {
        if (clientTick >= shownUntilTick || shownName.isEmpty()) {
            return;
        }
        long elapsed = clientTick - shownStartTick;
        long remaining = shownUntilTick - clientTick;
        float alpha = 1f;
        if (elapsed < FADE_TICKS) {
            alpha = elapsed / (float) FADE_TICKS;   // fade in
        } else if (remaining < FADE_TICKS) {
            alpha = remaining / (float) FADE_TICKS; // fade out
        }
        int a = Math.max(4, Math.min(255, (int) (alpha * 255)));
        int color = (a << 24) | 0xFFFFFF;
        Font font = Minecraft.getInstance().font;
        int x = g.guiWidth() / 2;
        int y = g.guiHeight() - 62; // bottom-centre, clear of the hotbar
        g.centeredText(font, shownName, x, y, color);
    }

    /** "minecraft:snowy_taiga" → "Snowy Taiga". */
    private static String prettyBiome(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}
