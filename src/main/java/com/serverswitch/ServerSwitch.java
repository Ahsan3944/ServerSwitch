package com.serverswitch;

import com.serverswitch.command.AdminCommands;
import com.serverswitch.command.PlayerCommands;
import com.serverswitch.config.NetworkConfig;
import com.serverswitch.events.NetworkEvents;
import com.serverswitch.hub.HubManager;
import com.serverswitch.server.ServerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerSwitch {
    public static final String MOD_ID = "serverswitch";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ServerManager serverManager;
    private static HubManager hubManager;

    public static void init() {
        NetworkEvents.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            NetworkConfig config = NetworkConfig.load(server);
            serverManager = new ServerManager(server, config);
            hubManager = new HubManager(serverManager);
            serverManager.ensureDefaults();
            LOGGER.info("ServerSwitch {} initialized.", VERSION);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { if (serverManager != null) serverManager.save(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if (serverManager != null) serverManager.tick(); });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PlayerCommands.register(dispatcher);
            AdminCommands.register(dispatcher);
        });
    }

    public static ServerManager servers() { if (serverManager == null) throw new IllegalStateException("ServerSwitch not ready"); return serverManager; }
    public static HubManager hub() { if (hubManager == null) throw new IllegalStateException("ServerSwitch not ready"); return hubManager; }
}