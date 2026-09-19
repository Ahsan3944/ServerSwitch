package com.serverswitch.hub;

import com.serverswitch.ServerSwitch;
import com.serverswitch.server.ServerManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HubManager {
    private final ServerManager servers;
    public HubManager(ServerManager servers) {
        this.servers = servers;
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                isProtected(player) ? false : true);
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                isProtected(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                isProtected(player) ? ActionResult.FAIL : ActionResult.PASS);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity player && isProtected(player)));
    }
    private boolean isProtected(net.minecraft.entity.player.PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        if (!servers.config().hubProtection) return false;
        return sp.getWorld().getRegistryKey().getValue().toString().equals(servers.config().hubWorld);
    }
}
