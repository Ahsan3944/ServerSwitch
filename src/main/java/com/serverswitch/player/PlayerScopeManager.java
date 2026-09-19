package com.serverswitch.player;

import com.serverswitch.ServerSwitch;
import com.serverswitch.server.ServerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import java.io.IOException;
import java.nio.file.*;

public final class PlayerScopeManager {
    private final MinecraftServer server;
    public PlayerScopeManager(MinecraftServer server, ServerRegistry ignored) { this.server=server; }

    public void saveCurrent(ServerPlayerEntity player) {
        String scope=ActiveScope.get(player);
        if(scope==null||scope.isBlank()) scope="hub";
        NbtCompound data=new NbtCompound();
        data.put("inventory",player.getInventory().writeNbt(new NbtList()));
        data.putFloat("health",player.getHealth());
        data.putInt("food",player.getHungerManager().getFoodLevel());
        data.putFloat("saturation",player.getHungerManager().getSaturationLevel());
        data.putInt("experienceLevel",player.experienceLevel);
        data.putFloat("experienceProgress",player.experienceProgress);
        data.putInt("experienceTotal",player.totalExperience);
        data.putInt("selectedSlot",player.getInventory().selectedSlot);
        write(player,scope,data);
    }
    public void loadFor(ServerPlayerEntity player,String scope) {
        Path path=file(player,scope);
        if(!Files.exists(path)) { player.getInventory().clear(); player.setHealth(player.getMaxHealth()); player.getHungerManager().setFoodLevel(20); player.getHungerManager().setSaturationLevel(5); player.experienceLevel=0; player.experienceProgress=0; player.totalExperience=0; return; }
        try {
            NbtCompound data=net.minecraft.nbt.NbtIo.read(path.toFile());
            player.getInventory().clear();
            if(data.contains("inventory",9)) player.getInventory().readNbt(data.getList("inventory",10));
            player.setHealth(Math.min(player.getMaxHealth(),Math.max(1.0f,data.getFloat("health"))));
            player.getHungerManager().setFoodLevel(Math.max(0,Math.min(20,data.getInt("food"))));
            player.getHungerManager().setSaturationLevel(Math.max(0,data.getFloat("saturation")));
            player.experienceLevel=Math.max(0,data.getInt("experienceLevel"));
            player.experienceProgress=Math.max(0,Math.min(1,data.getFloat("experienceProgress")));
            player.totalExperience=Math.max(0,data.getInt("experienceTotal"));
            player.getInventory().selectedSlot=Math.max(0,Math.min(8,data.getInt("selectedSlot")));
            player.playerScreenHandler.sendContentUpdates();
        } catch(IOException|RuntimeException e){ ServerSwitch.LOGGER.error("Could not load player scope "+scope+" for "+player.getUuid(),e); }
    }
    public void setActive(ServerPlayerEntity p,String scope){ ActiveScope.set(p,scope); }
    private void write(ServerPlayerEntity p,String scope,NbtCompound data){
        try {
            Path path=file(p,scope); Files.createDirectories(path.getParent()); Path tmp=path.resolveSibling(path.getFileName()+".tmp");
            net.minecraft.nbt.NbtIo.write(data,tmp.toFile());
            try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException e){ServerSwitch.LOGGER.error("Could not save player scope "+scope+" for "+p.getUuid(),e);}
    }
    private Path file(ServerPlayerEntity p,String scope){
        return server.getSavePath(WorldSavePath.ROOT).resolve("serverswitch").resolve("players").resolve(p.getUuidAsString()).resolve(scope+".nbt");
    }
}
