package dev.alone.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A drying rack (proposal §4.2) — the salt-free way to keep meat, done as honestly as the rest of the pack:
 * <ul>
 *   <li><b>Air-drying depends on the weather</b>: fast in warm dry air (a desert or a breezy autumn day),
 *       slow-but-safe in the cold (winter freeze-drying), and it <b>rots the meat instead</b> in warm wet
 *       conditions — rain on it, or a humid jungle/swamp — where you can't out-dry the bacteria.</li>
 *   <li><b>Smoking beats the weather</b>, but the fire must be <b>set below with a gap</b> so cool smoke
 *       rises to the rack; a fire <b>directly beneath</b> puts the wooden rack in the flames and burns it.</li>
 *   <li>It <b>keeps drying while you're away</b> — progress advances by elapsed world-time, not just ticks.</li>
 * </ul>
 * Finished jerky is marked on the food module's {@code alone:preserved} + {@code alone:dried} components
 * (looked up from the registry, so core needn't depend on food): a long — but not infinite — shelf life,
 * and lighter to carry (the water's gone).
 */
public class DryingRackBlockEntity extends BlockEntity {
    /** Effective work to dry a piece (at the "normal temperate" rate of 1.0). ~1.5 in-game days that way. */
    public static final int DRY_TIME = 36000;
    private static final float SMOKE_RATE = 4.0f;  // smoked over a fire — fast, and weather can't stop it
    private static final float HOT_DRY_RATE = 2.0f; // warm, dry, breezy — quickest natural drying
    private static final float COLD_RATE = 0.4f;    // cold and dry — slow, but safe (freeze-drying)
    private static final int SPOIL_LIMIT = 12000;   // ~half a day of warm+wet exposure and the meat rots
    private static final int MAX_STEP = 6000;       // cap the per-tick catch-up so a reload isn't instant

    private static final TagKey<Item> PERISHABLE = TagKey.create(Registries.ITEM,
        Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    public static final AttachmentType<ItemStack> DRYING = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_food"), ItemStack.CODEC);
    public static final AttachmentType<Integer> PROGRESS = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_progress"), com.mojang.serialization.Codec.INT);
    public static final AttachmentType<Integer> SPOIL = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_spoil"), com.mojang.serialization.Codec.INT);
    public static final AttachmentType<Long> LAST_TICK = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_last_tick"), com.mojang.serialization.Codec.LONG);

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

        // Advance by how much world-time actually passed (so it dries while the chunk was unloaded), capped
        // so a long absence catches up over a second of ticks rather than resolving in a single frame.
        long now = level.getGameTime();
        int elapsed = (int) Math.max(0L, Math.min(MAX_STEP, now - rack.getAttachedOrElse(LAST_TICK, now)));
        rack.setAttached(LAST_TICK, now);
        if (elapsed <= 0) {
            return;
        }

        // A fire directly beneath a wooden rack burns it (and the meat with it) — don't dry over open flame.
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.FIRE) || below.is(Blocks.LAVA)
            || (below.is(Blocks.CAMPFIRE) && below.getValue(BlockStateProperties.LIT))) {
            burnDown(level, pos);
            return;
        }

        // Smoke rises to the rack when a lit campfire sits two blocks down with an air gap between.
        boolean smoking = below.isAir() && level.getBlockState(pos.below(2)).is(Blocks.CAMPFIRE)
            && level.getBlockState(pos.below(2)).getValue(BlockStateProperties.LIT);

        float rate;
        int spoilStep = 0;
        if (smoking) {
            rate = SMOKE_RATE;
            if (level instanceof ServerLevel server && now % 6L == 0L) {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    2, 0.16, 0.05, 0.16, 0.01);
            }
        } else {
            Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(pos);
            float temp = biome.value().getBaseTemperature();
            boolean cold = Seasons.isWinter(level) || temp < 0.15f;
            boolean warm = temp > 0.6f && !cold;
            boolean humidBiome = biome.is(BiomeTags.IS_JUNGLE)
                || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
            boolean raining = level.isRainingAt(pos.above());
            if ((raining || (humidBiome && warm)) && !cold) {
                rate = 0f;
                spoilStep = elapsed; // warm + wet/humid: bacteria win — it rots rather than dries
            } else if (raining) {
                rate = 0f; // cold rain/snow — it just pauses (frozen), no rot
            } else if (cold) {
                rate = COLD_RATE; // cold and dry — slow but safe
            } else if (warm) {
                rate = HOT_DRY_RATE; // warm, dry air — quickest
            } else {
                rate = 1.0f; // temperate and dry — normal
            }
        }

        if (spoilStep > 0) {
            int spoil = rack.getAttachedOrElse(SPOIL, 0) + spoilStep;
            if (spoil >= SPOIL_LIMIT) {
                rack.setAttached(DRYING, new ItemStack(Items.ROTTEN_FLESH, food.getCount())); // it rotted
                rack.setAttached(SPOIL, 0);
                rack.setAttached(PROGRESS, 0);
                rack.setChanged();
                return;
            }
            rack.setAttached(SPOIL, spoil);
        }
        if (rate > 0f) {
            progress += Math.round(elapsed * rate);
            if (progress >= DRY_TIME) {
                progress = DRY_TIME;
                preserve(food); // dried into jerky
            }
            rack.setAttached(PROGRESS, progress);
            rack.setAttached(DRYING, food);
        }
        rack.setChanged();
    }

    /** The wooden rack (and the meat) caught fire — remove it, scatter a little flame. */
    private static void burnDown(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                10, 0.2, 0.2, 0.2, 0.02);
        }
        level.destroyBlock(pos, false); // burned up — rack and food lost
    }

    /** Mark the food as dried jerky: a long finite shelf life ({@code preserved}) and lighter ({@code dried}). */
    private static void preserve(ItemStack food) {
        DataComponentType<Long> spoilsAt = componentType("spoils_at");
        DataComponentType<Boolean> preserved = componentType("preserved");
        DataComponentType<Boolean> dried = componentType("dried");
        if (spoilsAt != null) {
            food.remove(spoilsAt); // drop the fresh-meat timer so the long preserved shelf is restamped
        }
        if (preserved != null) {
            food.set(preserved, true);
        }
        if (dried != null) {
            food.set(dried, true);
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
            setAttached(SPOIL, 0);
            setAttached(LAST_TICK, level.getGameTime());
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
            removeAttached(SPOIL);
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
