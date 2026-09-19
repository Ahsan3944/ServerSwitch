package com.serverswitch.gui;

import com.serverswitch.ServerSwitch;
import com.serverswitch.server.VirtualServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ServerSelector {
    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;

    private ServerSelector() {}

    public static void open(ServerPlayerEntity player) {
        List<VirtualServer> visible = ServerSwitch.servers().getServers().stream()
                .filter(s -> s.enabled && (!s.hidden || player.hasPermissionLevel(2)))
                .limit(45)
                .toList();

        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> createHandler(syncId, inventory, player, visible),
                Text.literal("ServerSwitch")
        );
        player.openHandledScreen(factory);
    }

    private static ScreenHandler createHandler(
            int syncId,
            PlayerInventory playerInventory,
            ServerPlayerEntity player,
            List<VirtualServer> servers
    ) {
        SimpleInventory inventory = new SimpleInventory(SIZE);

        // Server entries: first 45 slots.
        for (int i = 0; i < servers.size(); i++) {
            VirtualServer server = servers.get(i);
            ItemStack icon = icon(server.icon);
            icon.setCustomName(Text.literal(server.displayName));
            inventory.setStack(i, icon);
        }

        // HUB button.
        ItemStack hub = new ItemStack(Items.COMPASS);
        hub.setCustomName(Text.literal("HUB"));
        inventory.setStack(49, hub);

        // Information button.
        ItemStack info = new ItemStack(Items.PAPER);
        info.setCustomName(Text.literal("ServerSwitch"));
        inventory.setStack(53, info);

        inventory.markDirty();

        return new SelectorHandler(syncId, playerInventory, inventory, servers);
    }

    private static ItemStack icon(String value) {
        if (value != null && !value.isBlank()) {
            Identifier id = Identifier.tryParse(value.contains(":") ? value : "minecraft:" + value.toLowerCase());
            if (id != null) {
                Item item = Registries.ITEM.get(id);
                if (item != Items.AIR) return new ItemStack(item);
            }
        }
        return new ItemStack(Items.PLAYER_HEAD);
    }

    private static final class SelectorHandler extends GenericContainerScreenHandler {
        private final ServerPlayerEntity player;
        private final List<String> serverIds;

        private SelectorHandler(
                int syncId,
                PlayerInventory playerInventory,
                SimpleInventory inventory,
                List<VirtualServer> servers
        ) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, ROWS);
            this.player = player;
            this.serverIds = new ArrayList<>(servers.stream().map(s -> s.id).toList());
        }

        @Override
        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clicker) {
            if (!(clicker instanceof ServerPlayerEntity serverPlayer)) return;

            if (slotIndex >= 0 && slotIndex < 45 && slotIndex < serverIds.size()) {
                String id = serverIds.get(slotIndex);
                if (!ServerSwitch.servers().switchPlayer(serverPlayer, id)) {
                    serverPlayer.sendMessage(Text.literal("That server is currently unavailable."), true);
                } else {
                    serverPlayer.closeHandledScreen();
                }
                return;
            }

            if (slotIndex == 49) {
                if (ServerSwitch.servers().sendToHub(serverPlayer)) {
                    serverPlayer.closeHandledScreen();
                } else {
                    serverPlayer.sendMessage(Text.literal("HUB is unavailable."), true);
                }
                return;
            }

            // This is a selector, not an inventory. Block every other slot interaction.
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return player.isAlive();
        }

        @Override
        public void onClosed(PlayerEntity player) {
            // Selector inventory is virtual and must never drop its display items.
        }
    }
}
