package dev.alone.core;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A travois (proposal §6) — the pre-wheel hauler: two poles and a hide platform you drag behind you.
 * Deploy it from the item, load it with the heavy blocks your own carry limits (§5.1) won't let you
 * pocket, then <b>sneak-right-click to grab the handles</b> and it follows you, slowing you by how much
 * it's loaded (dragging a full sled is a crawl). Right-click to open its cargo; hit it to pack it back up
 * (it drops the travois and its load). Rendered as a low wooden platform.
 */
public class TravoisEntity extends Entity implements Container {
    public static final int SIZE = 27; // must match ChestMenu.threeRows, or opening it indexes out of bounds
    /** Drawn as a low platform with two trailing poles (see the renderer) until it gets real art.
     *  TEMP BUILD-FRESHNESS PROBE: gold blocks. If your travois isn't obviously GOLD after a relaunch,
     *  your client is running stale classes — which is also why the "red glow" fix hasn't taken. */
    public static final BlockState PLATFORM_BLOCK = Blocks.GOLD_BLOCK.defaultBlockState();
    public static final BlockState POLE_BLOCK = Blocks.GOLD_BLOCK.defaultBlockState();

    private static final Identifier DRAG_MODIFIER = Identifier.fromNamespaceAndPath("alone", "travois_drag");
    private static final double GRAB_RANGE_SQR = 400.0; // let go if the hauler gets more than 20 blocks off
    private static final float BASE_HAUL_FACTOR = 0.75f; // even empty, dragging a sled is slower than walking
    private static final float FULL_LOAD_KG = 200f;      // load at which you're at the slowest haul
    private static final float MIN_HAUL_FACTOR = 0.40f;

