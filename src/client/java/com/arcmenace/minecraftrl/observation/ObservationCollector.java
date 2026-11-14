package com.arcmenace.minecraftrl.observation;

import com.arcmenace.minecraftrl.player.InventoryObserver;
import com.arcmenace.minecraftrl.player.PlayerObserver;
import com.arcmenace.minecraftrl.player.PlayerState;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ObservationCollector {

    public static JsonObject collect(Minecraft client, long clientTick) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "observation");
        root.addProperty("tick", clientTick);

        if (client == null || client.player == null || client.level == null) {
            root.addProperty("status", "waiting_for_player");
            return root;
        }

        root.addProperty("status", "active");

        LocalPlayer player = client.player;

        PlayerState playerState = PlayerObserver.extract(player);
        if (playerState != null) {
            root.add("player", playerState.toJson());
        }

        root.add("inventory", InventoryObserver.extract(player));
        root.add("world", WorldObserver.extract(client.level, player, 2));
        root.add("entities", EntityObserver.extract(client.level, player, 32.0));
        root.add("aim", RaycastObserver.extract(client, player));

        return root;
    }
}

