package dev.alone.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
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
    public static final int SIZE = 18; // a couple of rows of heavy cargo
    /** The block model the travois is drawn as (a low platform) until it gets real art. */
    public static final BlockState SHOWN_BLOCK = Blocks.SPRUCE_SLAB.defaultBlockState();

    private static final Identifier DRAG_MODIFIER = Identifier.fromNamespaceAndPath("alone", "travois_drag");
    private static final double GRAB_RANGE_SQR = 400.0; // let go if the hauler gets more than 20 blocks off
    private static final float BASE_HAUL_FACTOR = 0.75f; // even empty, dragging a sled is slower than walking
    private static final float FULL_LOAD_KG = 200f;      // load at which you're at the slowest haul
    private static final float MIN_HAUL_FACTOR = 0.40f;

    /** Cargo persists on the entity as a list of the non-empty stacks (slot order isn't meaningful here). */
    public static final AttachmentType<List<ItemStack>> CARGO = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "travois_cargo"), ItemStack.CODEC.listOf());

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private boolean loaded = false;
    private UUID draggerUuid; // who's dragging right now (transient — you re-grab after a reload)

    public TravoisEntity(EntityType<? extends TravoisEntity> type, Level level) {
        super(type, level);
    }

    // ── Entity plumbing ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // no synched state for now — the visual is a fixed block model
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
            if (draggerUuid != null) {
                releaseDragger();
            } else {
                draggerUuid = player.getUUID();
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
        if (draggerUuid != null) {
            Player dragger = level().getPlayerByUUID(draggerUuid);
            if (dragger == null || dragger.isRemoved() || dragger.distanceToSqr(this) > GRAB_RANGE_SQR) {
                releaseDragger();
            } else {
                followAndSlow(dragger);
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        double friction = onGround() ? 0.55 : 0.96;
        setDeltaMovement(getDeltaMovement().multiply(friction, 0.98, friction));
    }

    private void followAndSlow(Player dragger) {
        // Trail a metre or so behind the hauler, on their heading.
        Vec3 heading = dragger.getLookAngle().multiply(1, 0, 1);
        if (heading.lengthSqr() < 1.0e-4) {
            heading = new Vec3(0, 0, 1);
        }
        Vec3 behind = dragger.position().subtract(heading.normalize().scale(1.3));
        Vec3 toTarget = behind.subtract(position());
        setDeltaMovement(toTarget.x * 0.5, getDeltaMovement().y, toTarget.z * 0.5);
        if (toTarget.horizontalDistanceSqr() > 1.0e-4) {
            setYRot((float) (Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z))));
        }

        float factor = haulFactor();
        if (dragger.getY() > getY() + 0.5) {
            factor *= 0.8f; // hauling uphill is worse
        }
        AttributeInstance speed = dragger.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(
                DRAG_MODIFIER, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** How much a loaded sled slows you: base drag, worse the heavier the cargo. */
    private float haulFactor() {
        float load = 0f;
        ensureLoaded();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                load += Carry.itemWeight(stack);
            }
        }
        float t = Math.min(1.0f, load / FULL_LOAD_KG);
        return BASE_HAUL_FACTOR - (BASE_HAUL_FACTOR - MIN_HAUL_FACTOR) * t;
    }

    private void releaseDragger() {
        if (draggerUuid != null) {
            Player dragger = level().getPlayerByUUID(draggerUuid);
            if (dragger != null) {
                AttributeInstance speed = dragger.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(DRAG_MODIFIER);
                }
            }
            draggerUuid = null;
        }
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