    /** Cargo persists on the entity as a list of the non-empty stacks (slot order isn't meaningful here). */
    public static final AttachmentType<List<ItemStack>> CARGO = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "travois_cargo"), ItemStack.CODEC.listOf());

    // Synced to nearby clients so the client can enforce the haul on the LOCAL player — player movement is
    // client-authoritative, so jump/sprint limiting has to happen client-side (see AloneCoreClient).
    private static final EntityDataAccessor<Integer> DATA_DRAGGER =
        SynchedEntityData.defineId(TravoisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_HAUL_FACTOR =
        SynchedEntityData.defineId(TravoisEntity.class, EntityDataSerializers.FLOAT);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private boolean loaded = false;

    /** Entity id of who's dragging, or -1. Client reads this to know if it should slow the local player. */
    public int getDraggerId() {
        return getEntityData().get(DATA_DRAGGER);
    }

    private void setDraggerId(int id) {
        getEntityData().set(DATA_DRAGGER, id);
    }

    /** The dragging player, either side, or null. */
    private Player draggerPlayer() {
        int id = getDraggerId();
        return id != -1 && level().getEntity(id) instanceof Player player ? player : null;
    }

    /** The current haul slowdown factor (1 = none), synced so the client caps the hauler's speed to match. */
    public float getHaulFactor() {
        return getEntityData().get(DATA_HAUL_FACTOR);
    }

    public TravoisEntity(EntityType<? extends TravoisEntity> type, Level level) {
        super(type, level);
    }

    // ── Entity plumbing ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DRAGGER, -1);
        builder.define(DATA_HAUL_FACTOR, 1.0f);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // cargo lives on the CARGO attachment (auto-persisted); nothing else to read
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // cargo lives on the CARGO attachment (auto-persisted); nothing else to write
    }

    @Override
    public boolean isPickable() {
        return !isRemoved(); // so you can right-click / hit it
    }

    // A wooden sled shouldn't read as "on fire" — being fire-immune stops the base renderer drawing the
    // flame overlay (which showed up as a red glow), and it won't burn up under you either.
    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    // The "red border showing through walls" is the glowing-outline pass, gated by isCurrentlyGlowing().
    // A travois is never a highlighted object — report not-glowing so it's never drawn into that pass.
    @Override
    public boolean isCurrentlyGlowing() {
        return false;
    }

    // It can slide up a single-block step (a kerb, a slab-high rise) so it follows over gentle ground, but
    // a two-block ledge stops it dead — you clear a ramp/path for it. That's what makes routes matter (§6).
    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || !(source.getEntity() instanceof Player)) {
            return false;
        }
        dropAll(); // hitting it packs the sled up — travois and cargo drop
        discard();
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseDragger(); // never leave a stuck slowdown on the hauler
        super.remove(reason);
    }

    // ── Interaction ──────────────────────────────────────────────────────────────────────────────
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hit) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            // Grab or drop the handles.
            if (getDraggerId() != -1) {
                releaseDragger();
            } else {
                setDraggerId(player.getId());
            }
            return InteractionResult.SUCCESS;
        }
        // Open the cargo bed.
        player.openMenu(new SimpleMenuProvider(
            (id, inv, opener) -> ChestMenu.threeRows(id, inv, this), Component.literal("Travois")));
        return InteractionResult.SUCCESS;
    }

    // ── Movement: gravity, follow-the-hauler, and the load-scaled slowdown ────────────────────────
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        // Gravity + ground friction so it settles and slides, not floats.
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, -0.08, 0.0));
        }
        Player dragger = draggerPlayer();
        if (dragger != null && !dragger.isRemoved() && dragger.distanceToSqr(this) <= GRAB_RANGE_SQR) {
            followAndSlow(dragger);
        } else if (getDraggerId() != -1) {
            releaseDragger();
        }
        move(MoverType.SELF, getDeltaMovement());
        double friction = onGround() ? 0.55 : 0.96;
        setDeltaMovement(getDeltaMovement().multiply(friction, 0.98, friction));
    }

    private void followAndSlow(Player dragger) {
        // Trail based on where the hauler WALKS, not where they look: just get pulled toward them once the
        // tether goes taut. Standing still and turning your head doesn't swing the sled around — it only
        // moves when you actually walk away from it, so it naturally follows your path of travel.
        Vec3 toPlayer = dragger.position().subtract(position());
        double dist = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
        double tether = 1.4;
        boolean beingPulled = dist > tether;
        if (beingPulled) {
            double nx = toPlayer.x / dist;
            double nz = toPlayer.z / dist;
            double pull = dist - tether;
            setDeltaMovement(nx * pull * 0.6, getDeltaMovement().y, nz * pull * 0.6);
            setYRot((float) Math.toDegrees(Math.atan2(-nx, nz))); // face the way it's being pulled
        } else {
            Vec3 dm = getDeltaMovement();
            setDeltaMovement(dm.x * 0.5, dm.y, dm.z * 0.5); // within reach — settle, don't reorient
        }

        float loadT = Math.min(1.0f, cargoWeight() / FULL_LOAD_KG);
        float factor = BASE_HAUL_FACTOR - (BASE_HAUL_FACTOR - MIN_HAUL_FACTOR) * loadT;
        if (dragger.getY() > getY() + 0.5) {
            factor *= 0.8f; // hauling uphill is worse
        }
        getEntityData().set(DATA_HAUL_FACTOR, factor); // sync so the client can cap the hauler to match

        // Slow the walk via the movement attribute (client respects it). The jump/sprint ceiling can't be
        // done here — player movement is client-authoritative — so the client enforces that (AloneCoreClient).
        AttributeInstance speed = dragger.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                DRAG_MODIFIER, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        // Dragging a loaded sled is real work — it costs stamina while it's actually being hauled. We gate
        // on the sled being pulled (server-reliable), NOT the player's delta, which reads ~0 server-side.
        if (beingPulled) {
            SurvivalMeters.exert(dragger, 0.06f + 0.24f * loadT);
        }
    }

    /** Total mass of the cargo, in kg — drives both the slowdown and the stamina cost. */
    private float cargoWeight() {
        ensureLoaded();
        float load = 0f;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                load += Carry.itemWeight(stack);
            }
        }
        return load;
    }

    private void releaseDragger() {
        Player dragger = draggerPlayer();
        if (dragger != null) {
            AttributeInstance speed = dragger.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(DRAG_MODIFIER);
            }
        }
        setDraggerId(-1);
        getEntityData().set(DATA_HAUL_FACTOR, 1.0f);
    }

    private void dropAll() {
        if (level().isClientSide()) {
            return;
        }
        ensureLoaded();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.2, getZ(), stack.copy()));
            }
        }
        level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.3, getZ(),
            new ItemStack(AloneItems.TRAVOIS)));
    }

    // ── Container (the cargo bed), backed by the CARGO attachment ─────────────────────────────────
    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        List<ItemStack> saved = getAttachedOrElse(CARGO, List.of());
        for (int i = 0; i < saved.size() && i < SIZE; i++) {
            items.set(i, saved.get(i).copy());
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        ensureLoaded();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        ensureLoaded();
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ensureLoaded();
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ensureLoaded();
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ensureLoaded();
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                nonEmpty.add(stack);
            }
        }
        setAttached(CARGO, nonEmpty);
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(this) < 64.0;
    }

    @Override
    public void clearContent() {
        ensureLoaded();
        for (int i = 0; i < SIZE; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }
}
