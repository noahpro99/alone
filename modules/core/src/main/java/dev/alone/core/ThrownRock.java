package dev.alone.core;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A hand-thrown rock (§8.1) — the most primitive ranged weapon there is, the tier <b>below the {@link
 * SlingshotItem slingshot}</b>. No forked stick or bands, just a stone off the ground and your arm: it
 * arcs (real gravity, unlike the slung stone's flat, fast hitscan), carries only a short way, and hits
 * softly. Enough to help drop a bird, a squirrel, or a rabbit if you're close and lucky; barely a
 * nuisance to anything big. It's free and always in reach, so it's the day-one food-getter you fall back
 * on before you've knapped a blade or bent a slingshot.
 *
 * <p>Mirrors vanilla {@code Snowball}: a {@link
 * net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile} that flings its
 * own item (the {@link AloneItems#ROCK loose rock}) and breaks on impact. On hitting an entity it deals a
 * small hurt <b>attributed to the thrower</b> (a {@code thrown} damage source whose causing entity is the
 * player), so a kill butchers, tracks, and drops exactly as a player kill would (see {@link Hunting} —
 * it keys off {@code source.getEntity() instanceof Player}).
 */
public class ThrownRock extends net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile {
    /** A thrown stone stings but doesn't maim — enough to help finish small game, little to a big animal. */
    private static final float DAMAGE = 2.0f;

    /** Registry/client constructor — the type is rebuilt here from saved data or the spawn packet. */
    public ThrownRock(final EntityType<? extends ThrownRock> type, final Level level) {
        super(type, level);
    }

    /** Thrown-by-a-mob constructor (the {@link net.minecraft.world.entity.projectile.Projectile.ProjectileFactory}
     *  shape {@code Projectile.spawnProjectileFromRotation} calls): spawns at the thrower's eye and sets them
     *  as the owner so the hit is credited to them. */
    public ThrownRock(final Level level, final LivingEntity thrower, final ItemStack itemStack) {
        super(AloneEntities.THROWN_ROCK, thrower, level, itemStack);
    }

    /** The item this projectile is (and renders as) when none was set — a plain loose rock. */
    @Override
    protected Item getDefaultItem() {
        return AloneItems.ROCK;
    }

    /** The little puff of rock chips flung off on impact (entity-event 3, like a snowball's snow burst). */
    private ParticleOptions getParticle() {
        ItemStack item = this.getItem();
        return item.isEmpty()
            ? new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(new ItemStack(getDefaultItem())))
            : new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(item));
    }

    @Override
    public void handleEntityEvent(final byte id) {
        if (id == 3) {
            ParticleOptions particle = this.getParticle();
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    /** On a body hit: a small hurt, credited to the thrower so hunting/butchering treats it as their kill.
     *  The knock is left to vanilla's own damage-knockback (small, and along the stone's line of flight). */
    @Override
    protected void onHitEntity(final EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity target = hitResult.getEntity();
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            target.hurtServer(serverLevel, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
        }
    }

    /** A thrown stone doesn't stick or bounce usefully — it lands and is spent (it's already back on the
     *  ground as world stone, so it doesn't drop itself back as an item). Flings its chip particles first. */
    @Override
    protected void onHit(final HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
