package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A loose rock (§8.1) — your day-one stone. It does three jobs, and this class only adds the third:
 * <ul>
 *   <li><b>Hammerstone</b> for knapping flint (hold both, right-click — handled client-side by the use
 *       mixin, which cancels the plain use, so throwing never fires while you're knapping).</li>
 *   <li><b>Hot-rock boiling</b> — sneak-right-click a lit campfire to heat it into a {@link
 *       AloneItems#HOT_ROCK} ({@link HotRocks}, a block/use callback that consumes the click first).</li>
 *   <li><b>Throwing</b> (this class): a plain right-click flings a {@link ThrownRock} — the crudest
 *       ranged weapon, the tier below the {@link SlingshotItem slingshot}. Weaker and shorter than a
 *       slung stone, but free.</li>
 * </ul>
 *
 * <p>The three coexist because the two special uses intercept the right-click <b>before</b> the item's
 * own {@link #use} ever runs (the client knap mixin cancels it; the hot-rock callback consumes the block
 * click), so {@code use} is reached only on a plain right-click with nothing else in play — exactly when
 * a throw is what you want. Mirrors {@code SnowballItem}/{@code EggItem}: consume one, play a throw
 * sound, and (server-side) spawn the projectile from the player's aim.
 */
public class RockItem extends Item {
    /** A short beat to stoop, weigh, and cock your arm for the next stone. */
    private static final int COOLDOWN = 10;
    /** A hurled stone is slow and low-arced — a shorter, softer lob than the slingshot's fast, flat sling. */
    private static final float SHOOT_POWER = 1.1f;
    /** A little scatter — a bare-hand throw is nowhere near as true as a slung stone. */
    private static final float UNCERTAINTY = 4.0f;

    public RockItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
            SoundSource.PLAYERS, 0.6f, 0.6f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (level instanceof ServerLevel serverLevel) {
            // yOffset 0 fires from the eye along the aim; SHOOT_POWER keeps the stone slow (short carry, big
            // arc) and UNCERTAINTY scatters it — a thrown rock is no marksman's tool.
            Projectile.spawnProjectileFromRotation(ThrownRock::new, serverLevel, stack, player,
                0.0f, SHOOT_POWER, UNCERTAINTY);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (!player.isCreative()) {
            stack.consume(1, player);
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }
}
