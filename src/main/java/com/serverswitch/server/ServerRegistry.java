package com.serverswitch.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serverswitch.ServerSwitch;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ServerRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Path serverDirectory;
    private final Map<String, VirtualServer> servers = new LinkedHashMap<>();
    private ServerRegistry(Path file) { this.file = file; this.serverDirectory = file.getParent().getParent().getParent().resolve("serverswitch").resolve("servers"); }

    public static ServerRegistry load(MinecraftServer server) {
        Path dir = server.getRunDirectory().toPath().resolve("config").resolve(ServerSwitch.MOD_ID);
        Path file = dir.resolve("servers.json");
        ServerRegistry registry = new ServerRegistry(file);
        try {
            Files.createDirectories(dir);
            if (Files.exists(file)) {
                VirtualServer[] loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), VirtualServer[].class);
                if (loaded != null) for (VirtualServer s : loaded) {
                    if (s != null && isValidId(s.id)) {
                        if (s.displayName == null || s.displayName.isBlank()) s.displayName = s.id;
                        registry.servers.put(s.id, s);
                    }
                }
            }
            registry.save();
        } catch (Exception e) { ServerSwitch.LOGGER.error("Could not load servers.json.", e); }
        return registry;
    }
    public Collection<VirtualServer> values() { return Collections.unmodifiableCollection(servers.values()); }
    public VirtualServer get(String id) { return id == null ? null : servers.get(id.toLowerCase(Locale.ROOT)); }
    public boolean create(String id) {
        id = id.toLowerCase(Locale.ROOT);
        if (!isValidId(id) || servers.containsKey(id)) return false;
        VirtualServer s = new VirtualServer(id); s.order = servers.size(); servers.put(id, s); save(); return true;
    }
    public boolean delete(String id) { if (id == null || servers.remove(id.toLowerCase(Locale.ROOT)) == null) return false; normalizeOrder(); save(); return true; }
    public void normalizeOrder() { int i=0; for (VirtualServer s: servers.values()) s.order=i++; }
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp=file.resolveSibling("servers.json.tmp");
            Files.writeString(tmp, GSON.toJson(servers.values()), StandardCharsets.UTF_8);
            try { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(IOException e){ Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
        } catch(IOException e){ ServerSwitch.LOGGER.error("Could not save servers.json.",e); }
    }
    public static boolean isValidId(String id){ return id != null && id.matches("[a-z0-9][a-z0-9_-]{0,31}"); }
}
