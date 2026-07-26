package dev.alone.core;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A log set up to be hollowed into a dugout canoe (§ boats) — the oldest boat there is, and a real build,
 * not a plank craft. You make a canoe the way it was always made: <b>char the inside with a live flame</b>
 * (right-click with a lit torch or a carried ember), then <b>scrape the burnt wood out with an axe</b>
 * (right-click with any axe) — each scrape is tiring work. Six char-and-scrape cycles hollow the log
 * through, and it becomes a finished canoe (a boat you set on the water). Brutal and slow, as it should be.
 */
public class DugoutBlock extends BaseEntityBlock {
    public static final MapCodec<DugoutBlock> CODEC = simpleCodec(DugoutBlock::new);

    private static final float STAMINA_PER_SCRAPE = 8f;

    public DugoutBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DugoutBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DugoutBlockEntity dugout)) {
            return InteractionResult.PASS;
        }
        boolean flame = stack.is(AloneItems.TORCH_LIT) || stack.is(AloneItems.EMBER);
        boolean axe = stack.is(ItemTags.AXES);
        if (!flame && !axe) {
            return InteractionResult.PASS; // nothing to work it with — let the held item do its own thing
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player; // past the client guard, this is always server-side
        boolean charred = dugout.getAttachedOrElse(DugoutBlockEntity.CHARRED, false);
        int stage = dugout.getAttachedOrElse(DugoutBlockEntity.STAGE, 0);

        // Char the inside with a live flame — the burn that softens the wood for scraping.
        if (flame) {
            if (charred) {
                sp.sendSystemMessage(Component.literal(
                    "It's already charred — scrape the burnt wood out with an axe first."), true);
                return InteractionResult.SUCCESS;
            }
            dugout.setAttached(DugoutBlockEntity.CHARRED, true);
            dugout.setChanged();
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.9f, 1.0f);
            sp.sendSystemMessage(Component.literal(
                "You char the inside of the log — now scrape the burnt wood out with an axe."), true);
            return InteractionResult.SUCCESS;
        }

        // Scrape the char out with an axe — the tiring half of the cycle.
        if (!charred) {
            sp.sendSystemMessage(Component.literal(
                "The wood's too hard to cut away — char the inside with fire first, then scrape."), true);
            return InteractionResult.SUCCESS;
        }
        if (SurvivalMeters.getStamina(player) <= 0f) {
            sp.sendSystemMessage(Component.literal("You're too spent to keep scraping — rest first."), true);
            return InteractionResult.SUCCESS;
        }
        SurvivalMeters.exert(player, STAMINA_PER_SCRAPE);
        level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.7f, 0.9f);
        if (!player.isCreative() && stack.isDamageableItem()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.7f, 1f);
            }
        }
        dugout.setAttached(DugoutBlockEntity.CHARRED, false);
        stage++;
        if (stage < DugoutBlockEntity.STAGES_TO_FINISH) {
            dugout.setAttached(DugoutBlockEntity.STAGE, stage);
            dugout.setChanged();
            sp.sendSystemMessage(Component.literal("You scrape out the charred wood… "
                + (stage * 100 / DugoutBlockEntity.STAGES_TO_FINISH) + "%"), true);
            return InteractionResult.SUCCESS;
        }

        // Hollowed through — it's a canoe now. Reuse the vanilla boat as the finished craft (the hard part
        // was the build); resolve it by id since 26.2 no longer exposes boat items as Items.* fields.
        Item boat = BuiltInRegistries.ITEM
            .getOptional(Identifier.fromNamespaceAndPath("minecraft", "oak_boat")).orElse(null);
        level.destroyBlock(pos, false);
        if (boat != null) {
            Block.popResource(level, pos, new ItemStack(boat));
        }
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.9f);
        sp.sendSystemMessage(Component.literal(
            "The log is hollowed through — a finished dugout canoe. Set it on the water."), true);
        return InteractionResult.SUCCESS;
    }
}
