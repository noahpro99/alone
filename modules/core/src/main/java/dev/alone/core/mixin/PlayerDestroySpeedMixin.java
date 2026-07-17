package dev.alone.core.mixin;

import dev.alone.core.AloneCore;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Honest break times (proposal §5.4). Rewrites the player's dig speed:
 * <ul>
 *   <li><b>Logs</b> need the right tool and chop slowly — an axe works, a crude blade barely, a
 *       bare hand not at all.</li>
 *   <li><b>Stone / ore</b> is real quarrying work — tens of seconds, not a fraction (with a
 *       worst-case cap so obsidian-tier blocks stay brutal but finite).</li>
 *   <li><b>Packed earth</b> (dirt/clay) is real excavation — ~3 in-game min a block with a shovel,
 *       near-hopeless by hand — calibrated to the 72x day/night time compression. <b>Loose</b> sand
 *       and gravel still scoop fast, and plants stay vanilla-fast.</li>
 * </ul>
 * Creative instabreak bypasses all of this.
 */
@Mixin(Player.class)
public class PlayerDestroySpeedMixin {
    // A hafted axe fells a log in ~30s (a mature 5–7 log tree is ~3 in-game min ≈ several real hours at
    // the 72x day/night scale — matching a knapped, chipped hand-axe). A crude chopper — a knife or an
    // improvised blade with no proper edge or haft — hacks at ~75s a log, a genuine ordeal by comparison.
    private static final float LOG_AXE_FACTOR = 0.04f;  // a real axe: slow but functional (~30s/log for flint)
    // Cap the tier bonus on FELLING. A keener metal edge does help, but chopping through a trunk is limited by
    // swing effort and the wood itself, not just sharpness — so a steel axe (material speed 7.0) shouldn't fell
    // ~2.8x faster than a hafted flint one (2.5). Clamping the axe's speed input to 4.0 leaves flint untouched
    // and gives the best metal axe only a modest ~1.6x edge (~19s/log vs ~30s), not a trivialising one.
    private static final float LOG_AXE_SPEED_CAP = 4.0f;
    private static final float LOG_CRUDE_SPEED = 0.04f; // a knife/improvised chopper: ~75s a log, a real slog
    private static final float STONE_FACTOR = 0.02f;   // ~50x slower quarrying
    // Calibrated to Minecraft's time compression: a 20-min day stands in for 24 real hours, so real
    // time reads at 72x (1 real hour ≈ 50 in-game seconds). Real excavation of 1 m³ of dirt is ~1–4h
    // WITH a shovel but a grueling 12–24h+ by hand — so bare hands are ~6x slower than a shovel. At 72x:
    // a shovel is ~190s a block (~3.5h), bare hands ~1080s (~18 min ≈ 21h) — genuinely near-hopeless.
    // Loose sand/gravel stays deliberately quick (granular, and foraging flint from gravel must work). (§5.4)
    private static final float DIRT_FACTOR = 0.002f;    // packed earth WITH a shovel — ~190s (1–4h real @72x)
    private static final float CLAY_FACTOR = 0.0016f;   // dense clay subsoil with a shovel — hardest earth (~280s)
    // Loose sand/gravel is granular so it's quicker than packed earth, but a whole cubic metre is still
    // real labour: ~1–1.5h continuous, i.e. ~60–75s a block at 72x. Flint isn't gated behind fully
    // clearing a gravel block, though — it shakes loose part-way through (see ServerPlayerGameModeMixin).
    private static final float LOOSE_FACTOR = 0.006f;   // loose sand/gravel with a shovel — ~60–75s a block
    private static final float NO_SHOVEL_DIG = 0.0007f; // packed earth by hand / wrong tool — ~1080s (~18 min), ~6x a shovel
    private static final float LOOSE_HAND_DIG = 0.008f; // loose sand/gravel by hand — ~90–110s, scoopable but slow
    private static final float LEAVES_HAND_FACTOR = 0.08f;  // tearing foliage by hand is slow, tugging work
    private static final float LEAVES_BLADE_FACTOR = 0.3f;  // an axe/hoe shears through it quicker
    private static final float MAX_BREAK_DIVISOR = 40f; // caps the worst-case time (~60s)
    // De-turfing: clearing the grass off a square metre of ground. Laborious by hand (~26s), quicker with
    // a hoe or shovel (~9s) — the surface job before the real excavation of the dirt beneath (§5.4).
    private static final float GRASS_CLEAR_TOOL = 0.09f; // hoe/shovel — ~9s to clear the turf
    private static final float GRASS_CLEAR_HAND = 0.03f; // by hand — ~26s, pulling grass and roots

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void alone$destroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;

