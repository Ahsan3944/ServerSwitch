package com.serverswitch.server;
import java.util.LinkedHashMap;
import java.util.Map;
public final class VirtualServer {
    public String id;
    public String displayName;
    public String description = "";
    public boolean enabled = true, locked = false, hidden = false;
    public int order = 0;
    public String icon = "PLAYER_HEAD";
    public String overworld = "";
    public String nether = "";
    public String end = "";
    public SpawnPoint spawn = new SpawnPoint();
    public Map<String, Boolean> rules = new LinkedHashMap<>();
    public VirtualServer() {}
    public VirtualServer(String id) { this.id = id; this.displayName = id; }
    public static final class SpawnPoint {
        public String world = "";
        public double x = 0.5, y = 80, z = 0.5;
        public float yaw = 0, pitch = 0;
    }
}
