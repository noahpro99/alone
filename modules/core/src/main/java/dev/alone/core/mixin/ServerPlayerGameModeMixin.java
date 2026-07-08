package dev.alone.core.mixin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    /** Abandoning a dig — remember the ticks of progress accrued so far. */
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void alone$saveOnAbort(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                   Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (action != ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK || !this.isDestroyingBlock) {
            return;
        }
        if (this.level.getBlockState(this.destroyPos).isAir()) {
            return;
        }
        int ticks = this.gameTicks - this.destroyProgressStart;
        if (ticks > 1) {
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
}