        if (state.is(BlockTags.LOGS)) {
            // if/else, not switch: a switch on an enum makes the compiler emit a synthetic inner class
            // ($SwitchMap), which mixin then has to relocate into Player and can choke on — avoid it.
            AloneCore.LogTool tool = AloneCore.classifyForLog(self.getMainHandItem());
            if (tool == AloneCore.LogTool.AXE) {
                // Clamp the tier speed first so a fine metal edge only modestly out-fells a flint one.
                float axeSpeed = Math.min(cir.getReturnValueF(), LOG_AXE_SPEED_CAP);
                cir.setReturnValue(axeSpeed * LOG_AXE_FACTOR);
            } else if (tool == AloneCore.LogTool.CRUDE) {
                cir.setReturnValue(LOG_CRUDE_SPEED);
            } else {
                cir.setReturnValue(0.0f);
            }
            return;
        }

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            float hardness = state.getDestroySpeed(self.level(), BlockPos.ZERO);
            float slowed = cir.getReturnValueF() * STONE_FACTOR;
            float floor = hardness > 0f ? hardness / MAX_BREAK_DIVISOR : 0f;
            // Skill by doing (§8.4): a practised miner quarries faster — up to ~1.5x at mastery.
            float speed = Math.max(slowed, floor)
                * (1.0f + 0.5f * dev.alone.core.Skills.proficiency(self, dev.alone.core.Skills.MINING));
            cir.setReturnValue(speed);
            return;
        }

        if (state.is(Blocks.GRASS_BLOCK)) {
            // Clearing the turf is its own job, distinct from excavating the dirt beneath: breaking a
            // grass block only strips the grass to bare dirt (see Turf), which you then dig at the packed-
            // earth rate below. De-turfing a square metre is real labour — laborious by hand, quicker with
            // a hoe or shovel — but nowhere near as long as digging out the whole cubic metre of soil.
            boolean tool = self.getMainHandItem().is(ItemTags.HOES) || self.getMainHandItem().is(ItemTags.SHOVELS);
            cir.setReturnValue(tool ? GRASS_CLEAR_TOOL : GRASS_CLEAR_HAND);
            return;
        }

        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            // "Loose" = genuinely granular material that scoops fast. Everything else in the shovel tag is
            // packed soil (grass, dirt, podzol, mycelium, path, farmland, mud…) and digs slow. We can't use
            // #minecraft:dirt to mean "soil": in 26.2 that tag is tiny (dirt/coarse/rooted only) — grass_block
            // and the rest are NOT in it — so relying on it made grass fall through to the loose path.
            boolean loose = state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL);
            boolean clay = state.is(Blocks.CLAY); // dense subsoil — the hardest earth, its own factor
            boolean hasShovel = self.getMainHandItem().is(ItemTags.SHOVELS);
            float speed = hasShovel
                ? cir.getReturnValueF() * (clay ? CLAY_FACTOR : (loose ? LOOSE_FACTOR : DIRT_FACTOR))
                : (loose ? LOOSE_HAND_DIG : NO_SHOVEL_DIG);
            // No shovel gives a FIXED speed (not scaled by the held item), so a pot or an axe is never
            // faster than bare hands; packed earth barely yields, so you can't claw a shelter out of the
            // ground without the right tool. With a shovel it's still heavy work — dirt slow, dense clay
            // subsoil slowest, loose sand/gravel quick (§5.4).
            cir.setReturnValue(speed);
            return;
        }

        if (state.is(BlockTags.LEAVES)) {
            // Tearing through dense foliage is slow, tugging work; a blade (axe/hoe) shears it quicker.
            ItemStack hand = self.getMainHandItem();
            float factor = (hand.is(ItemTags.AXES) || hand.is(ItemTags.HOES))
                ? LEAVES_BLADE_FACTOR : LEAVES_HAND_FACTOR;
            cir.setReturnValue(cir.getReturnValueF() * factor);
            return;
        }
        // Everything else (plants, glass, wool, …) stays vanilla-fast.
    }
}
