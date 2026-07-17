package dev.alone.core.client.mixin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client half of saved mining progress (proposal §5.4): a long dig you step away from isn't lost.
 * We stash the crack progress when you abandon a block and restore it when you come back, so the
 * animation resumes where it left off (the server winds its own timer back to match).
 *
 * <p>We also leave a <b>persistent chipped-crack indicator</b> on any block with saved progress while
 * you're away, so you can see at a glance what you'd started. It's drawn under a synthetic per-block
 * breaker id (negative, never colliding with real entities), refreshed each tick (the crack map
 * auto-expires entries after 400 ticks), and cleared the moment the block is resumed, broken, or gone.
 *
 * <p>Maps are lazily created — Mixin does <b>not</b> reliably run a {@code @Unique} instance-field
 * initialiser into the target constructor, so a {@code = new HashMap<>()} initialiser leaves the field
 * null and every access NPEs. The accessors build them on first use instead.
 *
 * <p>We also stop a <b>hotbar slot change</b> from wiping the crack. Vanilla's {@code sameDestroyTarget}
 * demands the <em>same held item</em>, so swapping tools (or a stray scroll) mid-dig counted as a new
 * target and zeroed your progress. We drop that item check — a dig is the same dig as long as it's the
 * same <em>block</em> — so switching slots keeps the crack (mining speed still tracks whatever tool is now
 * in hand, and no ABORT is sent so the server holds its timer to match).
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow private float destroyProgress;
    @Shadow private BlockPos destroyBlockPos;
    @Shadow private boolean isDestroying;
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    public int getDestroyStage() {
        throw new AssertionError();
    }

    @Unique
    private Map<BlockPos, Float> alone$saved;

    @Unique
    private Map<BlockPos, Float> alone$saved() {
        if (this.alone$saved == null) {
            this.alone$saved = new HashMap<>();
        }
        return this.alone$saved;
    }

    /** Same block = same dig, whatever tool is now in hand — so a hotbar slot change (deliberate or a stray
     *  scroll) no longer restarts the break and zeroes the crack. We OVERRIDE the whole check at HEAD rather
     *  than redirect its item-equality call: Fabric Item API already redirects that same {@code
     *  isSameItemSameComponents} instruction (its continue-block-breaking hook), and a second redirector on it
     *  crashes the class transform. A head inject leaves the instruction in place (Fabric's redirect still
     *  applies) and just supersedes the result — we care only that it's the same block position. */
    @Inject(method = "sameDestroyTarget", at = @At("HEAD"), cancellable = true)
    private void alone$sameBlockIsSameDig(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(pos.equals(this.destroyBlockPos));
    }

    /** Releasing mid-dig — stash how far along we were and drop a persistent crack marker. */
    @Inject(method = "stopDestroyBlock", at = @At("HEAD"))
    private void alone$saveOnStop(CallbackInfo ci) {
        alone$stash();
    }

    /** Looking at a different block without releasing also abandons the current one — stash it too. */
    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void alone$saveOnSwitch(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDestroying && !this.destroyBlockPos.equals(pos)) {
            alone$stash();
        }
    }

    /** startPrediction runs synchronously, so by TAIL the fresh state (progress=0) is already set. */
    @Inject(method = "startDestroyBlock", at = @At("TAIL"))
    private void alone$resumeOnStart(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        alone$resume(pos);
    }

    /** Fallback every mining tick, in case the start path didn't catch it. */
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void alone$resumeOnContinue(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        alone$resume(pos);
    }

    /** A block that actually broke can't carry stale progress if something is later placed there. */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void alone$clearOnBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        alone$forget(pos);
    }

    /** Keep the idle crack markers alive and honest — refresh them, and drop any whose block is gone. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void alone$maintainMarkers(CallbackInfo ci) {
        if (this.minecraft.level == null || this.minecraft.player == null || alone$saved().isEmpty()) {
            return;
        }
        int me = this.minecraft.player.getId();
        Iterator<Map.Entry<BlockPos, Float>> it = alone$saved().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Float> e = it.next();
            BlockPos pos = e.getKey();
            if (this.isDestroying && this.destroyBlockPos.equals(pos)) {
                continue; // being actively mined — the real crack (our id) covers it
            }
            if (!this.minecraft.level.isLoaded(pos)) {
                continue; // chunk not loaded — keep the record, just can't draw it yet
            }
            if (this.minecraft.level.getBlockState(pos).isAir()) {
                this.minecraft.level.destroyBlockProgress(alone$markerId(pos), pos, -1);
                it.remove(); // block's gone — no ghost crack
                continue;
            }
            this.minecraft.level.destroyBlockProgress(alone$markerId(pos), pos, alone$stage(e.getValue()));
        }
    }

    /** Below this, a dig is "barely scratched" — let it heal back to a pristine block instead of saving. */
    @Unique
    private static final float alone$MIN_SAVE = 0.10F;

    @Unique
    private void alone$stash() {
        if (this.isDestroying && this.destroyProgress >= alone$MIN_SAVE && this.destroyProgress < 1.0F) {
            BlockPos pos = this.destroyBlockPos.immutable();
            alone$saved().put(pos, this.destroyProgress);
            if (this.minecraft.level != null && this.minecraft.player != null) {
                this.minecraft.level.destroyBlockProgress(alone$markerId(pos), pos, alone$stage(this.destroyProgress));
            }
        }
    }

    @Unique
    private void alone$resume(BlockPos pos) {
        if (!this.isDestroying || !this.destroyBlockPos.equals(pos)) {
            return;
        }
        Float saved = alone$saved().get(pos);
        if (saved != null && this.destroyProgress < saved) {
            this.destroyProgress = saved;
            alone$forget(pos); // clears the idle marker; the live mining crack takes over
            if (this.minecraft.level != null && this.minecraft.player != null) {
                this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), pos, this.getDestroyStage());
            }
        }
    }

    /** Drop a saved dig and remove its idle crack marker. */
    @Unique
    private void alone$forget(BlockPos pos) {
        if (alone$saved().remove(pos) != null && this.minecraft.level != null) {
            this.minecraft.level.destroyBlockProgress(alone$markerId(pos), pos, -1);
        }
    }

    /** A stable negative breaker id per position — never collides with real (non-negative) entity ids. */
    @Unique
    private int alone$markerId(BlockPos pos) {
        return -2 - Math.floorMod(pos.hashCode(), 1_000_000);
    }

    /** Crack stage 0..9 for a fraction of progress (destroyBlockProgress treats 10+ as "remove"). */
    @Unique
    private int alone$stage(float progress) {
        return Math.max(0, Math.min(9, (int) (progress * 10.0F)));
    }
}
