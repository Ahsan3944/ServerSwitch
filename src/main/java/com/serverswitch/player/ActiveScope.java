package com.serverswitch.player;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveScope {
    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();

    private ActiveScope() {}

    public static String get(ServerPlayerEntity player) {
        return ACTIVE.get(player.getUuid());
    }

    public static void set(ServerPlayerEntity player, String scope) {
        if (scope == null || scope.isBlank()) {
            ACTIVE.remove(player.getUuid());
        } else {
            ACTIVE.put(player.getUuid(), scope);
        }
    }

    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }
}
