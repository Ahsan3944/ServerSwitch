package com.serverswitch.server;

import com.serverswitch.ServerSwitch;
import com.serverswitch.config.NetworkConfig;
import com.serverswitch.player.ActiveScope;
import com.serverswitch.player.PlayerScopeManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

public final class ServerManager {
    private final MinecraftServer server;
    private final NetworkConfig config;
    private final ServerRegistry registry;
    private final PlayerScopeManager playerScopes;

    public ServerManager(MinecraftServer server, NetworkConfig config) {
        this.server = server;
        this.config = config;
        this.registry = ServerRegistry.load(server);
        this.playerScopes = new PlayerScopeManager(server, registry);
    }

    public MinecraftServer minecraftServer() { return server; }
    public NetworkConfig config() { return config; }
    public ServerRegistry registry() { return registry; }
    public PlayerScopeManager playerScopes() { return playerScopes; }

    public List<VirtualServer> getServers() {
        return registry.values().stream()
                .sorted(Comparator.comparingInt(s -> s.order))
                .toList();
    }

    public VirtualServer get(String id) {
        return registry.get(id);
    }

    public boolean create(String id) {
        return registry.create(id);
    }

    public boolean delete(String id) {
        if (id == null || id.equalsIgnoreCase("hub")) return false;
        return registry.delete(id);
    }

    public void ensureDefaults() {
        if (registry.values().isEmpty()) {
            registry.create("main");
            VirtualServer main = registry.get("main");
            if (main != null) {
                main.displayName = "Main";
                main.overworld = config.hubWorld;
                main.spawn.world = config.hubWorld;
                main.spawn.x = config.hubX;
                main.spawn.y = config.hubY;
                main.spawn.z = config.hubZ;
                registry.save();
            }
        }
    }

    public ServerWorld resolveWorld(String worldId) {
        if (worldId == null || worldId.isBlank()) return null;
        Identifier id = Identifier.tryParse(worldId);
        if (id == null) return null;
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, id);
        return server.getWorld(key);
    }

    public boolean canJoin(ServerPlayerEntity player, VirtualServer target) {
        if (target == null || !target.enabled) return false;
        if ((target.hidden || target.locked) && !player.hasPermissionLevel(2)) return false;
        return resolveWorld(target.spawn.world.isBlank() ? target.overworld : target.spawn.world) != null;
    }

    public boolean switchPlayer(ServerPlayerEntity player, String id) {
        VirtualServer target = get(id);
        if (!canJoin(player, target)) return false;

        ServerWorld destination = resolveWorld(
                target.spawn.world == null || target.spawn.world.isBlank()
                        ? target.overworld
                        : target.spawn.world
        );
        if (destination == null) return false;

        String current = ActiveScope.get(player);
        if (current == null || current.isBlank()) current = "hub";

        if (current.equals(target.id)) {
            return true;
        }

        playerScopes.saveCurrent(player);
        playerScopes.loadFor(player, target.id);

        VirtualServer.SpawnPoint spawn = target.spawn;
        player.teleport(destination, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);
        ActiveScope.set(player, target.id);

        player.sendMessage(net.minecraft.text.Text.literal(
                "§7[ServerSwitch] §fConnected to §b" + target.displayName + "§f."
        ), false);
        return true;
    }

    public boolean sendToHub(ServerPlayerEntity player) {
        ServerWorld destination = resolveWorld(config.hubWorld);
        if (destination == null) return false;

        playerScopes.saveCurrent(player);
        playerScopes.loadFor(player, "hub");

        player.teleport(destination, config.hubX, config.hubY, config.hubZ,
                config.hubYaw, config.hubPitch);
        ActiveScope.set(player, "hub");
        return true;
    }

    public void save() {
        registry.save();
        config.save();
    }

    public void tick() {
        // Reserved for transition recovery, autosave and future lifecycle tasks.
    }
}
