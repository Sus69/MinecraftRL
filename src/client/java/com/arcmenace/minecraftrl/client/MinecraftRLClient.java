package com.arcmenace.minecraftrl.client;

import com.arcmenace.minecraftrl.MinecraftRL;
import com.arcmenace.minecraftrl.actions.ActionCommand;
import com.arcmenace.minecraftrl.actions.PlayerController;
import com.arcmenace.minecraftrl.communication.RLWebSocketServer;
import com.arcmenace.minecraftrl.observation.ObservationCollector;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class MinecraftRLClient implements ClientModInitializer {
    private static final int DEFAULT_PORT = 8080;
    private static RLWebSocketServer server;
    private static final PlayerController playerController = new PlayerController();
    private static long tickCounter = 0;

    @Override
    public void onInitializeClient() {
        MinecraftRL.LOGGER.info("MinecraftRL Client initializing...");

        try {
            server = new RLWebSocketServer(DEFAULT_PORT);
            server.start();
            MinecraftRL.LOGGER.info("MinecraftRL WebSocket server started on ws://127.0.0.1:{}", DEFAULT_PORT);
        } catch (Exception e) {
            MinecraftRL.LOGGER.error("Failed to start MinecraftRL WebSocket server: {}", e.getMessage(), e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception ignored) {}
            }
        }));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        tickCounter++;

        if (client.player == null || client.level == null) {
            playerController.reset(client);
            return;
        }

        if (server != null) {
            if (server.consumeResetRequest()) {
                playerController.reset(client);
            }

            ActionCommand nextAction = server.pollNextAction();
            if (nextAction != null) {
                playerController.setCommand(nextAction);
            }

            playerController.tick(client);

            if (server.hasConnectedClients()) {
                JsonObject observation = ObservationCollector.collect(client, tickCounter);
                server.broadcastObservation(observation);
            } else if (tickCounter % 100 == 0) {
                MinecraftRL.LOGGER.info("[MinecraftRL] Active in world at tick {}. Waiting for Python WebSocket connection on port {}...", tickCounter, DEFAULT_PORT);
            }
        }
    }
}
