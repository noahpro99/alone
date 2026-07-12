package dev.alone.core;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The squirrel — small game to round out the woods alongside the rabbit and chicken. On the show
 * <i>Alone</i> the little animals, not big game, are what people actually live on: caught in numbers,
 * a scrap of meat each. It <b>scurries</b> on the ground and, when a person gets close, <b>bolts for the
 * nearest tree and climbs the trunk</b> — the thing that makes a squirrel a squirrel and not a rabbit.
 * The climb reuses vanilla's wall-climber mechanism (as spiders do), gated to <b>trees</b> (logs/leaves) so
 * it scales trunks, not player walls. It's still made <b>skittish</b> by {@link Wildlife} (so it flees when
 * no tree is near), winds <b>fast</b> under {@link Tracking persistence} thanks to its tiny body, counts
 * toward {@link GameStock overhunting}, and a blade kill gives only a scrap of pelt (butcher salvage scales
 * with body size). Reuses the rabbit render-state as <b>placeholder art</b> until a real model lands.
 */
public class Squirrel extends Animal {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID =
        SynchedEntityData.defineId(Squirrel.class, EntityDataSerializers.BYTE);

    // How the squirrel reads a person and a tree. It lets you get fairly close, then makes for a trunk.
    private static final double CLIMB_TRIGGER = 16.0; // a person this near and a tree in reach → go up
    private static final double GIVE_UP = 20.0;       // stays up the tree until they back off this far
    private static final int TREE_SEARCH = 8;         // how far it will run for a trunk
    private static final int CLIMB_HEIGHT = 6;        // how high up the trunk it climbs before it clings
    private static final double CLIMB_SPEED = 1.3;

    public Squirrel(EntityType<? extends Squirrel> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbTreeGoal(this)); // preempts the ground flee when a tree is near
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, Squirrel::isSquirrelFood, false));
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0)); // quick, darty scurry
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        // The skittish ground-flee (bolt when a person is noticed, but no tree to climb) is injected by
        // Wildlife at priority 2, so it slots just under the climb.
    }

    /** Wall-climber navigation so it can path up a trunk to reach a spot above it, exactly as a spider does. */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS_ID, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        // Cling to a trunk only when actually pressed against a tree — so it climbs trees, not fences.
        if (!this.level().isClientSide()) {
            this.setClimbing(this.horizontalCollision && this.besideTree());
        }
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setClimbing(boolean value) {
        byte flags = this.entityData.get(DATA_FLAGS_ID);
        flags = value ? (byte) (flags | 1) : (byte) (flags & ~1);
        this.entityData.set(DATA_FLAGS_ID, flags);
    }

    /** True when a trunk or canopy is right beside the squirrel (at foot or head height). */
    private boolean besideTree() {
        BlockPos base = this.blockPosition();
        for (BlockPos p : new BlockPos[]{base, base.above()}) {
            for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var state = this.level().getBlockState(p.relative(dir));
                if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSquirrelFood(ItemStack stack) {
        return stack.is(ItemTags.RABBIT_FOOD); // omnivore; placeholder forage until a nut/seed set lands
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isSquirrelFood(stack);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return AloneEntities.SQUIRREL.create(level, EntitySpawnReason.BREEDING); // squirrels breed squirrels
    }

    /**
     * A person gets within a dozen-odd blocks → make for the nearest tree and scurry up the trunk, then
     * cling and watch until they back off. This is what separates the squirrel from the rabbit: cornered,
     * it doesn't just zig-zag away on the ground, it goes vertical where you can't follow.
     */
    private static class ClimbTreeGoal extends Goal {
        private final Squirrel squirrel;
        private Player threat;
        private BlockPos perch;
        private int repath;

        ClimbTreeGoal(Squirrel squirrel) {
            this.squirrel = squirrel;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player p = squirrel.level().getNearestPlayer(squirrel, CLIMB_TRIGGER);
            if (p == null || p.isCreative() || p.isSpectator()) {
                return false;
            }
            BlockPos target = findClimbTarget();
            if (target == null) {
                return false; // no tree in reach — let the ground-flee handle it
            }
            this.threat = p;
            this.perch = target;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return threat != null && threat.isAlive() && perch != null
                && squirrel.distanceToSqr(threat) <= GIVE_UP * GIVE_UP;
        }

        @Override
        public void start() {
            squirrel.getNavigation().moveTo(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5, CLIMB_SPEED);
            repath = 0;
        }

        @Override
        public void stop() {
            threat = null;
            perch = null;
            squirrel.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (threat != null) {
                squirrel.getLookControl().setLookAt(threat, 30f, 30f);
            }
            // Reached the perch and hugging the trunk → cling and watch (onClimbable holds it against gravity).
            if (squirrel.getY() >= perch.getY() - 1 && squirrel.besideTree()) {
                squirrel.getNavigation().stop();
                return;
            }
            if (squirrel.getNavigation().isDone() && --repath <= 0) {
                squirrel.getNavigation().moveTo(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5, CLIMB_SPEED);
                repath = 10;
            }
        }

        /** The nearest tree trunk within reach, and a spot several blocks up it (toward the canopy) to climb to. */
        private BlockPos findClimbTarget() {
            Level level = squirrel.level();
            BlockPos origin = squirrel.blockPosition();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            BlockPos bestTrunk = null;
            double best = Double.MAX_VALUE;
            for (int dx = -TREE_SEARCH; dx <= TREE_SEARCH; dx++) {
                for (int dz = -TREE_SEARCH; dz <= TREE_SEARCH; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!level.getBlockState(m).is(BlockTags.LOGS)) {
                            continue;
                        }
                        double d = dx * dx + dz * dz;
                        if (d < best) {
                            best = d;
                            bestTrunk = m.immutable();
                        }
                        break; // one trunk hit per column is enough
                    }
                }
            }
            if (bestTrunk == null) {
                return null;
            }
            // Climb toward the top of the contiguous trunk (the canopy), capped a few blocks up.
            BlockPos top = bestTrunk;
            for (int i = 0; i < CLIMB_HEIGHT; i++) {
                BlockPos up = top.above();
                if (level.getBlockState(up).is(BlockTags.LOGS)) {
                    top = up;
                } else {
                    break;
                }
            }
            return top;
        }
    }
}
