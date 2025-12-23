package de.tomalbrc.blockboy_arcade.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.tomalbrc.blockboy_arcade.BlockBoyArcade;
import de.tomalbrc.blockboy_arcade.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BlockBoyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> blockboy = literal("blockboy-arcade").requires(Permissions.require("blockboy-arcade.command", 1));

        blockboy.then(literal("link").then(argument("friend", StringArgumentType.word()).suggests(new PlayingPlayerSuggestionProvider()).executes(x -> {
            var player = x.getSource().getPlayer();
            if (player == null) {
                x.getSource().sendFailure(Component.literal("Must be ran from a player!"));
            }

            String name = StringArgumentType.getString(x, "friend");
            Player friend = player.level().getServer().getPlayerList().getPlayerByName(name);

            if (friend != null) {
                var s1 = BlockBoyArcade.ACTIVE_SESSIONS.get(player);
                var s2 = BlockBoyArcade.ACTIVE_SESSIONS.get(friend);
                if (s2 == null) {
                    player.sendSystemMessage(Component.literal("Could not connect with " + name));
                    return 1;
                }

                try {
                    assert s1.getController() != null;
                    assert s2.getController() != null;

                    s1.getController().link(s2.getController());
                } catch (IOException e) {
                    player.sendSystemMessage(Component.literal("Could not connect with " + name));
                    BlockBoyArcade.LOGGER.error("Could not link: {}", e.getLocalizedMessage());
                }
            }

            return 0;
        })));

        blockboy.then(literal("unlink").then(argument("friend", StringArgumentType.word()).suggests(new PlayingPlayerSuggestionProvider()).executes(x -> {
            var player = x.getSource().getPlayer();
            if (player == null) {
                x.getSource().sendFailure(Component.literal("Must be ran from a player!"));
            }

            var name = StringArgumentType.getString(x, "friend");
            var friend = player.level().getServer().getPlayerList().getPlayerByName(name);

            if (friend != null) {
                var s1 = BlockBoyArcade.ACTIVE_SESSIONS.get(player);
                try {
                    assert s1.getController() != null;
                    s1.getController().unlink();
                } catch (IOException e) {
                    player.sendSystemMessage(Component.literal("Could not unlink, are you linked with someone?"));
                    BlockBoyArcade.LOGGER.error("Could not unlink: {}", e.getLocalizedMessage());
                }
            }

            return 0;
        })));

        dispatcher.getRoot().addChild(blockboy.build());
    }

    public static class PlayingPlayerSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) {

            for (Player p : BlockBoyArcade.ACTIVE_SESSIONS.keySet()) {
                if (p != context.getSource().getPlayer()) builder.suggest(p.getDisplayName().getString());
            }

            return builder.buildFuture();
        }
    }
}
