package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A kiln in the middle of firing (proposal §3.2). It holds a load of unfired ware, banks fuel, and works
 * it slowly — <b>~2 minutes of steady, fuel-hungry burning per batch</b>. Data lives on the block entity
 * as attachments (like the campfire's fuel), so it persists without hand-rolled NBT. The block's
 * interactions (load, fuel, take) live in {@link KilnBlock}.
 */
public class KilnBlockEntity extends BlockEntity {
    /** Real firing is slow — a full firing is a couple of hours; compressed to ~2 minutes of burn. */
    public static final int FIRE_TIME = 2400;

    public static final AttachmentType<ItemStack> LOADED = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "kiln_loaded"), ItemStack.CODEC);
    public static final AttachmentType<Integer> FUEL = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "kiln_fuel"), Codec.INT);
    public static final AttachmentType<Integer> PROGRESS = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "kiln_progress"), Codec.INT);
    /** Effective fire time for the current load — baked in at load from the potter's skill (§8.4). */
    public static final AttachmentType<Integer> FIRE_TARGET = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "kiln_fire_target"), Codec.INT);

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(AloneBlocks.KILN_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KilnBlockEntity kiln) {
        ItemStack loaded = kiln.getAttachedOrElse(LOADED, ItemStack.EMPTY);
        int fuel = kiln.getAttachedOrElse(FUEL, 0);
        if (loaded.isEmpty() || fuel <= 0 || firedResult(loaded.getItem()) == null) {
            return; // nothing to fire, or the fire's gone out
        }
        kiln.setAttached(FUEL, fuel - 1); // the fire eats its fuel whether or not the ware is done
        int progress = kiln.getAttachedOrElse(PROGRESS, 0) + 1;
        if (progress >= kiln.getAttachedOrElse(FIRE_TARGET, FIRE_TIME)) { // skill fires faster (baked at load)
            ItemStack fired = firedResult(loaded.getItem());
            fired.setCount(loaded.getCount()); // the whole batch hardens together
            kiln.setAttached(LOADED, fired);
            kiln.setAttached(PROGRESS, 0);
        } else {
            kiln.setAttached(PROGRESS, progress);
        }
        kiln.setChanged();
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 6L == 0L) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                3, 0.15, 0.05, 0.15, 0.01);
        }
    }

    /** What a loaded item fires into — clay hardens to a pot or a brick. Null means it can't be fired. */
    public static ItemStack firedResult(Item item) {
        if (item == AloneItems.UNFIRED_CLAY_POT) {
            return new ItemStack(AloneItems.CLAY_POT);
        }
        if (item == Items.CLAY_BALL) {
            return new ItemStack(Items.BRICK);
        }
        return null;
    }
}
