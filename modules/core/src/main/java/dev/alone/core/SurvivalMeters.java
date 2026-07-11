package dev.alone.core;

import com.mojang.serialization.Codec;
import dev.alone.core.net.SurvivalSyncPayload;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/**
 * Survival meters (proposal §1) — the daily-loop drivers every other system reads and feeds.
 * <b>Stamina</b> (§1.4), <b>thirst</b> (§1.2), and ambient <b>temperature</b> (§1.3), plus a
 * realism tweak: shorter arm reach than vanilla. Server-authoritative via persistent attachments,
 * synced to the HUD each half-second. This is the framework the condition panel and later meters
 * hang off.
 */
public final class SurvivalMeters {
    private SurvivalMeters() {
    }

    public static final float MAX_STAMINA = 100f;
    // MC sprint ≈ 5.6 m/s — a hard run, not an all-out dash. An average fit adult holds that pace a
    // few minutes; we compress to ~40s to empty (~225 m) so it's costly but not a 10-second gasp.
    private static final float SPRINT_DRAIN = 0.125f;
    private static final float SWING_DRAIN = 0.1f;  // mining / chopping / attacking is exertion
    private static final float REST_REGEN = 0.2f;   // ~25s rest to full
    private static final float STAMINA_LOW = 20f;
    private static final float FATIGUE_GAIN = 0.04f;         // exertion builds medium-term fatigue…
    private static final float FATIGUE_RECOVER = 0.015f;     // …which sheds slowly with rest…
    private static final float FATIGUE_SLEEP_RECOVER = 1.5f; // …and fast while sleeping (clears a night).
    private static final float SLEEP_REGEN = 4.0f;           // stamina refilled per tick while asleep
    private static final float FATIGUE_MAX_PENALTY = 0.25f;  // sore → up to 25% lower stamina ceiling

    public static final float MAX_THIRST = 100f;
    private static final float THIRST_DRAIN = 0.01f;        // base, ~8 min at rest
    private static final float THIRST_DRAIN_SPRINT = 0.02f; // exertion
    private static final float THIRST_DRAIN_SWEAT = 0.04f;  // full-body-heat sweat rate (scaled by how hot you run)
    private static final float THIRST_LOW = 15f;
    // Thermoregulation feedback (§1.3) — a hot body sweats (dehydrates), a cold body burns food and shivers.
    private static final float COLD_SHIVER = -30f;      // below this, stiff muscles recover stamina poorly
    private static final int CHILL_INTERVAL = 600;      // roll a chill about every 30s of cold-and-wet
    private static final float CHILL_CHANCE = 0.2f;      // odds per roll — a few minutes of exposure and it takes
    private static final float COLD_EXHAUSTION = 0.03f; // extra food burned per tick keeping warm (scaled by cold)

    private static final float SINK_WEIGHT = 22f; // past this you can't stay afloat (one full block ≈ 30 kg)
    // Slow vitality recovery (§1.5) — heals only when fed, hydrated, and free of active wounds/illness.
    private static final int HEAL_FOOD_MIN = 14;    // need a reasonably full stomach to mend
    private static final float HEAL_THIRST_MIN = 30f;
    private static final int HEAL_INTERVAL = 200;   // 1 HP / ~10s in good conditions (full heal ≈ 3–4 min)
    private static final float SWIM_FREE_WEIGHT = 8f;   // load past this makes staying afloat real work
    private static final float SWIM_WEIGHT_DRAIN = 0.012f; // extra stamina/tick per kg over, while in water

    // Going up is work you do by jumping (and by climbing — see Climbing), not by walking a slope. Each
    // jump costs stamina, and a load makes every hop up a hill or a step bite far harder (§1.4/§5.1).
    private static final float JUMP_BASE = 3f;      // a bare hop
    private static final float JUMP_PER_KG = 0.12f; // …plus what your pack adds to launching yourself

    /** Realistic block reach — shorter than vanilla's 4.5, but long enough to place a block beneath your
     *  feet at the top of a jump (~eye 1.6 + jump 1.25), so pillar-jumping works. Also the drink raycast. */
    public static final double BLOCK_REACH = 3.0;
    private static final double ENTITY_REACH = 2.5; // shorter reach is realism; combat worked once Weakness was fixed

