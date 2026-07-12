package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Local game populations &amp; overhunting (proposal §7.2). Wild game isn't infinite: hunt one patch of
 * country too hard and you <b>thin it out</b> — the animals grow scarce there and stop spawning, until the
 * population breeds back over the following days, or you move to fresh ground. It's the hunting twin of the
 * finite {@link FishStock fish stocks}, and the same lesson a survivor learns: work a range, don't strip
 * one valley bare. Stored as a per-chunk stock (0..{@link #FULL}) on a chunk attachment, recovered lazily
 * from the last time the ground was hunted, so there's no periodic bookkeeping. Predators aren't game.
 */
public final class GameStock {
    private GameStock() {
    }

    public static final int FULL = 100;
    private static final int KILL_COST = 12;                 // each head taken — ~8 from a patch before it thins
    private static final int RECOVERY_TICKS_PER_POINT = 900; // a few in-game days for a hunted-out range to return
    private static final float MAX_SUPPRESS = 0.9f;          // even a stripped patch spawns the odd straggler

    public static final AttachmentType<Integer> STOCK = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "game_stock"), Codec.INT);
    public static final AttachmentType<Long> LAST_HUNTED = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "game_last_hunted"), Codec.LONG);

    public static void init() {
        // A player's kill draws the local game down; natural deaths and predator kills don't count as hunting.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel level && isGame(entity)
                && source.getEntity() instanceof Player) {
                deplete(level, entity.blockPosition());
            }
        });
    }

    /** Wild game you hunt for food — passive animals, but not the bold predators (wolves, bears). */
    public static boolean isGame(Entity entity) {
        return entity instanceof Animal && !Scent.PREDATORS.contains(entity.getType());
    }

    /** May a game animal spawn naturally here, given how hard this ground's been hunted? */
    public static boolean spawnAllowed(ServerLevel level, BlockPos pos, RandomSource random) {
        float suppress = depletion(level, pos) * MAX_SUPPRESS;
        return suppress <= 0f || random.nextFloat() >= suppress;
    }

    private static float depletion(ServerLevel level, BlockPos pos) {
        return 1f - current(level, pos) / (float) FULL;
    }

    /** Current stock, recovered on the fly from the last-hunted time (not written back — reads stay cheap). */
    private static int current(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        long now = level.getGameTime();
        long last = chunk.getAttachedOrElse(LAST_HUNTED, now);
        int stock = chunk.getAttachedOrElse(STOCK, FULL);
        return Math.min(FULL, stock + (int) (Math.max(0L, now - last) / RECOVERY_TICKS_PER_POINT));
    }

    private static void deplete(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        chunk.setAttached(STOCK, Math.max(0, current(level, pos) - KILL_COST));
        chunk.setAttached(LAST_HUNTED, level.getGameTime());
    }
}
