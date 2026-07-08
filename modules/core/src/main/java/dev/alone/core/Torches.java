package dev.alone.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Torch fuel (proposal §5.6). A lit torch is burning fuel:
 * <ul>
 *   <li><b>Held</b> — it loses a point of fuel each tick; spent, it reverts to a dark burnt-out torch.</li>
 *   <li><b>Placed</b> — it keeps burning: its fuel drains each tick and it gutters out to a spent torch
 *       when empty. Mining it early hands back a torch with the fuel that was left (no free refill).</li>
 * </ul>
 * Lighting an unlit torch carries its remaining fuel across to the lit item.
 */
public final class Torches {
    private Torches() {
    }

    /** Placed lit torches → fuel used (damage) at that block, per level. */
    private static final Map<ServerLevel, Map<BlockPos, Integer>> PLACED = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                burnHeld(player);
            }
            burnPlaced(server); // mounted torches burn down too
        });

        // Mining a tracked torch returns a torch carrying its remaining fuel, not a fresh vanilla one.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
                return;
            }
            Integer used = remove(serverLevel, pos);
            if (used == null || player.isCreative()) {
                return;
            }
            // drop the fuel-carrying torch and remove the plain vanilla torch this break just spawned
            for (ItemEntity ie : serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5))) {
                if (ie.getItem().is(Items.TORCH)) {
                    ie.discard();
                    break;
                }
            }
            ItemStack back = new ItemStack(AloneItems.TORCH_LIT);
            back.setDamageValue(used);
            Block.popResource(serverLevel, pos, back);
        });
    }

    private static void burnHeld(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(AloneItems.TORCH_LIT)) {
                continue;
            }
            int used = stack.getDamageValue() + 1;
            if (used >= stack.getMaxDamage()) {
                ItemStack spent = new ItemStack(AloneItems.TORCH); // burnt out — dark again
                spent.setDamageValue(stack.getMaxDamage() - 1);
                inventory.setItem(i, spent);
            } else {
                stack.setDamageValue(used);
            }
        }
    }

    private static void burnPlaced(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Map<BlockPos, Integer> map = PLACED.get(level);
            if (map == null || map.isEmpty()) {
                continue;
            }
            Iterator<Map.Entry<BlockPos, Integer>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = it.next();
                BlockPos pos = entry.getKey();
                if (!level.isLoaded(pos)) {
                    continue; // frozen while its chunk is unloaded
                }
                if (!(level.getBlockState(pos).getBlock() instanceof TorchBlock)) {
                    it.remove(); // removed by non-player means (piston, water…)
                    continue;
                }
                int used = entry.getValue() + 1;
                if (used >= AloneItems.TORCH_FUEL) {
                    level.levelEvent(1009, pos, 0); // extinguish fizzle
                    level.removeBlock(pos, false);
                    ItemStack spent = new ItemStack(AloneItems.TORCH); // leaves a dark, burnt-out torch
                    spent.setDamageValue(AloneItems.TORCH_FUEL - 1);
                    Block.popResource(level, pos, spent);
                    it.remove();
                } else {
                    entry.setValue(used);
                }
            }
        }
    }

    /** Record a torch just planted at {@code pos}, carrying its used-fuel (damage). */
    public static void registerPlaced(net.minecraft.world.level.Level level, BlockPos pos, int fuelUsed) {
        if (level instanceof ServerLevel serverLevel) {
            PLACED.computeIfAbsent(serverLevel, k -> new HashMap<>()).put(pos.immutable(), fuelUsed);
        }
    }

    private static Integer remove(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Integer> map = PLACED.get(level);
        return map == null ? null : map.remove(pos);
    }

    /** Convert the unlit torch in the given hand into a lit one, carrying its remaining fuel. */
    public static void light(Player player, InteractionHand hand) {
        ItemStack unlit = player.getItemInHand(hand);
        if (!unlit.is(AloneItems.TORCH)) {
            return;
        }
        ItemStack lit = new ItemStack(AloneItems.TORCH_LIT);
        lit.setDamageValue(unlit.getDamageValue());
        player.setItemInHand(hand, lit);
    }

    /** A block you could light a torch from. */
    public static boolean isFireSource(BlockState state) {
        if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return true;
        }
        if (state.getBlock() instanceof TorchBlock) {
            return true;
        }
        return state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT);
    }
}
