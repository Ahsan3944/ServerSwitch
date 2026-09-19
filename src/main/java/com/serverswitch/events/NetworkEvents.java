package com.serverswitch.events;

import com.serverswitch.ServerSwitch;
import com.serverswitch.player.ActiveScope;
import com.serverswitch.server.VirtualServer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NetworkEvents {
    private NetworkEvents() {}
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String direct = ServerSwitch.servers().config().directJoin;
            if (ServerSwitch.servers().config().recording && !direct.isBlank()) {
                if (!ServerSwitch.servers().switchPlayer(player, direct)) ServerSwitch.servers().sendToHub(player);
            } else {
                ServerSwitch.servers().sendToHub(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerSwitch.servers().playerScopes().saveCurrent(player);
            ActiveScope.clear(player);
        });
    }
}
