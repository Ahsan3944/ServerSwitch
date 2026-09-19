package com.serverswitch;

import net.fabricmc.api.ModInitializer;

public final class ServerSwitchEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerSwitch.init();
    }
}
