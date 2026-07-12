package dev.alone.core;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.serialization.Codec;

/**
 * The Alone-style start (see {@code wiki/docs/roadmap.md} — "The start"). Like the show, you begin a new
 * world with <b>nothing</b>, and get to <b>bring a small kit</b> — here <b>two items</b> — chosen from an
 * approved list, read against the <b>biome you woke in</b>. That turns worldgen into a run: a gill net and
 * a tarp read very differently on a cold coast than in a boreal winter.
 *
 * <p>This is the <b>functional, command-driven</b> version of that idea: on your first join to a world you're
 * told your biome and that you may bring two things; <code>/alone loadout</code> lists the options (biome shown),
 * and <code>/alone loadout pick &lt;item&gt;</code> packs one. It's deliberately server-side and testable — a
 * graphical selection screen is a later polish pass over this same foundation (the picks, the biome read,
 * the one-time gate all live here). Picks are stored per player, per world, so it happens once per run.
 */
public final class Loadout {
    private Loadout() {
    }

    /** How many items you may bring (the show's kit, cut to two — see roadmap). */
    private static final int STARTING_PICKS = 2;

    /** Picks remaining for this player this world. {@code -1} = not yet initialised (a brand-new run). */
    public static final AttachmentType<Integer> PICKS =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "loadout_picks"), Codec.INT);

    /** The keys already packed this run (comma-joined) — so you bring two <em>different</em> things, and so
     *  we can read your kit back to you when it's set. */
    public static final AttachmentType<String> TAKEN =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "loadout_taken"), Codec.STRING);

    /** One entry on the approved list: a key you type, a one-line label, and the stack(s) it packs. */
    private record Choice(String key, String label, Supplier<List<ItemStack>> kit) {
    }

    // The approved list. Each is real gear whose mechanic already exists — and each is something that's
    // genuinely hard or slow to come by otherwise, so the pick actually changes your opening (see the
    // roadmap audit). Rations aside, these are the "brought advantage" a contestant would prize.
    private static final List<Choice> CHOICES = List.of(
        new Choice("ferro_rod", "Ferro rod — lights a fire fast, even in the rain",
            () -> List.of(new ItemStack(AloneItems.FERRO_ROD))),
        new Choice("bow", "Takedown bow + 16 arrows — ranged hunting from day one",
            () -> List.of(new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 16))),
        new Choice("pot", "Iron pot — boil water clean and cook, no clay needed",
            () -> List.of(new ItemStack(AloneItems.IRON_POT))),
        new Choice("sleeping_bag", "Warmth-rated sleeping bag — full rest through a freezing night",
            () -> List.of(new ItemStack(AloneItems.SLEEPING_BAG))),
        new Choice("tarp", "3 tarps — waterproof, fireproof shelter you can pitch anywhere",
            () -> List.of(new ItemStack(AloneItems.TARP, 3))),
        new Choice("snare_wire", "12 lengths of snare wire — skip the slow hand-twisting of cordage",
            () -> List.of(new ItemStack(Items.STRING, 12))),
        new Choice("axe", "Felling axe — fell trees and split wood without knapping one first",
            () -> List.of(new ItemStack(Items.IRON_AXE))),
        new Choice("knife", "Field knife — a proper blade for butchering and carving",
            () -> List.of(new ItemStack(AloneItems.FLINT_KNIFE))),
        new Choice("fishing_rod", "Rod, line & hooks — fish the water without building a weir",
            () -> List.of(new ItemStack(Items.FISHING_ROD))),
        new Choice("sewing_kit", "Sewing kit — keep your hide clothing mended",
            () -> List.of(new ItemStack(AloneItems.SEWING_KIT))),
        new Choice("rope", "2 coils of rope — a ready climb line down any cliff",
            () -> List.of(new ItemStack(AloneItems.ROPE, 2))),
        new Choice("rations", "8 cooked rations — a starting stock of food to buy you time",
            () -> List.of(new ItemStack(Items.COOKED_BEEF, 8)))
    );

    public static void init() {
        // First join to a world → set up the two picks and tell the player where they woke.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            int remaining = player.getAttachedOrElse(PICKS, -1);
            if (remaining < 0) {
                remaining = STARTING_PICKS;
                player.setAttached(PICKS, remaining);
            }
            if (remaining > 0) {
                promptOnJoin(player, remaining);
            }
        });

        // The kit is a one-time, per-run choice. Persistent attachments do NOT carry to the new player
        // entity on respawn, so without this a death (or a return from the End) would reset the picks and
        // let you choose two more. Carry the state across every respawn — death and End alike.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, returnFromEnd) -> {
            newPlayer.setAttached(PICKS, oldPlayer.getAttachedOrElse(PICKS, -1));
            newPlayer.setAttached(TAKEN, oldPlayer.getAttachedOrElse(TAKEN, ""));
        });

        // Registered under "alone" (→ "/alone loadout"); Brigadier merges this with AdminCommand's "alone".
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("alone")
                .then(Commands.literal("loadout")
                    .executes(ctx -> list(ctx.getSource()))
                    .then(Commands.literal("pick")
                        .then(Commands.argument("item", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                for (Choice c : CHOICES) {
                                    builder.suggest(c.key());
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> pick(ctx.getSource(), StringArgumentType.getString(ctx, "item"))))))));
    }

    /** The welcome on first join: your biome, what it will do to you, and that you may bring two things. */
    private static void promptOnJoin(ServerPlayer player, int remaining) {
        player.sendSystemMessage(Component.literal(
            "You wake in " + biomeName(player) + " with nothing but the clothes you're in."));
        player.sendSystemMessage(Component.literal(biomeAdvice(player)));
        player.sendSystemMessage(Component.literal(
            "You may bring " + remaining + " item" + (remaining == 1 ? "" : "s") + " from home.  ")
            .append(Component.literal("[Choose your kit]").withStyle(s -> s
                .withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/alone loadout")))));
    }

    /** {@code /alone loadout} — show the biome, how many picks are left, and the whole approved list. */
    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int remaining = Math.max(0, initialised(player));
        if (remaining <= 0) {
            source.sendSuccess(() -> Component.literal("Your kit is already set — this was a one-time choice."), false);
            return Command.SINGLE_SUCCESS;
        }
        final int left = remaining;
        source.sendSuccess(() -> Component.literal(
            "You woke in " + biomeName(player) + ". " + biomeAdvice(player)), false);
        source.sendSuccess(() -> Component.literal(
            "You may still pack " + left + " item" + (left == 1 ? "" : "s")
                + " — click one to pack it (or type /alone loadout pick <name>):"), false);
        for (Choice c : CHOICES) {
            Component line = Component.literal("  ▶ " + c.label()).withStyle(s -> s
                .withColor(ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/alone loadout pick " + c.key())));
            source.sendSuccess(() -> line, false);
        }
        return Command.SINGLE_SUCCESS;
    }

    /** {@code /alone loadout pick <item>} — pack one item, if you've a pick left. */
    private static int pick(CommandSourceStack source, String key) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int remaining = initialised(player);
        if (remaining <= 0) {
            source.sendFailure(Component.literal("Your kit is already set — nothing left to pack."));
            return 0;
        }
        Choice choice = null;
        for (Choice c : CHOICES) {
            if (c.key().equals(key)) {
                choice = c;
                break;
            }
        }
        if (choice == null) {
            source.sendFailure(Component.literal("No such item '" + key + "'. Run /alone loadout to see the list."));
            return 0;
        }
        // Bring two DIFFERENT things — you wouldn't pack two identical tools.
        String taken = player.getAttachedOrElse(TAKEN, "");
        if (!taken.isEmpty() && (("," + taken + ",").contains("," + key + ","))) {
            source.sendFailure(Component.literal("You've already packed that — bring something different."));
            return 0;
        }
        for (ItemStack stack : choice.kit().get()) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false); // no room — it falls at your feet
            }
        }
        String newTaken = taken.isEmpty() ? key : taken + "," + key;
        player.setAttached(TAKEN, newTaken);
        remaining--;
        player.setAttached(PICKS, remaining);
        final Choice packed = choice;
        final int left = remaining;
        source.sendSuccess(() -> Component.literal("Packed: " + packed.label() + "."
            + (left > 0 ? " " + left + " pick left." : "")), false);
        if (left == 0) {
            source.sendSuccess(() -> Component.literal(
                "Your kit is set: " + prettyKit(newTaken) + ". Good luck — you'll get no more."), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    /** Read a comma-joined key list back as a human phrase, e.g. "ferro rod and sleeping bag". */
    private static String prettyKit(String taken) {
        String[] keys = taken.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                sb.append(i == keys.length - 1 ? " and " : ", ");
            }
            sb.append(keys[i].replace('_', ' '));
        }
        return sb.toString();
    }

    /** The strategic read of the land: what this biome will do to you, so the pick actually means something. */
    private static String biomeAdvice(ServerPlayer player) {
        var biome = player.level().getBiome(player.blockPosition()).value();
        float temp = biome.getBaseTemperature();
        boolean rains = biome.hasPrecipitation();
        if (temp <= 0.2f) {
            return "It's cold country — the nights are the enemy here: warmth and a fire that won't fail "
                + "in the wet will decide whether you wake up.";
        }
        if (temp >= 1.5f && !rains) {
            return "Hot and dry — water, not cold, is what kills here: a way to boil what little you find, "
                + "and food to cross the barrens, matter most.";
        }
        if (temp >= 1.0f) {
            return "Warm country — cold won't trouble you, so fight for food instead: game and fish are "
                + "there for whoever can reach them.";
        }
        return "Temperate woodland — mild enough to rough it, so press your edge: game, timber and fire "
            + "are all within reach.";
    }

    /** Read the picks left, initialising a brand-new player to the full allowance. */
    private static int initialised(ServerPlayer player) {
        int remaining = player.getAttachedOrElse(PICKS, -1);
        if (remaining < 0) {
            remaining = STARTING_PICKS;
            player.setAttached(PICKS, remaining);
        }
        return remaining;
    }

    /** A readable name for the biome the player is standing in — the strategic read of the start. */
    private static String biomeName(ServerPlayer player) {
        return player.level().getBiome(player.blockPosition())
            .unwrapKey().map(k -> k.identifier().getPath().replace('_', ' ')).orElse("the wild");
    }
}
