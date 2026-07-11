package dev.alone.core.mixin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server half of saved mining progress (proposal §5.4). When you abandon a dig, we remember how many
 * ticks of progress you'd built up; when you resume the same block, we wind {@code destroyProgressStart}
 * back by that much, so the server's own break check ({@code getDestroyProgress * ticksSpent}) reaches
 * the threshold at the resumed point — matching the client's restored crack. Fresh digs are untouched.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Shadow private int destroyProgressStart;
    @Shadow private BlockPos destroyPos;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private int gameTicks;

    // Lazily built — Mixin doesn't reliably run a @Unique instance-field initialiser into the
    // target constructor, so a field initialiser would leave this null and NPE on first use.
    @Unique
    private Map<BlockPos, Integer> alone$savedTicks;

    @Unique
    private Map<BlockPos, Integer> alone$savedTicks() {
        if (this.alone$savedTicks == null) {
            this.alone$savedTicks = new HashMap<>();
        }
        return this.alone$savedTicks;
    }

    // You don't excavate a whole cubic metre of gravel to get flint — you sift it, and nodules shake loose
    // BIT BY BIT as you work through it. So flint turns up at a series of stages into the dig, not all at
    // once and not only when the block finally breaks. Tracked per block (packed position → how many stages
    // already checked) so partial digs still pay out, but you can't re-scrape one block forever (§1.3).
    @Unique private static final float[] FLINT_STAGES = {0.20f, 0.40f, 0.60f, 0.80f};
    @Unique private static final float FLINT_CHANCE = 0.40f; // odds each stage of sifting turns up a nodule
    @Unique private Map<Long, Integer> alone$sifted;

    @Unique
    private Map<Long, Integer> alone$sifted() {
        if (this.alone$sifted == null) {
            this.alone$sifted = new HashMap<>();
        }
        return this.alone$sifted;
    }

    /** Abandoning a dig — remember the ticks of progress accrued so far. */
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void alone$saveOnAbort(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                   Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (action != ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK || !this.isDestroyingBlock) {
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = this.level.getBlockState(this.destroyPos);
        if (state.isAir()) {
            return;
        }
        int ticks = this.gameTicks - this.destroyProgressStart;
        // Only remember a dig that's actually underway (≥10%). A barely-scratched block heals back to
        // pristine, matching the client, so a stray click doesn't leave a permanent chip.
        float fraction = state.getDestroyProgress(this.player, this.level, this.destroyPos) * ticks;
        if (ticks > 1 && fraction >= 0.10f) {
            this.alone$savedTicks().put(this.destroyPos.immutable(), ticks);
        }
    }

    /**
     * The START branch does {@code destroyProgressStart = gameTicks}. If we saved progress for this
     * block, subtract it so mining resumes rather than restarting. Only the START path writes this
     * field, so this redirect fires exactly there.
     */
    @Redirect(method = "handleBlockBreakAction", at = @At(value = "FIELD",
        target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyProgressStart:I", opcode = Opcodes.PUTFIELD))
    private void alone$resumeStart(ServerPlayerGameMode self, int value, BlockPos pos,
                                   ServerboundPlayerActionPacket.Action action, Direction direction,
                                   int maxY, int sequence) {
        Integer saved = this.alone$savedTicks().remove(pos);
        this.destroyProgressStart = saved != null ? value - saved : value;
    }

    /**
     * Flint foraging (proposal §1.3). {@code incrementDestroyProgress} returns how far into the current dig
     * the block is, every tick. On gravel, flint shakes loose bit by bit as you work through it: at each
     * newly-crossed {@link #FLINT_STAGES stage} a nodule may turn up (a chance, not a certainty). Tracked
     * per block by how many stages have already been checked, so a partial dig keeps what it already sifted
     * out but one gravel block can't be re-scraped for endless flint.
     */
    @Inject(method = "incrementDestroyProgress", at = @At("RETURN"))
    private void alone$siftFlint(BlockState state, BlockPos pos, int startTick,
                                 CallbackInfoReturnable<Float> cir) {
        if (!state.is(Blocks.GRAVEL)) {
            return;
        }
        float progress = cir.getReturnValueF();
        long key = pos.asLong();
        int checked = this.alone$sifted().getOrDefault(key, 0);
        int before = checked;
        while (checked < FLINT_STAGES.length && progress >= FLINT_STAGES[checked]) {
            if (this.player.getRandom().nextFloat() < FLINT_CHANCE) {
                Block.popResource(this.level, pos, new ItemStack(Items.FLINT)); // another nodule shakes loose
            }
            checked++;
        }
        if (checked != before) {
            this.alone$sifted().put(key, checked);
        }
    }

    /** Once a block is actually removed, forget it was sifted so its spot can be foraged again later. */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void alone$forgetSifted(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.alone$sifted != null) {
            this.alone$sifted.remove(pos.asLong());
        }
    }
}
