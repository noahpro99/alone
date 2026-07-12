package dev.alone.core;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Butchering (proposal §7.3). A clean kill with a <b>blade</b> lets you work the carcass properly —
 * killing a hide-bearing animal with a knife, sword, axe or spear salvages its <b>raw hide</b>, its
 * <b>bone</b>, and often a strand of <b>sinew</b> (cordage — string) on top of the usual meat, so the whole
 * animal is used and the flint knife and forged blades pay off in the hunt. Beat one to death with your
 * fists (or a pickaxe) and you waste it all. Which animals are butcherable, and what counts as a blade, are
 * datapack tags ({@code alone:hide_bearing} / {@code alone:blades}). Bone feeds bonemeal — and so the
 * farming that keeps scurvy at bay; sinew is animal cordage alongside plant fibre.
 *
 * <p>Crucially the hide comes off <b>green</b>: a fresh kill gives {@link AloneItems#RAW_HIDE}, not leather.
 * A wet raw hide is not usable leather — it will only rot and stiffen. Turning it into leather is days of
 * work on a {@link AloneBlocks#TANNING_RACK}. And the carcass yields its own tanning agent: a lump of
 * {@link AloneItems#ANIMAL_BRAINS}, the authentic brain-tan, because "a beast has just enough brains to tan
 * its own hide." So each kill can roughly tan its own skin — a real, patient 1:1 investment, and leather is
 * no longer a drop but a product of that work.</p>
 */
public final class Hunting {
    private Hunting() {
    }

    public static final TagKey<EntityType<?>> HIDE_BEARING =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "hide_bearing"));
    public static final TagKey<Item> BLADES =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "blades"));

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return;
            }
            if (!entity.getType().builtInRegistryHolder().is(HIDE_BEARING)) {
                return;
            }
            if (!(source.getEntity() instanceof Player player)) {
                return;
            }
            Skills.gain(player, Skills.TRACKING, 1); // you learn the quarry by taking it — blade or not
            if (!player.getMainHandItem().is(BLADES)) {
                return; // no blade → no clean butchering (but the hunt still taught you)
            }
            // Salvage scales with the animal's body: a squirrel or rabbit gives a scrap of pelt and little
            // bone, a mid-size grazer a usable hide, a horse a large one. Bulk ≈ body volume from its size.
            double bulk = entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
            var rng = player.getRandom();
            int hide;
            int bone;
            float sinewChance;
            if (bulk < 0.35) {           // small game (rabbit, squirrel, chicken) — often barely worth skinning
                hide = rng.nextInt(2);           // 0–1 leather
                bone = rng.nextInt(2);           // 0–1 bone
                sinewChance = 0.15f;
            } else if (bulk < 1.6) {     // mid game (pig, sheep, goat, cow, deer) — a proper hide
                hide = 1 + rng.nextInt(2);       // 1–2 leather
                bone = 1 + rng.nextInt(2);       // 1–2 bone
                sinewChance = 0.6f;
            } else {                     // big game (horse, camel) — a large hide and heavy bone
                hide = 2 + rng.nextInt(2);       // 2–3 leather
                bone = 2 + rng.nextInt(2);       // 2–3 bone
                sinewChance = 0.75f;
            }
            if (hide > 0) {
                // A green, wet hide — worthless until it's tanned on a rack (see AloneItems#RAW_HIDE).
                entity.spawnAtLocation(level, new ItemStack(AloneItems.RAW_HIDE, hide));
                // ...and the brains to tan it: enough to work roughly its own hide (the classic brain-tan).
                // Bigger beasts carry a bigger skull, so a large hide comes with a little more tanning agent.
                int brains = Math.max(1, hide - 1);
                entity.spawnAtLocation(level, new ItemStack(AloneItems.ANIMAL_BRAINS, brains));
            }
            if (bone > 0) {
                entity.spawnAtLocation(level, new ItemStack(Items.BONE, bone));
            }
            if (rng.nextFloat() < sinewChance) {
                // Sinew from the tendons — a bit of animal cordage (string), when the carcass yields it.
                entity.spawnAtLocation(level, new ItemStack(Items.STRING, 1));
            }
        });
    }
}
