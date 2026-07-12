package dev.alone.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A tanning rack (proposal §7.3) — the load-and-leave station that turns a green {@link AloneItems#RAW_HIDE}
 * into real {@code minecraft:leather}. Lay a raw hide on it and it consumes a lump of
 * {@link AloneItems#ANIMAL_BRAINS} (the classic brain-tan — "a beast has just enough brains to tan its own
 * hide") to begin working the skin. Then it just takes <b>time</b>: tanning is slow, hands-off, patient
 * work, so the rack grinds for {@link #TAN_TIME} ticks — notably longer than the drying rack's cure, because
 * turning hide into leather really is days of labour. It keeps working while you're away (progress advances
 * by elapsed world-time, capped so a reload doesn't resolve it in one frame). When it's done, the hide has
 * become leather; right-click to collect it. Pull an unfinished hide and you get the raw hide back — but the
 * brains that were worked in are gone, so it's a real waste to be impatient.
 */
public class TanningRackBlockEntity extends BlockEntity {
    /** How long tanning takes. 72000t ≈ 60 real min ≈ 3 in-game days at this pack's 72× day — deliberately
     *  double the drying rack's cure, because tanning a hide into leather is the slower, more patient job. */
    public static final int TAN_TIME = 72000;
    private static final int MAX_STEP = 6000; // cap per-tick catch-up so a long absence isn't instant

    // Persisted AND synced to the client, so the rack can render the hide (or finished leather) sitting on it.
    public static final AttachmentType<ItemStack> TANNING = AttachmentRegistry.create(
        net.minecraft.resources.Identifier.fromNamespaceAndPath("alone", "tanning_rack_hide"),
        builder -> builder.persistent(ItemStack.CODEC)
            .syncWith(ItemStack.OPTIONAL_STREAM_CODEC, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> PROGRESS = AttachmentRegistry.createPersistent(
        net.minecraft.resources.Identifier.fromNamespaceAndPath("alone", "tanning_rack_progress"),
        com.mojang.serialization.Codec.INT);
    public static final AttachmentType<Long> LAST_TICK = AttachmentRegistry.createPersistent(
        net.minecraft.resources.Identifier.fromNamespaceAndPath("alone", "tanning_rack_last_tick"),
        com.mojang.serialization.Codec.LONG);

    public TanningRackBlockEntity(BlockPos pos, BlockState state) {
        super(AloneBlocks.TANNING_RACK_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TanningRackBlockEntity rack) {
        ItemStack hide = rack.getAttachedOrElse(TANNING, ItemStack.EMPTY);
        if (hide.isEmpty() || !hide.is(AloneItems.RAW_HIDE)) {
            return; // nothing tanning (empty, or already finished into leather waiting to be taken)
        }
        int progress = rack.getAttachedOrElse(PROGRESS, 0);

        // Advance by however much world-time actually elapsed (so it tans while the chunk was unloaded),
        // capped so a long absence catches up over a second of ticks rather than resolving in one frame.
        long now = level.getGameTime();
        long last = rack.getAttachedOrElse(LAST_TICK, now);
        int elapsed = (int) Math.max(0L, Math.min(MAX_STEP, now - last));
        rack.setAttached(LAST_TICK, last + elapsed);
        if (elapsed <= 0) {
            return;
        }

        progress += elapsed;
        if (progress >= TAN_TIME) {
            // Worked through: the hide is leather now. Keep the count (a 1:1 tan).
            rack.setAttached(TANNING, new ItemStack(Items.LEATHER, hide.getCount()));
            rack.setAttached(PROGRESS, TAN_TIME);
        } else {
            rack.setAttached(PROGRESS, progress);
            // Occasional feedback that the rack is working — a little rise off the curing skin.
            if (level instanceof ServerLevel server && now % 40L == 0L) {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.16, 0.05, 0.16, 0.0);
            }
        }
        rack.setChanged();
    }

    /**
     * Lay a raw hide on the rack to start tanning — but only if a lump of brains is on hand to work it.
     * Consumes one {@link AloneItems#RAW_HIDE} from the held stack and one {@link AloneItems#ANIMAL_BRAINS}
     * from anywhere in the player's inventory (off hand, other slot — the brain-tan agent).
     */
    public InteractionResult place(Level level, Player player, ItemStack held, InteractionHand hand) {
        if (!getAttachedOrElse(TANNING, ItemStack.EMPTY).isEmpty() || !held.is(AloneItems.RAW_HIDE)) {
            return InteractionResult.PASS;
        }
        if (!player.isCreative() && !hasBrains(player)) {
            // No tanning agent — tell the player why the hide won't take, rather than silently doing nothing.
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "You need animal brains to tan this hide."), true);
            }
            return InteractionResult.SUCCESS; // consumed the click (with feedback), but nothing placed
        }
        if (!level.isClientSide()) {
            setAttached(TANNING, held.copyWithCount(1));
            setAttached(PROGRESS, 0);
            setAttached(LAST_TICK, level.getGameTime());
            setChanged();
            if (!player.isCreative()) {
                held.shrink(1);
                consumeBrains(player);
            }
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    /** Take whatever is on the rack — finished leather if it tanned through, else the raw hide back. */
    public InteractionResult retrieve(Level level, Player player) {
        ItemStack item = getAttachedOrElse(TANNING, ItemStack.EMPTY);
        if (item.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            removeAttached(TANNING);
            removeAttached(PROGRESS);
            removeAttached(LAST_TICK);
            setChanged();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    /** For the block's drop-on-break: whatever is currently on the rack. */
    public ItemStack heldHide() {
        return getAttachedOrElse(TANNING, ItemStack.EMPTY);
    }

    private static boolean hasBrains(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(AloneItems.ANIMAL_BRAINS)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeBrains(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(AloneItems.ANIMAL_BRAINS)) {
                stack.shrink(1);
                return;
            }
        }
    }
}
