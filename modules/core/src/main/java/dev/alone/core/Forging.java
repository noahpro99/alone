package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/**
 * Forge &amp; temper (proposal §8.2 — timed crafting). Metal gear isn't finished in the crafting grid;
 * a grid-crafted metal tool or piece of armour comes out an <b>unforged blank</b> — brittle, barely
 * usable — until you work it hot at an anvil:
 * <ol>
 *   <li><b>Heat</b> — hold the piece in your main hand next to a lit forge (blast furnace, furnace,
 *       smoker, or a campfire/lava) and it glows hotter each tick; step away and it cools.</li>
 *   <li><b>Hammer</b> — right-click an anvil (with a {@code smithing_hammer} in your pack) while it's
 *       hot to land a blow. A piece takes many blows across several heats — real, slow forging.</li>
 *   <li><b>Quality</b> — the finished piece gets a random quality that sets its durability, from a
 *       crude blade to a masterwork. Not happy? Reheat and <b>re-temper</b> it to reroll — but every
 *       rework tires the steel and permanently lowers its ceiling. A (re)forge also repairs it.</li>
 * </ol>
 * Durability is applied live in {@code ItemStackForgeDurabilityMixin}; the tooltip lives in the client.
 */
public final class Forging {
    private Forging() {
    }

    /** Which items must be forged — iron &amp; steel tools and armour (bundled datapack tag). */
    public static final TagKey<Item> FORGEABLE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "forgeable"));

    /** Forge quality 0..1 — its <b>presence</b> means the piece is forged (absent = an unforged blank). */
    public static final DataComponentType<Float> QUALITY = register("forge_quality", Codec.FLOAT, ByteBufCodecs.FLOAT);
    /** Current heat, 0..{@link #HEAT_MAX}; rises by a forge, falls away from one. */
    public static final DataComponentType<Integer> HEAT = register("forge_heat", Codec.INT, ByteBufCodecs.VAR_INT);
    /** Hammer blows landed toward completing the current (re)forge. */
    public static final DataComponentType<Integer> BLOWS = register("forge_blows", Codec.INT, ByteBufCodecs.VAR_INT);
    /** How many times re-tempered — each rework lowers the durability ceiling. */
    public static final DataComponentType<Integer> REFORGE = register("reforge_count", Codec.INT, ByteBufCodecs.VAR_INT);

    private static final int HEAT_MAX = 1000;
    private static final int HEAT_GAIN = 10;      // per tick in a forge → ~5s to fully heat
    private static final int HEAT_DECAY = 4;      // per tick away from heat → ~12s of workable heat
    private static final int HEAT_MIN_WORK = 350; // below this it's too cold to move under the hammer
    private static final int HEAT_PER_BLOW = 200; // each blow bleeds heat → ~5 blows a heat, then reheat
    private static final int BRITTLE_DURABILITY = 20; // an unforged blank shatters fast
    private static final int FORGE_RADIUS = 2;

    public static void init() {
        // Heat the held piece next to a forge; cool it away from one.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                heatTick(player);
            }
        });

        // Right-click an anvil holding a hot forgeable piece → a hammer blow (instead of the rename GUI).
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(level.getBlockState(hit.getBlockPos()).getBlock() instanceof AnvilBlock)) {
                return InteractionResult.PASS;
            }
            ItemStack piece = player.getMainHandItem();
            if (!isForgeable(piece)) {
                return InteractionResult.PASS; // ordinary anvil use
            }
            return hammer(player, level, piece);
        });
    }

    // ------------------------------------------------------------------ durability (read by the mixin)

    public static boolean isForgeable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            return stack.is(FORGEABLE);
        } catch (IllegalStateException tagsNotBoundYet) {
            return false; // during early bootstrap tags aren't ready — treat as non-forgeable
        }
    }

    public static boolean isForged(ItemStack stack) {
        return stack.has(QUALITY);
    }

    /** The piece's real max durability given its forge state — brittle if unforged, quality-scaled if forged. */
    public static int effectiveMaxDamage(ItemStack stack, int baseMaxDamage) {
        if (!isForged(stack)) {
            return Math.min(baseMaxDamage, BRITTLE_DURABILITY);
        }
        float quality = stack.getOrDefault(QUALITY, 0.5f);
        int reforge = stack.getOrDefault(REFORGE, 0);
        float factor = 0.6f + quality * 0.9f;               // crude 0.6× … masterwork 1.5×
        float penalty = (float) Math.pow(0.85, reforge);    // each rework tires the steel
        return Math.max(1, Math.round(baseMaxDamage * factor * penalty));
    }

    // ------------------------------------------------------------------ heating

    private static void heatTick(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (!isForgeable(main)) {
            return;
        }
        int heat = main.getOrDefault(HEAT, 0);
        if (nearForge(player)) {
            if (heat < HEAT_MAX) {
                main.set(HEAT, Math.min(HEAT_MAX, heat + HEAT_GAIN));
            }
            if (player.tickCount % 10 == 0) {
                int pct = heat * 100 / HEAT_MAX;
                actionBar(player, "Heating the metal: " + pct + "%"
                    + (heat >= HEAT_MIN_WORK ? " — ready to hammer" : ""));
            }
        } else if (heat > 0) {
            main.set(HEAT, Math.max(0, heat - HEAT_DECAY));
        }
    }

    private static boolean nearForge(Player player) {
        Level level = player.level();
        BlockPos c = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -FORGE_RADIUS; dx <= FORGE_RADIUS; dx++) {
            for (int dy = -FORGE_RADIUS; dy <= FORGE_RADIUS; dy++) {
                for (int dz = -FORGE_RADIUS; dz <= FORGE_RADIUS; dz++) {
                    cursor.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (isForgeSource(level.getBlockState(cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isForgeSource(BlockState state) {
        var fluid = state.getFluidState().getType();
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return true;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER);
        }
        return false;
    }

    // ------------------------------------------------------------------ hammering

    private static InteractionResult hammer(Player player, Level level, ItemStack piece) {
        if (level.isClientSide()) {
            return InteractionResult.CONSUME; // stop the anvil GUI client-side; the server does the work
        }
        if (!hasHammer(player)) {
            actionBar(player, "You need a smithing hammer to work the metal.");
            return InteractionResult.SUCCESS;
        }
        int heat = piece.getOrDefault(HEAT, 0);
        if (heat < HEAT_MIN_WORK) {
            actionBar(player, "Too cold to work — heat it in a forge first.");
            return InteractionResult.SUCCESS;
        }
        piece.set(HEAT, Math.max(0, heat - HEAT_PER_BLOW));
        damageHammer(player);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4f, 1.4f);
        player.swing(InteractionHand.MAIN_HAND);

        int blows = piece.getOrDefault(BLOWS, 0) + 1;
        int required = blowsRequired(piece);
        if (blows >= required) {
            piece.remove(BLOWS);
            complete(player, piece);
        } else {
            piece.set(BLOWS, blows);
            actionBar(player, "Forging… " + blows + "/" + required
                + "  (heat " + piece.getOrDefault(HEAT, 0) * 100 / HEAT_MAX + "%)");
        }
        return InteractionResult.SUCCESS;
    }

    private static void complete(Player player, ItemStack piece) {
        boolean reforge = isForged(piece);
        int reforgeCount = piece.getOrDefault(REFORGE, 0) + (reforge ? 1 : 0);
        float quality = quality(player.getRandom());
        piece.set(QUALITY, quality);
        piece.set(REFORGE, reforgeCount);
        piece.set(HEAT, 0);
        piece.setDamageValue(0); // a fresh edge — (re)forging repairs it fully

        int durability = piece.getMaxDamage(); // now reflects the new quality via the mixin
        String grade = gradeName(quality);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(reforge
                ? "Reworked into a " + grade + " piece — the steel tires (durability " + durability + ")."
                : "You forge a " + grade + " piece (durability " + durability + ")."));
        }
    }

    private static int blowsRequired(ItemStack piece) {
        var equippable = piece.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            return switch (equippable.slot()) {
                case CHEST -> 20; // a cuirass is the most metal to move
                case LEGS -> 16;
                default -> 12;    // helmet / boots
            };
        }
        return 12; // tools and weapons
    }

    private static boolean hasHammer(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(AloneItems.SMITHING_HAMMER)) {
                return true;
            }
        }
        return false;
    }

    private static void damageHammer(Player player) {
        if (player.isCreative()) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(AloneItems.SMITHING_HAMMER)) {
                int damage = stack.getDamageValue() + 1;
                if (damage >= stack.getMaxDamage()) {
                    inventory.setItem(i, ItemStack.EMPTY); // the hammer finally wears out
                } else {
                    stack.setDamageValue(damage);
                }
                return;
            }
        }
    }

    /** A bell-ish roll — middling forges are common, crude and masterwork rare. */
    private static float quality(RandomSource random) {
        return (random.nextFloat() + random.nextFloat()) / 2f;
    }

    public static String gradeName(float quality) {
        if (quality < 0.2f) {
            return "crude";
        }
        if (quality < 0.4f) {
            return "rough";
        }
        if (quality < 0.6f) {
            return "serviceable";
        }
        if (quality < 0.8f) {
            return "fine";
        }
        return "masterwork";
    }

    private static void actionBar(Player player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(message), true); // true = action-bar overlay
        }
    }

    private static <T> DataComponentType<T> register(String path, Codec<T> codec,
                                                     net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, T> streamCodec) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("alone", path),
            DataComponentType.<T>builder().persistent(codec).networkSynchronized(streamCodec).build());
    }
}