    /** Game time of the last "too exhausted to hit" message, so it doesn't spam on every swing. */
    public static final AttachmentType<Long> LAST_EXHAUSTION_MSG =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "last_exhaustion_msg"), Codec.LONG);

    /** Game time until which "vigor" lasts — a golden-apple second wind: fast recovery, no fatigue (§1.4). */
    public static final AttachmentType<Long> VIGOR_UNTIL =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "vigor_until"), Codec.LONG);
    private static final float VIGOR_REGEN_MULT = 3f;

    public static final AttachmentType<Float> STAMINA =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "stamina"), Codec.FLOAT);
    /** Medium-term soreness (0..100): the higher it is, the lower your usable stamina ceiling (§1.4). */
    public static final AttachmentType<Float> FATIGUE =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "fatigue"), Codec.FLOAT);
    public static final AttachmentType<Float> THIRST =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "thirst"), Codec.FLOAT);

    // Body temperature (§1.3): -100 (freezing) .. 0 (comfortable) .. +100 (overheating).
    private static final float NEUTRAL_AMBIENT = 0.8f; // biome temp that feels comfortable
    private static final float TEMP_SCALE = 100f;
    private static final float WET_COLD_SHIFT = 35f;   // wetness chills you / blunts heat
    private static final float WATER_TARGET = -30f;    // being IN water pulls you to at least "slightly cold"
    // Caves are NOT the mild refuge they seem (proposal §1.3, cave hypothermia). Below the surface, air
    // goes stagnant and near-saturated, and the rock locks to a cold that only deepens with depth. All on
    // the biome scale (NEUTRAL_AMBIENT ≈ comfortable): CAVE_COLD ≈ 0.45 → a shivering -35 body-equivalent,
    // survivable dry but a hypothermia risk once wet or still; the deep floor ≈ 0.2 → -60, cold even dry.
    private static final float CAVE_COLD = 0.45f;      // the constant-cold zone (~15m down): a damp, shivering chill
    private static final float CAVE_DEEP_FLOOR = 0.2f; // the deep abyss near bedrock: hypothermic even when dry
    private static final int CAVE_VARIABLE_DEPTH = 15; // m over which the surface's influence fades underground
    private static final int CAVE_DEEP_DEPTH = 110;    // m below the cold zone at which the chill bottoms out (~bedrock)
    private static final float NIGHT_CHILL = 0.2f;      // nights are colder than days (biome scale)
    private static final float BLIZZARD_CHILL = 0.6f;   // a winter storm freezes the exposed fast — any roof breaks it
    private static final float ROOF_INSULATION = 0.4f;  // a bare roof (open lean-to) blunts ~40% of cold/heat (§5.5)
    private static final float MAX_INSULATION = 0.78f;   // a snug, fully-walled shelter — holds heat far better
    private static final int WALL_REACH = 4;             // a wall within this many blocks shelters that side
    private static final float HOT_MEAL_CAP = 15f;       // a hot meal warms you toward comfort, not heatstroke
    // Clothing insulation (§1.3/§5.5): leather/hide layers trap body heat — a blessing in the cold,
    // a burden in the heat; metal plates insulate little and bake in the sun.
    private static final float LEATHER_INSULATION = 9f;  // per hide piece — a full set (~36) beats a cold night
    private static final float METAL_INSULATION = 2f;    // per metal plate — barely a windbreak
    private static final float HEAT_TRAP = 0.4f;         // fraction of your insulation that turns into a burden when hot
    private static final float METAL_SUN_HEAT = 4f;      // per metal plate baking under an open sky
    private static final EquipmentSlot[] ARMOR_SLOTS =
        {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public static final float HOT_MEAL_WARMTH = 25f;     // body-temp bump from a hot meal (drifts back)
    private static final float EXPOSURE_RATE = 0.03f;  // drift toward a harsher environment (slow)
    private static final float RECOVERY_RATE = 0.05f;  // drift back toward a milder one
    private static final float HEAT_RATE = 0.08f;      // radiant heat warms gradually, not instantly
    private static final float HYPOTHERMIA = -50f;
    private static final float SEVERE_COLD = -85f;
    private static final float HEATSTROKE = 50f;
    private static final float SEVERE_HOT = 85f;
    // Temperature is a slow, telegraphed killer in real life — once you're at the extreme, ~1 HP / 5s
    // (~100s from full), so the shivering/roasting warning band gives you time to reach fire or shade.
    private static final int TEMP_DAMAGE_INTERVAL = 100;

    public static final AttachmentType<Float> BODY_TEMP =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "body_temp"), Codec.FLOAT);
    /** How wet you are, in ticks — soaked by water/rain, drying over time (§1.3). */
    public static final AttachmentType<Integer> WETNESS =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "wetness"), Codec.INT);
    private static final int MAX_WETNESS = 600;   // ~30s to dry naturally

    /** Clock time you lay down, so waking can charge you the metabolism of the night you slept through. */
    public static final AttachmentType<Long> SLEEP_START =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "sleep_start"), Codec.LONG);
    private static final float SLEEP_THIRST_PER_DAY = 40f; // water lost sleeping a whole day (a night ≈ half)
    private static final int SLEEP_HUNGER_PER_DAY = 8;      // food burned sleeping a whole day
    private static final int DRY_NATURAL = 1;
    private static final int DRY_BY_FIRE = 5;     // a fire dries you ~5x faster

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
                Nutrition.tick(player);
            }
        });

        // Death is a setback, not a reset (§11): you're "carried home" and wake days later — battered,
        // hungry, thirsty, and weak, with a lingering injury. Recovery is the real death penalty.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, returnFromEnd) -> {
            if (returnFromEnd) {
                return; // only after an actual death, not returning from the End
            }
            newPlayer.setHealth(newPlayer.getMaxHealth() * 0.4f);
            newPlayer.getFoodData().setFoodLevel(6);
            newPlayer.setAttached(THIRST, 25f);
            newPlayer.setAttached(STAMINA, 20f);
            newPlayer.setAttached(FATIGUE, 80f);
            Conditions.addSprain(newPlayer, 600); // you didn't walk home unscathed
            newPlayer.sendSystemMessage(Component.literal(
                "You wake at your homestead, days later — battered, hungry, and weak."));
        });

        // Exhaustion makes your hits feeble (Weakness at 0 stamina). When a spent player swings at a
        // mob, tell them why it did nothing — otherwise "hitting cows doesn't work" reads as a bug.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!level.isClientSide() && !player.isCreative() && getStamina(player) <= 0f) {
                long now = level.getGameTime();
                if (now - player.getAttachedOrElse(LAST_EXHAUSTION_MSG, -100L) >= 30L) {
                    player.setAttached(LAST_EXHAUSTION_MSG, now);
                    player.sendSystemMessage(Component.literal("You're too exhausted to land a solid blow — rest first."));
                }
            }
            return InteractionResult.PASS;
        });

        // Remember when you lay down, so waking can charge the night's metabolism (§1.4).
        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayer player) {
                player.setAttached(SLEEP_START, player.level().getOverworldClockTime());
            }
        });

        // A night's sleep restores you (§1.4) — but only as well as you were comfortable (§5.5). Warm
        // and sheltered = a full night; cold or roasting = a poor one. Applied on waking. And sleep is
        // NOT free: you wake having burned the night's food and water, so a long rest leaves you hungry
        // and dry — you can't skip the night's metabolism by skipping the night.
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayer player) {
                float quality = restQuality(player);
                rest(player, 100f * quality, MAX_STAMINA * quality);
                if (quality < 1.0f) {
                    player.sendSystemMessage(Component.literal(getBodyTemp(player) < 0f
                        ? "You slept poorly — too cold to rest well."
                        : "You slept poorly — too hot to rest well."));
                }

                // Charge the metabolism of the time actually slept (the clock jumps to morning).
                long start = player.getAttachedOrElse(SLEEP_START, player.level().getOverworldClockTime());
                long slept = ((player.level().getOverworldClockTime() - start) % 24000L + 24000L) % 24000L;
                float dayFraction = slept / 24000f;
                drink(player, -SLEEP_THIRST_PER_DAY * dayFraction);
                int hunger = Math.round(SLEEP_HUNGER_PER_DAY * dayFraction);
                if (hunger > 0) {
                    player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - hunger));
                }
                if (slept > 3000L) {
                    player.sendSystemMessage(Component.literal(
                        "You wake rested — but the night has left you hungry and dry."));
                }
                // A night's rest also mends illness (§1.5): sickness, a festering wound, and dysentery all
                // ebb as you sleep — the more comfortable the rest, the more (a shivering night mends little).
                int mend = Math.round(slept * 0.5f * quality);
                if (mend > 0) {
                    Conditions.relieveSickness(player, mend);
                    Conditions.relieveInfection(player, mend);
                    Conditions.relieveDysentery(player, mend);
                }
                player.removeAttached(SLEEP_START);
            }
        });
    }

    /** True when there's a solid block within about a metre below the feet — you're on or just off the
     *  bottom, close enough to push off it and rise a block even under a load that would otherwise sink you. */
    private static boolean nearSolidBelow(Player player) {
        if (player.onGround()) {
            return true;
        }
        Level level = player.level();
        BlockPos below = player.blockPosition().below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    public static float getStamina(Player player) {
        return player.getAttachedOrElse(STAMINA, MAX_STAMINA);
    }

    public static float getFatigue(Player player) {
        return player.getAttachedOrElse(FATIGUE, 0f);
    }

    public static float getThirst(Player player) {
        return player.getAttachedOrElse(THIRST, MAX_THIRST);
    }

    public static float getBodyTemp(Player player) {
        return player.getAttachedOrElse(BODY_TEMP, 0f);
    }

    /** Restore thirst (drinking). */
    public static void drink(Player player, float amount) {
        // clamps both ends so a negative amount (salt water dehydrating you, §1.2) is safe too
        player.setAttached(THIRST, Math.max(0f, Math.min(MAX_THIRST, getThirst(player) + amount)));
    }

    /**
     * How restful sleep is, by how comfortable your body is (§1.4/§5.5): a warm, sheltered night
     * restores you fully; shivering or roasting, you barely rest. 1.0 comfortable → 0.3 extreme.
     */
    public static float restQuality(Player player) {
        float t = Math.abs(getBodyTemp(player));
        if (t >= 50f) {
            return 0.3f; // freezing or roasting — fitful, almost no recovery
        }
        if (t >= 20f) {
            return 0.6f; // cold/hot — a poor night
        }
        return 1.0f;     // comfortable — a full night's rest
    }

    /** A hot meal warms you (§1.3) — raises body temperature toward comfort, never into heatstroke. */
    public static void warm(Player player, float amount) {
        float current = getBodyTemp(player);
        float warmed = Math.min(HOT_MEAL_CAP, current + amount);
        if (warmed > current) {
            player.setAttached(BODY_TEMP, warmed); // it drifts back toward the environment over time
        }
    }

    /** A cool drink eases the heat (§1.2/§1.3) — lowers an overheated body toward comfort, never into a
     *  chill (a drink can't make a comfortable body cold). Drifts back toward the environment over time. */
    public static void cool(Player player, float amount) {
        float current = getBodyTemp(player);
        if (current <= 0f) {
            return; // only relief when you're actually hot
        }
        player.setAttached(BODY_TEMP, Math.max(0f, current - amount));
    }

    /** Ingesting cold — a mouthful of snow, ice-cold water — pulls your core DOWN, and unlike {@link #cool}
     *  it can chill a comfortable body below neutral into the cold (§1.3). It's why you melt snow, not eat it. */
    public static void chill(Player player, float amount) {
        player.setAttached(BODY_TEMP, Math.max(-100f, getBodyTemp(player) - amount));
    }

    /** Restore stamina directly (energy from food). */
    public static void restoreStamina(Player player, float amount) {
        player.setAttached(STAMINA, Math.max(0f, Math.min(MAX_STAMINA, getStamina(player) + amount)));
    }

    /** Grant a "vigor" window (a golden-apple second wind): fast stamina recovery and no fatigue build. */
    public static void grantVigor(Player player, int ticks) {
        long until = player.level().getGameTime() + ticks;
        player.setAttached(VIGOR_UNTIL, Math.max(player.getAttachedOrElse(VIGOR_UNTIL, 0L), until));
    }

    public static boolean isVigorous(Player player) {
        return player.level().getGameTime() < player.getAttachedOrElse(VIGOR_UNTIL, 0L);
    }

    /** Spend stamina on effort (mining, chopping, …). */
    public static void exert(Player player, float amount) {
        if (amount > 0f) {
            player.setAttached(STAMINA, Math.max(0f, getStamina(player) - amount));
        }
    }

    /** Recover from resting/sleeping — sheds medium-term fatigue and restores stamina. */
    public static void rest(Player player, float fatigueShed, float staminaRestore) {
        player.setAttached(FATIGUE, Math.max(0f, getFatigue(player) - fatigueShed));
        player.setAttached(STAMINA, Math.min(MAX_STAMINA, getStamina(player) + staminaRestore));
    }

    /**
     * Ambient temperature around the player (§1.3): biome baseline, cooled by weather.
     * Season will feed in here once §10 (Alone: World) exists — see the TODO.
     */
    public static float ambientTemperature(Player player) {
        return ambientTemperatureAt(player.level(), player.blockPosition());
    }

    /** Environmental body-equivalent temperature (-100 cold .. 0 comfortable .. +100 hot) at a position —
     *  used by systems that care about a spot's warmth, e.g. how fast food spoils there (§4.2). */
    public static float environmentTempAt(Level level, BlockPos pos) {
        return (ambientTemperatureAt(level, pos) - NEUTRAL_AMBIENT) * TEMP_SCALE;
    }

    /** Ambient temperature (biome scale) at an arbitrary position — the position-based core of the above. */
    public static float ambientTemperatureAt(Level level, BlockPos pos) {
        boolean roofed = !level.canSeeSky(pos); // a block/roof/canopy overhead — you're under cover
        float temp = level.getBiome(pos).value().getBaseTemperature(); // biome
        temp += Seasons.temperatureOffset(level);                      // §10 — winter cold, summer hot
        // Underground (roofed, below sea level): NOT a warm refuge. Real caves settle into a constant,
        // near-saturated cold that deepens with depth (§1.3 cave hypothermia). It's survivable while dry
        // and moving, but the wet-chill and shiver code below turns wet-or-still into a real hypothermia
        // risk — and what you're WEARING decides it, since clothing insulation offsets the cold target.
        if (roofed && pos.getY() < level.getSeaLevel()) {
            int depth = level.getSeaLevel() - pos.getY(); // ~metres below sea level
            // The Variable Zone (0–15m): shallow caves still track the surface, just heavily moderated by
            // the surrounding stone — a partial refuge from summer heat and winter cold, easing colder as
            // you descend toward the constant zone.
            float moderated = temp + (NEUTRAL_AMBIENT - temp) * MAX_INSULATION;
            if (depth <= CAVE_VARIABLE_DEPTH) {
                return moderated + (CAVE_COLD - moderated) * (depth / (float) CAVE_VARIABLE_DEPTH);
            }
            // The Constant Cold Zone (15m+) and the deep abyss below it: a damp ~4–12°C chill, locked
            // independent of the season above, growing colder toward bedrock.
            float d = Math.min(1f, (depth - CAVE_VARIABLE_DEPTH) / (float) CAVE_DEEP_DEPTH);
            return CAVE_COLD + (CAVE_DEEP_FLOOR - CAVE_COLD) * d;
        }
        if (roofed) {
            // Under a roof (§5.5): shielded from rain and the cold night sky, and insulated toward comfort.
            // But a bare roof is only a lean-to — the more WALLED you are, the better it holds heat, so a
            // snug enclosed cabin blunts the extremes far more than an open shelter. A fire inside does the rest.
            float insulation = ROOF_INSULATION + (MAX_INSULATION - ROOF_INSULATION) * enclosure(level, pos);
            temp += (NEUTRAL_AMBIENT - temp) * insulation;
        } else {
            if (level.isRainingAt(pos)) {
                temp -= level.isThundering() ? 0.35f : 0.2f; // rain/snow/storm chills the exposed
            }
            if (isBlizzard(level)) {
                temp -= BLIZZARD_CHILL; // a winter storm: driving snow and wind, deadly in the open
            }
            long timeOfDay = level.getOverworldClockTime() % 24000L;
            if (timeOfDay >= 13000L && timeOfDay < 23000L) {
                temp -= NIGHT_CHILL; // clear night sky is colder
            }
        }
        return temp;
    }

    /**
     * How enclosed a roofed spot is, 0 (open lean-to) .. 1 (walled on all sides) — walls block the wind and
     * hold heat, so a snug cabin shelters far better than a bare roof (§5.5). Each of the four sides counts
     * if there's a solid block within {@link #WALL_REACH}.
     */
    private static float enclosure(Level level, BlockPos pos) {
        int walls = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            p.set(pos);
            for (int i = 1; i <= WALL_REACH; i++) {
                p.move(d);
                var state = level.getBlockState(p);
                if (!state.isAir() && !state.getCollisionShape(level, p).isEmpty()) {
                    walls++;
                    break; // this side is sheltered
                }
            }
        }
        return walls / 4f;
    }

    /**
     * How your clothing bends the environment's pull on your body temperature (§1.3/§5.5). Hide/leather
     * layers insulate — in the cold they blunt the chill (up to comfortable, never warmer than the air);
     * in the heat they trap warmth and make it worse. Bare metal plates barely insulate and, under an
     * open sky in the heat, bake you. Returns the adjusted environmental target.
     */
    private static float clothingShift(Player player, float envTarget) {
        float insulation = 0f; // hide layers that trap body heat
        int metal = 0;         // conducting plates
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) {
                continue;
            }
            if (isLeather(worn)) {
                insulation += LEATHER_INSULATION;
            } else if (worn.has(net.minecraft.core.component.DataComponents.EQUIPPABLE)) {
                metal++;
                insulation += METAL_INSULATION;
            }
        }
        if (envTarget < 0f) {
            // Cold: clothing warms you back toward comfort, but can't push you past it.
            return Math.min(0f, envTarget + insulation);
        }
        if (envTarget > 0f) {
            // Heat: layers you can't shed trap warmth, and sun-baked metal adds to it.
            boolean underSun = player.level().canSeeSky(player.blockPosition());
            float sun = underSun ? metal * METAL_SUN_HEAT : 0f;
            return envTarget + insulation * HEAT_TRAP + sun;
        }
        return envTarget;
    }

    /** Leather/hide armour — the insulating layers, as opposed to conducting metal plates. */
    private static boolean isLeather(ItemStack stack) {
        return stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)
            || stack.is(Items.TURTLE_HELMET);
    }

    /** What one jump costs — a base hop plus what your carried load adds to launching yourself, so
     *  hopping up a hill under a heavy pack is genuinely exhausting (§1.4/§5.1). */
    public static float jumpCost(Player player) {
        return JUMP_BASE + Carry.totalWeight(player) * JUMP_PER_KG;
    }

    private static void tick(ServerPlayer player) {
        // Realistic reach, shorter than vanilla 4.5. Creative keeps vanilla values for building.
        setBaseValue(player, Attributes.BLOCK_INTERACTION_RANGE, player.isCreative() ? 4.5 : BLOCK_REACH);
        setBaseValue(player, Attributes.ENTITY_INTERACTION_RANGE, player.isCreative() ? 3.0 : ENTITY_REACH);

        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        float temperature = ambientTemperature(player);

        // Stamina (short-term) + fatigue (medium-term soreness) — §1.4.
        float fatigue = getFatigue(player);
        float effectiveMax = MAX_STAMINA * (1f - fatigue / 100f * FATIGUE_MAX_PENALTY);
        float stamina = getStamina(player);
        boolean exerting = player.isSprinting() || player.swinging || player.isSwimming(); // sprint, swim, mine, fight
        // Only real swimming/floating is work — wading through shallow water (standing on the bottom) is
        // like walking on land, so you recover stamina there instead of "swimming".
        boolean afloat = player.isInWater() && !player.onGround();
        // Keeping a heavy load afloat is exhausting on its own — the more you haul, the faster you tire.
        float swimLoadDrain = 0f;
        if (afloat) {
            swimLoadDrain = Math.max(0f, Carry.totalWeight(player) - SWIM_FREE_WEIGHT) * SWIM_WEIGHT_DRAIN;
        }
        boolean vigor = isVigorous(player); // golden-apple second wind: fast recovery, no soreness
        if (exerting || swimLoadDrain > 0f) {
            float drain = (player.isSprinting() ? SPRINT_DRAIN : (exerting ? SWING_DRAIN : 0f)) + swimLoadDrain;
            stamina -= drain;
            if (!vigor) {
                fatigue = Math.min(100f, fatigue + drain * FATIGUE_GAIN); // vigor spares you the soreness
            }
        } else {
            float regen = player.isShiftKeyDown() ? REST_REGEN * 2f : REST_REGEN; // sit still to recover faster
            if (player.isSleeping()) {
                regen = SLEEP_REGEN; // a night's sleep refills stamina fast (§1.4)
            }
            if (player.getFoodData().getFoodLevel() < 6) {
                regen *= 0.3f; // hungry → poor recovery (§1.4: stamina restores from food)
            }
            if (getBodyTemp(player) <= COLD_SHIVER) {
                regen *= 0.5f; // shivering, stiff — the cold blunts recovery (§1.3)
            }
            if (vigor) {
                regen *= VIGOR_REGEN_MULT; // a second wind — you bounce back fast
            }
            stamina += regen;
            float shed = (player.isSleeping() ? FATIGUE_SLEEP_RECOVER : FATIGUE_RECOVER) * (vigor ? 4f : 1f);
            fatigue = Math.max(0f, fatigue - shed);
        }
        stamina = Math.max(0f, Math.min(effectiveMax, stamina)); // soreness caps how far it refills
        applyStaminaPenalties(player, stamina);
        player.setAttached(STAMINA, stamina);
        player.setAttached(FATIGUE, fatigue);

        // If you're slowed (exhaustion, cold, thirst…) you can't sprint — which also kills the flat
        // sprint-jump boost that otherwise let a slowed player leap faster than they can walk (§1.4).
        if (player.hasEffect(MobEffects.SLOWNESS)) {
            player.setSprinting(false);
        }

        // Thirst — only drains; drink to restore. Faster when exerting, and you sweat the hotter your
        // *body* runs (§1.2/§1.3): the desert, a fire, heavy clothing, and hard exertion all cost water.
        float thirst = getThirst(player);
        float bodyHeat = getBodyTemp(player); // last tick's body temp — already folds in biome/fire/clothing
        float drain = THIRST_DRAIN
            + (player.isSprinting() ? THIRST_DRAIN_SPRINT : 0f)
            + (bodyHeat > 0f ? bodyHeat / 100f * THIRST_DRAIN_SWEAT : 0f);
        thirst = clamp(thirst - drain, MAX_THIRST);
        applyThirstPenalties(player, thirst);
        player.setAttached(THIRST, thirst);

        // Body temperature (§1.3), two influences:
        float bodyTemp = getBodyTemp(player);
        float heat = nearbyHeat(player); // reused below for both warming and drying

        // Wetness — soaked by water/rain, dries over time (faster by a fire); you stay cold while wet.
        int wetness = player.getAttachedOrElse(WETNESS, 0);
        if (player.isInWaterOrRain()) {
            wetness = MAX_WETNESS;
        } else if (wetness > 0) {
            wetness = Math.max(0, wetness - (heat > 5f ? DRY_BY_FIRE : DRY_NATURAL));
        }
        player.setAttached(WETNESS, wetness);
        boolean wet = wetness > 0;

        //  1) Environmental drift — biome + weather + wetness. Slow (minutes) unless you're wet.
        boolean submerged = player.isInWater();
        float envTarget = (temperature - NEUTRAL_AMBIENT) * TEMP_SCALE;
        if (wet) {
            envTarget -= WET_COLD_SHIFT;
        }
        if (submerged) {
            // water always pulls you down to at least "slightly cold", fast; a cold biome/winter
            // still drives it lower (icy water stays deadly).
            envTarget = Math.min(envTarget, WATER_TARGET);
        } else {
            // What you're wearing shifts how the environment reaches you (§1.3/§5.5) — but only in air;
            // soaked clothing insulates nothing.
            envTarget = clothingShift(player, envTarget);
        }
        envTarget = clampTemp(envTarget);
        float envRate = Math.abs(envTarget) > Math.abs(bodyTemp) ? EXPOSURE_RATE : RECOVERY_RATE;
        if (wet && envTarget < bodyTemp) {
            envRate *= 3f; // wet = rapid heat loss — you chill fast (§1.3)
        }
        if (submerged && envTarget < bodyTemp) {
            envRate *= 4f; // submerged = you chill all the way to cold quickly
        }
        bodyTemp = approach(bodyTemp, envTarget, envRate);

        //  2) Radiant heat — nearby fire/lava warms you FAST (seconds) and never cools you.
        if (heat > bodyTemp) {
            bodyTemp = approach(bodyTemp, clampTemp(heat), HEAT_RATE);
        }

        applyTemperatureEffects(player, bodyTemp);
        player.setAttached(BODY_TEMP, bodyTemp);

        // Staying warm costs calories (§1.3): a cold body shivers and burns food to hold its heat —
        // the colder you are, the more you eat just to keep from freezing.
        if (bodyTemp < 0f) {
            player.causeFoodExhaustion(-bodyTemp / 100f * COLD_EXHAUSTION);
        }

        // Slow vitality recovery (§1.5): your body mends over time, but only when fed, hydrated, and
        // NOT actively wounded or ill — a wound or fever has to be dealt with before you heal.
        if (player.getHealth() < player.getMaxHealth()
            && player.getFoodData().getFoodLevel() >= HEAL_FOOD_MIN
            && getThirst(player) >= HEAL_THIRST_MIN
            && !Conditions.isBleeding(player) && !Conditions.isInfected(player) && !Conditions.isSick(player)
            && player.tickCount % HEAL_INTERVAL == 0) {
            player.heal(1.0f);
        }

        // Carried weight → movement penalty (§5.1): heavier load, slower you move.
        Carry.applyWeightMovement(player);

        // Swimming with a heavy load — too heavy and you can't stay afloat in open water; you sink (§5.1).
        // But you can always kick off the bottom to rise about a block — enough to not drown pinned against
        // the floor, and to climb out of water in steps — so the sink only clamps you once you're clear of
        // the bottom. Push up a block, sink back; push up a block, grab the ledge.
        if (player.isInWater() && Carry.totalWeight(player) > SINK_WEIGHT && !nearSolidBelow(player)) {
            Vec3 motion = player.getDeltaMovement();
            float over = Carry.totalWeight(player) - SINK_WEIGHT;
            double sinkRate = -0.045 - Math.min(0.06, over * 0.004); // a slow, unstoppable sink — not a plummet
            if (motion.y > sinkRate) {
                player.setDeltaMovement(motion.x, sinkRate, motion.z);
                player.hurtMarked = true; // force the client to apply the downward pull — no swimming up
            }
        }

        // Push state to the client HUD a few times a second (temperature field carries body temp).
        if (player.tickCount % 10 == 0) {
            ServerPlayNetworking.send(player,
                new SurvivalSyncPayload(stamina, thirst, bodyTemp, Conditions.flags(player), getFatigue(player)));
        }
    }

    private static void applyStaminaPenalties(ServerPlayer player, float stamina) {
        if (stamina <= 0f) {
            player.setSprinting(false); // you can't push past empty
            effect(player, MobEffects.SLOWNESS, 1);
            effect(player, MobEffects.MINING_FATIGUE, 1);
            effect(player, MobEffects.WEAKNESS, 0); // spent = feeble: a bare-handed hit lands for nothing
        } else if (stamina < STAMINA_LOW) {
            effect(player, MobEffects.SLOWNESS, 0);
            effect(player, MobEffects.MINING_FATIGUE, 0);
        }
    }

    private static void applyThirstPenalties(ServerPlayer player, float thirst) {
        if (thirst <= 0f) {
            effect(player, MobEffects.WEAKNESS, 0);
            effect(player, MobEffects.SLOWNESS, 0);
            effect(player, MobEffects.MINING_FATIGUE, 0);
        } else if (thirst < THIRST_LOW) {
            effect(player, MobEffects.SLOWNESS, 0); // thirsty = sluggish, but you can still fight
        }
    }

    private static void applyTemperatureEffects(ServerPlayer player, float bodyTemp) {
        ServerLevel level = (ServerLevel) player.level();
        if (bodyTemp <= HYPOTHERMIA) {
            int severe = bodyTemp <= SEVERE_COLD ? 1 : 0;
            effect(player, MobEffects.SLOWNESS, severe);
            effect(player, MobEffects.MINING_FATIGUE, severe);
            effect(player, MobEffects.WEAKNESS, 0);
            if (bodyTemp <= SEVERE_COLD && player.tickCount % TEMP_DAMAGE_INTERVAL == 0) {
                player.hurtServer(level, level.damageSources().freeze(), 1.0f);
            }
        } else if (bodyTemp >= HEATSTROKE) {
            int severe = bodyTemp >= SEVERE_HOT ? 1 : 0;
            effect(player, MobEffects.SLOWNESS, severe);
            effect(player, MobEffects.WEAKNESS, severe);
            if (bodyTemp >= SEVERE_HOT && player.tickCount % TEMP_DAMAGE_INTERVAL == 0) {
                player.hurtServer(level, level.damageSources().onFire(), 1.0f);
            }
        }

        // Exposure: sustained cold AND wet settles into a chill (§1.3/§1.5). It's the wet-cold combination —
        // soaked clothing insulates nothing — not deep freeze, that makes you ill, and the chill lingers
        // after you've warmed up. Dry off or get to a fire before it takes hold.
        if (bodyTemp <= COLD_SHIVER && player.getAttachedOrElse(WETNESS, 0) > 0
            && player.tickCount % CHILL_INTERVAL == 0 && player.getRandom().nextFloat() < CHILL_CHANCE) {
            Conditions.addSickness(player, Conditions.FOODBORNE_ILLNESS_TICKS / 2);
            player.sendSystemMessage(Component.literal(
                "The wet cold has settled into your chest — you've caught a chill."));
        }

        // A blizzard caught in the open (weather with teeth): the killing cold is already in the ambient
        // above; out under the driving snow it also slows you to a trudge and tells you to get to cover.
        if (isBlizzard(level) && level.canSeeSky(player.blockPosition())) {
            effect(player, MobEffects.SLOWNESS, 0); // whiteout — you can barely push through the wind and snow
            if (player.tickCount % 200 == 0) {
                player.sendSystemMessage(Component.literal(
                    "A blizzard drives snow sideways — get under a roof and to a fire, or the cold will take you."),
                    true);
            }
        }
    }

    /** A blizzard — a winter thunderstorm of driving snow. Deadly cold in the open; any roof breaks the worst. */
    public static boolean isBlizzard(Level level) {
        return level.isThundering() && Seasons.isWinter(level);
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> which, int amplifier) {
        player.addEffect(new MobEffectInstance(which, 40, amplifier, false, false, true));
    }

    private static final int HEAT_RADIUS = 3;

    /** Warmth from nearby fire/lava (§1.3) — the strongest close source wins, with distance falloff. */
    private static float nearbyHeat(Player player) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        float best = 0f;
        for (int dx = -HEAT_RADIUS; dx <= HEAT_RADIUS; dx++) {
            for (int dy = -HEAT_RADIUS; dy <= HEAT_RADIUS; dy++) {
                for (int dz = -HEAT_RADIUS; dz <= HEAT_RADIUS; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    float heat = heatValue(level, cursor, level.getBlockState(cursor));
                    if (heat <= 0f) {
                        continue;
                    }
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    // Gentle falloff so a fire's warmth carries across the whole radius, not just point-blank.
                    float falloff = (float) Math.max(0.0, 1.0 - dist / (HEAT_RADIUS + 2));
                    best = Math.max(best, heat * falloff);
                }
            }
        }
        return best;
    }

    // Heat sources warm you toward a target body temperature. Only lava (or standing in fire) is hot
    // enough to overheat you; a campfire/fire just keeps the chill off — cozy, never heatstroke (§1.3).
    private static float heatValue(Level level, BlockPos pos, BlockState state) {
        var fluid = state.getFluidState().getType();
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 90f; // dangerously hot
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return 22f;
        }
        if (state.is(Blocks.MAGMA_BLOCK)) {
            return 8f;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
            || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
            || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
            return 3f;
        }
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
            if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                BlockEntity be = level.getBlockEntity(pos);
                float fraction = be != null
                    ? Math.min(1f, Campfires.getFuel(be) / (float) Campfires.INITIAL_FUEL) : 1f;
                return 16f * Math.max(0.4f, fraction); // cozy, dwindling as it burns down; never overheats
            }
            return 6f; // lit furnace / smoker / blast furnace
        }
        return 0f;
    }

    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    private static float clampTemp(float value) {
        return Math.max(-100f, Math.min(100f, value));
    }

    private static float clamp(float value, float max) {
        return Math.max(0f, Math.min(max, value));
    }

    private static void setBaseValue(ServerPlayer player, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }
}
