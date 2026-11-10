package com.walnutbeachdigital.joulesmith.guide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests.Chapter;
import com.walnutbeachdigital.joulesmith.guide.HandbookQuests.Quest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /joulesmith handbook ... (operators only):
 * <ul>
 *   <li>unlock &lt;chapter|all&gt; [players]: opens a chapter by ticking off the quest that opens it</li>
 *   <li>complete &lt;chapter|all&gt; [players]: opens a chapter and ticks off every quest in it</li>
 *   <li>lock &lt;chapter|all&gt; [players]: closes a chapter and clears its quests</li>
 *   <li>reset [players]: clears all Handbook progress</li>
 * </ul>
 * Players default to whoever runs the command.
 */
public final class HandbookCommand {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.joulesmith.handbook.unknown", name));

    private enum Action { UNLOCK, COMPLETE, LOCK }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var handbook = Commands.literal("handbook");
        for (Action action : Action.values()) {
            handbook.then(Commands.literal(action.name().toLowerCase(Locale.ROOT))
                    .then(Commands.argument("chapter", StringArgumentType.word())
                            .suggests((ctx, b) -> SharedSuggestionProvider.suggest(chapterNames(), b))
                            .executes(ctx -> apply(ctx, action, List.of(ctx.getSource().getPlayerOrException())))
                            .then(Commands.argument("players", EntityArgument.players())
                                    .executes(ctx -> apply(ctx, action, EntityArgument.getPlayers(ctx, "players"))))));
        }
        handbook.then(Commands.literal("reset")
                .executes(ctx -> reset(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> reset(ctx, EntityArgument.getPlayers(ctx, "players")))));
        dispatcher.register(Commands.literal("joulesmith").requires(s -> s.hasPermission(2)).then(handbook));
    }

    private static List<String> chapterNames() {
        List<String> names = new ArrayList<>();
        names.add("all");
        for (Chapter c : Chapter.values()) names.add(c.id());
        return names;
    }

    private static List<Chapter> chapters(String name) throws CommandSyntaxException {
        if (name.equals("all")) return List.of(Chapter.values());
        for (Chapter c : Chapter.values()) if (c.id().equals(name)) return List.of(c);
        throw UNKNOWN.create(name);
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, Action action, Collection<ServerPlayer> players)
            throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "chapter");
        List<Chapter> chapters = chapters(name);
        for (ServerPlayer player : players) {
            Set<String> done = HandbookProgress.done(player);
            for (Chapter c : chapters) {
                switch (action) {
                    case UNLOCK -> { if (c.opener != null) done.add(c.opener); }
                    case COMPLETE -> {
                        if (c.opener != null) done.add(c.opener);
                        for (Quest q : HandbookQuests.chapter(c)) done.add(q.id());
                    }
                    case LOCK -> {
                        if (c.opener != null) done.remove(c.opener);
                        for (Quest q : HandbookQuests.chapter(c)) done.remove(q.id());
                    }
                }
            }
            HandbookProgress.set(player, done);
        }
        Component what = name.equals("all") ? Component.translatable("commands.joulesmith.handbook.all") : chapters.get(0).displayName();
        String key = "commands.joulesmith.handbook." + action.name().toLowerCase(Locale.ROOT);
        ctx.getSource().sendSuccess(() -> Component.translatable(key, what, players.size()), true);
        return players.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) HandbookProgress.set(player, Set.of());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.joulesmith.handbook.reset", players.size()), true);
        return players.size();
    }

    private HandbookCommand() {}
}
