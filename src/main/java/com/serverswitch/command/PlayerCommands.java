package com.serverswitch.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.serverswitch.ServerSwitch;
import com.serverswitch.server.VirtualServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PlayerCommands {
    private PlayerCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(net.minecraft.server.command.CommandManager.literal("hub")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (!ServerSwitch.servers().sendToHub(player)) {
                        ctx.getSource().sendError(Text.literal("HUB is not configured or its world is unavailable."));
                        return 0;
                    }
                    ctx.getSource().sendFeedback(() -> Text.literal("Returned to HUB."), false);
                    return 1;
                }));

        dispatcher.register(net.minecraft.server.command.CommandManager.literal("server")
                .executes(ctx -> {
                    var servers = ServerSwitch.servers().getServers().stream()
                            .filter(s -> s.enabled && !s.hidden)
                            .toList();
                    if (servers.isEmpty()) {
                        ctx.getSource().sendFeedback(() -> Text.literal("No public servers are currently available."), false);
                        return 1;
                    }
                    ctx.getSource().sendFeedback(() -> Text.literal("Available servers:"), false);
                    for (VirtualServer s : servers) {
                        String state = s.locked ? " [LOCKED]" : "";
                        ctx.getSource().sendFeedback(() -> Text.literal("- " + s.displayName + state + " (" + s.id + ")"), false);
                    }
                    return 1;
                })
                .then(net.minecraft.server.command.CommandManager.argument("server", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (VirtualServer s : ServerSwitch.servers().getServers()) {
                                if (s.enabled && (!s.hidden || ctx.getSource().hasPermissionLevel(2))) builder.suggest(s.id);
                            }
                            builder.suggest("hub");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "server");
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if ("hub".equalsIgnoreCase(id)) {
                                return ServerSwitch.servers().sendToHub(player) ? 1 : 0;
                            }
                            VirtualServer target = ServerSwitch.servers().get(id);
                            if (!ServerSwitch.servers().canJoin(player, target)) {
                                ctx.getSource().sendError(Text.literal("That server is unavailable."));
                                return 0;
                            }
                            if (!ServerSwitch.servers().switchPlayer(player, id)) {
                                ctx.getSource().sendError(Text.literal("Could not switch servers. Check the server world configuration."));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal("Connected to " + target.displayName + "."), false);
                            return 1;
                        })));

        dispatcher.register(net.minecraft.server.command.CommandManager.literal("server")
                .then(net.minecraft.server.command.CommandManager.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(() -> Text.literal("/server - list servers"), false);
                            ctx.getSource().sendFeedback(() -> Text.literal("/server <name> - join a server"), false);
                            ctx.getSource().sendFeedback(() -> Text.literal("/hub - return to HUB"), false);
                            return 1;
                        }))
                .then(net.minecraft.server.command.CommandManager.literal("version")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(() -> Text.literal("ServerSwitch " + ServerSwitch.VERSION + " | Minecraft 1.20.1"), true);
                            return 1;
                        })));
    }
}
