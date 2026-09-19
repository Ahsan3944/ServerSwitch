package com.serverswitch.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.serverswitch.ServerSwitch;
import com.serverswitch.server.VirtualServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AdminCommands {
    private AdminCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        var root = net.minecraft.server.command.CommandManager.literal("serverswitch")
                .requires(s -> s.hasPermissionLevel(2));

        var server = net.minecraft.server.command.CommandManager.literal("server")
                .then(net.minecraft.server.command.CommandManager.literal("create")
                        .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id=StringArgumentType.getString(ctx,"id");
                                    boolean ok=ServerSwitch.servers().create(id);
                                    ctx.getSource().sendFeedback(() -> Text.literal(ok ? "Created server profile: "+id : "Could not create server profile."), true);
                                    return ok ? 1 : 0;
                                })))
                .then(net.minecraft.server.command.CommandManager.literal("delete")
                        .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word())
                                .then(net.minecraft.server.command.CommandManager.literal("confirm")
                                        .executes(ctx -> {
                                            String id=StringArgumentType.getString(ctx,"id");
                                            boolean ok=ServerSwitch.servers().delete(id);
                                            ctx.getSource().sendFeedback(() -> Text.literal(ok ? "Deleted server profile: "+id : "Server not found."), true);
                                            return ok ? 1 : 0;
                                        }))))
                .then(net.minecraft.server.command.CommandManager.literal("rename")
                        .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word())
                                .then(net.minecraft.server.command.CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
                                            if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
                                            s.displayName=StringArgumentType.getString(ctx,"name");
                                            ServerSwitch.servers().save();
                                            ctx.getSource().sendFeedback(() -> Text.literal("Renamed server."), true); return 1;
                                        }))))
                .then(toggle("enable", true)).then(toggle("disable", false))
                .then(state("lock", true)).then(state("unlock", false))
                .then(hidden("hide", true)).then(hidden("unhide", false))
                .then(net.minecraft.server.command.CommandManager.literal("info")
                        .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
                                    if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                            s.displayName+" ["+s.id+"] enabled="+s.enabled+" locked="+s.locked+" hidden="+s.hidden+
                                            " overworld="+s.overworld+" nether="+s.nether+" end="+s.end), false);
                                    return 1;
                                })))
                .then(net.minecraft.server.command.CommandManager.literal("list")
                        .executes(ctx -> {
                            for(VirtualServer s:ServerSwitch.servers().getServers())
                                ctx.getSource().sendFeedback(() -> Text.literal(s.id+" | "+s.displayName+" | enabled="+s.enabled+" locked="+s.locked+" hidden="+s.hidden), false);
                            return 1;
                        }));

        var world = net.minecraft.server.command.CommandManager.literal("world")
                .then(setWorld("overworld")).then(setWorld("nether")).then(setWorld("end"));

        var spawn = net.minecraft.server.command.CommandManager.literal("spawn")
                .then(net.minecraft.server.command.CommandManager.literal("set")
                        .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity p=ctx.getSource().getPlayer();
                                    VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
                                    if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
                                    s.spawn.world=p.getWorld().getRegistryKey().getValue().toString();
                                    s.spawn.x=p.getX(); s.spawn.y=p.getY(); s.spawn.z=p.getZ();
                                    s.spawn.yaw=p.getYaw(); s.spawn.pitch=p.getPitch();
                                    ServerSwitch.servers().save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("Spawn set for "+s.displayName+"."), true); return 1;
                                })));

        var hub = net.minecraft.server.command.CommandManager.literal("hub")
                .then(net.minecraft.server.command.CommandManager.literal("set")
                        .executes(ctx -> {
                            ServerPlayerEntity p=ctx.getSource().getPlayer();
                            var c=ServerSwitch.servers().config();
                            c.hubWorld=p.getWorld().getRegistryKey().getValue().toString();
                            c.hubX=p.getX(); c.hubY=p.getY(); c.hubZ=p.getZ(); c.hubYaw=p.getYaw(); c.hubPitch=p.getPitch();
                            c.save();
                            ctx.getSource().sendFeedback(() -> Text.literal("HUB spawn set."), true); return 1;
                        }))
                .then(net.minecraft.server.command.CommandManager.literal("border")
                        .then(net.minecraft.server.command.CommandManager.argument("blocks", DoubleArgumentType.doubleArg(16, 30000000))
                                .executes(ctx -> {
                                    ServerSwitch.servers().config().hubBorder=DoubleArgumentType.getDouble(ctx,"blocks");
                                    ServerSwitch.servers().config().save();
                                    ctx.getSource().sendFeedback(() -> Text.literal("HUB border saved."), true); return 1;
                                })))
                .then(net.minecraft.server.command.CommandManager.literal("protect")
                        .executes(ctx -> {
                            ServerSwitch.servers().config().hubProtection=true;
                            ServerSwitch.servers().config().save();
                            ctx.getSource().sendFeedback(() -> Text.literal("HUB protection enabled."), true); return 1;
                        }));

        var direct = net.minecraft.server.command.CommandManager.literal("directjoin")
                .then(net.minecraft.server.command.CommandManager.literal("off").executes(ctx -> {
                    ServerSwitch.servers().config().directJoin=""; ServerSwitch.servers().config().save(); return 1;
                }))
                .then(net.minecraft.server.command.CommandManager.literal("status").executes(ctx -> {
                    String v=ServerSwitch.servers().config().directJoin;
                    ctx.getSource().sendFeedback(() -> Text.literal(v.isBlank()?"Direct join: OFF":"Direct join: "+v), false); return 1;
                }))
                .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
                    String id=StringArgumentType.getString(ctx,"id");
                    if(ServerSwitch.servers().get(id)==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
                    ServerSwitch.servers().config().directJoin=id; ServerSwitch.servers().config().save(); return 1;
                }));

        var recording = net.minecraft.server.command.CommandManager.literal("recording")
                .then(net.minecraft.server.command.CommandManager.literal("start")
                        .then(net.minecraft.server.command.CommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
                            String id=StringArgumentType.getString(ctx,"id");
                            if(ServerSwitch.servers().get(id)==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
                            ServerSwitch.servers().config().recording=true;
                            ServerSwitch.servers().config().recordingServer=id;
                            ServerSwitch.servers().config().directJoin=id;
                            for(VirtualServer s:ServerSwitch.servers().getServers()) if(!s.id.equals(id)) s.locked=true;
                            ServerSwitch.servers().save();
                            ctx.getSource().sendFeedback(() -> Text.literal("Recording mode started for "+id+"."), true); return 1;
                        })))
                .then(net.minecraft.server.command.CommandManager.literal("stop").executes(ctx -> {
                    ServerSwitch.servers().config().recording=false;
                    ServerSwitch.servers().config().recordingServer="";
                    ServerSwitch.servers().config().directJoin="";
                    ServerSwitch.servers().save();
                    ctx.getSource().sendFeedback(() -> Text.literal("Recording mode stopped. Locked servers remain locked until explicitly unlocked."), true); return 1;
                }))
                .then(net.minecraft.server.command.CommandManager.literal("status").executes(ctx -> {
                    var c=ServerSwitch.servers().config();
                    ctx.getSource().sendFeedback(() -> Text.literal(c.recording?"Recording: "+c.recordingServer:"Recording: OFF"), false); return 1;
                }));

        root.then(server).then(world).then(spawn).then(hub).then(direct).then(recording)
            .then(net.minecraft.server.command.CommandManager.literal("status").executes(ctx -> {
                ctx.getSource().sendFeedback(() -> Text.literal("ServerSwitch "+ServerSwitch.VERSION+" | virtual servers="+ServerSwitch.servers().getServers().size()), false); return 1;
            }))
            .then(net.minecraft.server.command.CommandManager.literal("reload").executes(ctx -> {
                ServerSwitch.servers().save();
                ctx.getSource().sendFeedback(() -> Text.literal("Configuration saved. Full reload will be expanded in a later phase."), true); return 1;
            }));
        d.register(root);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> toggle(String cmd, boolean value) {
        return net.minecraft.server.command.CommandManager.literal(cmd).then(net.minecraft.server.command.CommandManager.argument("id",StringArgumentType.word()).executes(ctx -> {
            VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
            if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
            s.enabled=value; ServerSwitch.servers().save(); return 1;
        }));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> state(String cmd, boolean value) {
        return net.minecraft.server.command.CommandManager.literal(cmd).then(net.minecraft.server.command.CommandManager.argument("id",StringArgumentType.word()).executes(ctx -> {
            VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
            if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
            s.locked=value; ServerSwitch.servers().save(); return 1;
        }));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> hidden(String cmd, boolean value) {
        return net.minecraft.server.command.CommandManager.literal(cmd).then(net.minecraft.server.command.CommandManager.argument("id",StringArgumentType.word()).executes(ctx -> {
            VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
            if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
            s.hidden=value; ServerSwitch.servers().save(); return 1;
        }));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> setWorld(String dimension) {
        return net.minecraft.server.command.CommandManager.literal("set")
                .then(net.minecraft.server.command.CommandManager.argument("id",StringArgumentType.word())
                .then(net.minecraft.server.command.CommandManager.argument("world",StringArgumentType.word()).executes(ctx -> {
                    VirtualServer s=ServerSwitch.servers().get(StringArgumentType.getString(ctx,"id"));
                    if(s==null){ctx.getSource().sendError(Text.literal("Server not found."));return 0;}
                    String world=StringArgumentType.getString(ctx,"world");
                    if(ServerSwitch.servers().resolveWorld(world)==null){ctx.getSource().sendError(Text.literal("World is not loaded/available: "+world));return 0;}
                    if(dimension.equals("overworld")) s.overworld=world;
                    if(dimension.equals("nether")) s.nether=world;
                    if(dimension.equals("end")) s.end=world;
                    ServerSwitch.servers().save(); return 1;
                })));
    }
}
