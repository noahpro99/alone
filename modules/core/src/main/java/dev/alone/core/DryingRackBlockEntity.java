package dev.alone.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A drying rack (proposal §4.2) — the salt-free way to keep meat. Hang a perishable food on it and it
 * slowly dries into jerky that <b>never spoils</b>: air-drying is slow and stops in the rain, but a
 * <b>lit campfire directly beneath</b> smokes it several times faster (and through wet weather). It's the
 * patient, fuel-or-time answer that complements salting (which spends scarce salt). The preserved mark
 * lives on the food module's {@code alone:preserved} component, looked up from the registry so core needn't
 * depend on the food module.
 */
public class DryingRackBlockEntity extends BlockEntity {
    /** ~1.5 in-game days to air-dry a piece; smoking over a fire is much quicker. */
    public static final int DRY_TIME = 36000;
    public static final int SMOKE_RATE = 4; // a fire below dries it 4x faster (and rain can't stop it)

    private static final TagKey<Item> PERISHABLE = TagKey.create(Registries.ITEM,
        Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    public static final AttachmentType<ItemStack> DRYING = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_food"), ItemStack.CODEC);
    public static final AttachmentType<Integer> PROGRESS = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_progress"), com.mojang.serialization.Codec.INT);

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(AloneBlocks.DRYING_RACK_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        ItemStack food = rack.getAttachedOrElse(DRYING, ItemStack.EMPTY);
        if (food.isEmpty()) {
            return;
        }
        int progress = rack.getAttachedOrElse(PROGRESS, 0);
        if (progress >= DRY_TIME) {
            return; // already dried — waiting to be taken
        }

        BlockState below = level.getBlockState(pos.below());
        boolean smoking = below.is(Blocks.CAMPFIRE) && below.getValue(BlockStateProperties.LIT);
        int rate;
        if (smoking) {
            rate = SMOKE_RATE; // fire dries it fast, rain or shine
            if (level instanceof ServerLevel server && level.getGameTime() % 6L == 0L) {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    2, 0.12, 0.05, 0.12, 0.01);
            }
        } else if (level.isRainingAt(pos.above())) {
            rate = 0; // meat can't air-dry in the rain — put a roof over it, or smoke it
        } else {
            rate = 1; // patient air-drying
        }

        progress += rate;
        if (progress >= DRY_TIME) {
            preserve(food);
        }
        rack.setAttached(PROGRESS, progress);
        rack.setAttached(DRYING, food);
        rack.setChanged();
    }

    /** Marks the food as preserved (via the food module's registered components, looked up by id). */
    private static void preserve(ItemStack food) {
        DataComponentType<Boolean> preserved = componentType("preserved");
        DataComponentType<Long> spoilsAt = componentType("spoils_at");
        if (preserved != null) {
            food.set(preserved, true);
        }
        if (spoilsAt != null) {
            food.remove(spoilsAt); // cancel any running shelf-life
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> DataComponentType<T> componentType(String path) {
        return (DataComponentType<T>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(
            Identifier.fromNamespaceAndPath("alone", path));
    }

    /** Hang a perishable food on the rack (one piece). */
    public InteractionResult place(Level level, Player player, ItemStack held, InteractionHand hand) {
        if (!getAttachedOrElse(DRYING, ItemStack.EMPTY).isEmpty() || !held.is(PERISHABLE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            setAttached(DRYING, held.copyWithCount(1));
            setAttached(PROGRESS, 0);
            setChanged();
            if (!player.isCreative()) {
                held.shrink(1);
            }
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    /** Take whatever is on the rack (dried jerky, or a still-drying piece). */
    public InteractionResult retrieve(Level level, Player player) {
        ItemStack food = getAttachedOrElse(DRYING, ItemStack.EMPTY);
        if (food.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            removeAttached(DRYING);
            removeAttached(PROGRESS);
            setChanged();
            if (!player.getInventory().add(food)) {
                player.drop(food, false);
            }
        }
        player.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    /** For the block's drop-on-break: whatever is currently hung up. */
    public ItemStack heldFood() {
        return getAttachedOrElse(DRYING, ItemStack.EMPTY);
    }
}
