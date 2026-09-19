package com.serverswitch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.serverswitch.ServerSwitch;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class NetworkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String version = "1";
    public String hubWorld = "minecraft:overworld";
    public double hubX = 0.5, hubY = 80, hubZ = 0.5;
    public float hubYaw = 0, hubPitch = 0;
    public double hubBorder = 640;
    public boolean hubProtection = true;
    public String directJoin = "";
    public String recordingServer = "";
    public boolean recording = false;
    private transient Path file;

    public static NetworkConfig load(MinecraftServer server) {
        Path dir = server.getRunDirectory().toPath().resolve("config").resolve(ServerSwitch.MOD_ID);
        Path file = dir.resolve("config.json");
        NetworkConfig cfg;
        try {
            Files.createDirectories(dir);
            cfg = Files.exists(file) ? GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), NetworkConfig.class) : new NetworkConfig();
            if (cfg == null) cfg = new NetworkConfig();
        } catch (Exception e) {
            ServerSwitch.LOGGER.error("Could not load config.", e);
            cfg = new NetworkConfig();
        }
        cfg.file = file;
        cfg.save();
        return cfg;
    }
    public void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("config.json.tmp");
            Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { ServerSwitch.LOGGER.error("Could not save config.", e); }
    }
}
