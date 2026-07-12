package dev.alone.core;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
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
 * A drying rack (proposal §4.2, §7.3) — a lashed frame of poles that does the camp's air-curing, and does
 * <b>both</b> jobs a real one does, deciding which by <b>what you load it with</b>:
 * <ul>
 *   <li><b>Drying meat into jerky.</b> Hang a perishable food and it air-dries over time — fast in warm dry
 *       air (a desert or a breezy autumn day), slow-but-safe in the cold (winter freeze-drying), and it
 *       <b>rots the meat instead</b> in warm wet conditions (rain, or a humid jungle/swamp). <b>Smoking beats
 *       the weather</b>, but the fire must sit below with an air gap, or a fire directly beneath burns the
 *       rack down.</li>
 *   <li><b>Tanning a hide into leather.</b> Stretch a green {@link AloneItems#RAW_HIDE} on it — spending a lump
 *       of {@link AloneItems#ANIMAL_BRAINS} (the classic brain-tan, "a beast has just enough brains to tan its
 *       own hide") — and it works into {@code minecraft:leather} over {@link #TAN_TIME}, deliberately far
 *       longer than a meat-cure because tanning really is days of patient labour.</li>
 * </ul>
 * Either way it <b>keeps working while you're away</b> (progress advances by elapsed world-time, capped so a
 * reload doesn't resolve it in one frame). Finished jerky is marked on the food module's {@code alone:preserved}
 * + {@code alone:dried} components (looked up from the registry, so core needn't depend on food).
 */
public class DryingRackBlockEntity extends BlockEntity {
    /** Effective work to dry a piece (at the "normal temperate" rate of 1.0). ~1.5 in-game days that way. */
    public static final int DRY_TIME = 36000;
    /** How long tanning a hide into leather takes. 72000t ≈ 60 real min ≈ 3 in-game days at this pack's 72×
     *  day — deliberately double the meat-cure, because tanning is the slower, more patient job. */
    public static final int TAN_TIME = 72000;
    private static final float SMOKE_RATE = 4.0f;  // smoked over a fire — fast, and weather can't stop it
    private static final float HOT_DRY_RATE = 2.0f; // warm, dry, breezy — quickest natural drying
    private static final float COLD_RATE = 0.4f;    // cold and dry — slow, but safe (freeze-drying)
    private static final int SPOIL_LIMIT = 12000;   // ~half a day of warm+wet exposure and the meat rots
    private static final int MAX_STEP = 6000;       // cap the per-tick catch-up so a reload isn't instant

    private static final TagKey<Item> PERISHABLE = TagKey.create(Registries.ITEM,
        Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    // Persisted AND synced to the client, so the rack can render whatever sits on it — meat, jerky, a stretched
    // hide, or finished leather (visual only).
    public static final AttachmentType<ItemStack> DRYING = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("alone", "drying_rack_food"),
        builder -> builder.persistent(ItemStack.CODEC)
            .syncWith(ItemStack.OPTIONAL_STREAM_CODEC, net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));
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
        ItemStack loaded = rack.getAttachedOrElse(DRYING, ItemStack.EMPTY);
        if (loaded.isEmpty()) {
            return;
        }
        // A green hide on the frame is being tanned; anything else hung on it is meat being dried. (Once tanning
        // finishes the hide has become leather, so it no longer matches here and just waits to be taken.)
        if (loaded.is(AloneItems.RAW_HIDE)) {
            tanTick(level, pos, rack, loaded);
        } else {
            dryTick(level, pos, rack, loaded);
        }
    }

    /** Tanning path: a stretched raw hide works into leather purely on elapsed time — slow, hands-off. */
    private static void tanTick(Level level, BlockPos pos, DryingRackBlockEntity rack, ItemStack hide) {
        int progress = rack.getAttachedOrElse(PROGRESS, 0);
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
            rack.setAttached(DRYING, new ItemStack(Items.LEATHER, hide.getCount()));
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

    /** Drying path: meat air-dries into jerky at a weather-dependent rate (or rots in warm wet). */
    private static void dryTick(Level level, BlockPos pos, DryingRackBlockEntity rack, ItemStack food) {
        int progress = rack.getAttachedOrElse(PROGRESS, 0);
        if (progress >= DRY_TIME) {
            return; // already dried — waiting to be taken
        }

        // Advance by how much world-time actually passed (so it dries while the chunk was unloaded), capped
        // so a long absence catches up over a second of ticks rather than resolving in a single frame.
        long now = level.getGameTime();
        long last = rack.getAttachedOrElse(LAST_TICK, now);
        int elapsed = (int) Math.max(0L, Math.min(MAX_STEP, now - last));
        // Advance only by what we credit this tick — NOT all the way to `now` — so a long absence's backlog
        // isn't thrown away: it catches up MAX_STEP per tick over the next few ticks (matching Spoilage,
        // which drains away-time in full). Setting LAST_TICK = now discarded everything past one MAX_STEP.
        rack.setAttached(LAST_TICK, last + elapsed);
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
        DataComponentType<Long> freshness = componentType("freshness");
        DataComponentType<Long> freshnessSeen = componentType("freshness_seen");
        DataComponentType<Boolean> preserved = componentType("preserved");
        DataComponentType<Boolean> dried = componentType("dried");
        if (freshness != null) {
            food.remove(freshness); // reset the budget so the long preserved shelf is restamped when carried
        }
        if (freshnessSeen != null) {
            food.remove(freshnessSeen);
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

    /**
     * Load the rack. A <b>perishable food</b> hangs up to dry into jerky; a <b>raw hide</b> stretches on to
     * tan into leather (which spends a lump of {@link AloneItems#ANIMAL_BRAINS} from the player's inventory —
     * the brain-tan agent). Anything else, or an already-occupied rack, is refused.
     */
    public InteractionResult place(Level level, Player player, ItemStack held, InteractionHand hand) {
        if (!getAttachedOrElse(DRYING, ItemStack.EMPTY).isEmpty()) {
            return InteractionResult.PASS; // already occupied
        }
        if (held.is(AloneItems.RAW_HIDE)) {
            return placeHide(level, player, held, hand);
        }
        if (held.is(PERISHABLE)) {
            return placeFood(level, player, held, hand);
        }
        return InteractionResult.PASS;
    }

    /** Hang a perishable food on the rack (one piece) to dry. */
    private InteractionResult placeFood(Level level, Player player, ItemStack held, InteractionHand hand) {
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

    /** Stretch a raw hide on the rack to tan — but only with a lump of brains on hand to work it. */
    private InteractionResult placeHide(Level level, Player player, ItemStack held, InteractionHand hand) {
        if (!player.isCreative() && !hasBrains(player)) {
            // No tanning agent — tell the player why the hide won't take, rather than silently doing nothing.
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(
                    "You need animal brains to tan this hide."), true);
            }
            return InteractionResult.SUCCESS; // consumed the click (with feedback), but nothing placed
        }
        if (!level.isClientSide()) {
            setAttached(DRYING, held.copyWithCount(1));
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

    /** Take whatever is on the rack (dried jerky or a still-drying piece; finished leather or a still-green hide). */
    public InteractionResult retrieve(Level level, Player player) {
        ItemStack item = getAttachedOrElse(DRYING, ItemStack.EMPTY);
        if (item.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            removeAttached(DRYING);
            removeAttached(PROGRESS);
            removeAttached(SPOIL);
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
    public ItemStack heldFood() {
        return getAttachedOrElse(DRYING, ItemStack.EMPTY);
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
