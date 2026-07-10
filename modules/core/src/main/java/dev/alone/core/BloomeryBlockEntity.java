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
 * A bloomery mid-smelt (proposal §3.2/§8.2). It takes a load of iron and burns <b>charcoal</b> (a wood
 * fire isn't hot enough) to work it into a spongy {@link AloneItems#IRON_BLOOM iron bloom} over a long,
 * fuel-hungry burn — ~3 minutes and a couple of charcoal per bloom, which you then hammer into an ingot.
 * State (loaded iron, banked charcoal, progress) lives on the block entity as attachments. Interactions
 * live in {@link BloomeryBlock}.
 */
public class BloomeryBlockEntity extends BlockEntity {
    /** A bloomery smelt is hours of work IRL; compressed to ~3 minutes of hot burning per bloom. */
    public static final int SMELT_TIME = 3600;
    public static final int FUEL_PER_CHARCOAL = 1600; // hot fuel — a bloom takes a couple of charcoal

    public static final AttachmentType<ItemStack> LOADED = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "bloomery_loaded"), ItemStack.CODEC);
    public static final AttachmentType<Integer> FUEL = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "bloomery_fuel"), Codec.INT);
    public static final AttachmentType<Integer> PROGRESS = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "bloomery_progress"), Codec.INT);

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(AloneBlocks.BLOOMERY_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity bloomery) {
        ItemStack loaded = bloomery.getAttachedOrElse(LOADED, ItemStack.EMPTY);
        int fuel = bloomery.getAttachedOrElse(FUEL, 0);
        if (loaded.isEmpty() || fuel <= 0 || smeltResult(loaded.getItem()) == null) {
            return; // nothing to smelt, or the fire's gone cold
        }
        bloomery.setAttached(FUEL, fuel - 1);
        int progress = bloomery.getAttachedOrElse(PROGRESS, 0) + 1;
        if (progress >= SMELT_TIME) {
            ItemStack bloom = smeltResult(loaded.getItem());
            bloom.setCount(loaded.getCount());
            bloomery.setAttached(LOADED, bloom);
            bloomery.setAttached(PROGRESS, 0);
        } else {
            bloomery.setAttached(PROGRESS, progress);
        }
        bloomery.setChanged();
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 5L == 0L) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                2, 0.12, 0.05, 0.12, 0.01);
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.35, pos.getZ() + 0.5,
                1, 0.12, 0.02, 0.12, 0.005);
        }
    }

    /** Iron smelts into a bloom; nothing else smelts here. Null means it can't be smelted in a bloomery. */
    public static ItemStack smeltResult(Item item) {
        if (item == Items.RAW_IRON) {
            return new ItemStack(AloneItems.IRON_BLOOM);
        }
        return null;
    }

    /** A bloomery burns charcoal (or coal) — a plain wood fire never reaches the heat to smelt iron. */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.CHARCOAL) || stack.is(Items.COAL)) {
            return FUEL_PER_CHARCOAL;
        }
        return 0;
    }
}
