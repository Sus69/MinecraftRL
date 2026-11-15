package com.arcmenace.minecraftrl.client;

import com.arcmenace.minecraftrl.MinecraftRL;
import com.arcmenace.minecraftrl.actions.ActionCommand;
import com.arcmenace.minecraftrl.actions.PlayerController;
import com.arcmenace.minecraftrl.communication.RLWebSocketServer;
import com.arcmenace.minecraftrl.observation.ObservationCollector;
import com.arcmenace.minecraftrl.testing.ModTester;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public class MinecraftRLClient implements ClientModInitializer {
    private static final int DEFAULT_PORT = 8080;
    private static RLWebSocketServer server;
    private static final PlayerController playerController = new PlayerController();
    private static KeyMapping testKeyBinding;
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

        // Register F12 hotkey for live mod capability testing
        testKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.minecraftrl.test",
            InputConstants.KEY_F12,
            "category.minecraftrl"
        ));

        // Add visual "Test RL" button to the right of the inventory bar in InventoryScreen
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                int leftPos = (scaledWidth - 176) / 2;
                int topPos = (scaledHeight - 166) / 2;
                // Right next to the hotbar (slots 0-8) at the bottom of the inventory GUI
                int btnX = leftPos + 178;
                int btnY = topPos + 142;

                Button testButton = Button.builder(Component.literal("Test RL"), b -> {
                    client.setScreen(null); // Close inventory screen so the player can physically perform actions
                    ModTester.startTest(client, playerController);
                })
                .bounds(btnX, btnY, 52, 20)
                .tooltip(Tooltip.create(Component.literal("Runs a live automated test validating all RL observations, camera control, movement, hotbar switching, and combat commands.")))
                .build();

                Screens.getButtons(screen).add(testButton);
            }
        });

        // Visual HUD badge right next to the in-game hotbar
        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) {
                return;
            }

            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();

            // Minecraft hotbar is 182px wide, centered horizontally at (screenWidth - 182) / 2.
            // Right edge of hotbar is at (screenWidth / 2) + 91.
            int badgeX = (screenWidth / 2) + 96;
            int badgeY = screenHeight - 22;

            if (ModTester.isRunning()) {
                String text = "[RL Test] " + ModTester.getStatusText();
                int textWidth = client.font.width(text);
                int boxWidth = textWidth + 8;
                guiGraphics.fill(badgeX, badgeY, badgeX + boxWidth, badgeY + 20, 0xAA003300);
                guiGraphics.fill(badgeX, badgeY, badgeX + boxWidth, badgeY + 1, 0xFF55FF55);
                guiGraphics.drawString(client.font, text, badgeX + 4, badgeY + 6, 0x55FF55);
            } else {
                String text = "[RL Test] F12";
                int textWidth = client.font.width(text);
                int boxWidth = textWidth + 8;
                guiGraphics.fill(badgeX, badgeY, badgeX + boxWidth, badgeY + 20, 0x77000000);
                guiGraphics.fill(badgeX, badgeY, badgeX + boxWidth, badgeY + 1, 0xFFAAAAAA);
                guiGraphics.drawString(client.font, text, badgeX + 4, badgeY + 6, 0xCCCCCC);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        tickCounter++;

        if (client.player == null || client.level == null) {
            playerController.reset(client);
            return;
        }

        // Check if F12 test key was clicked
        if (testKeyBinding != null && testKeyBinding.consumeClick()) {
            if (ModTester.isRunning()) {
                ModTester.stopTest(playerController, client);
            } else {
                ModTester.startTest(client, playerController);
            }
        }

        // Update active self-test if running
        ModTester.tick(client, playerController);

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
            } else if (tickCounter % 100 == 0 && !ModTester.isRunning()) {
                MinecraftRL.LOGGER.info("[MinecraftRL] Active in world at tick {}. Waiting for Python WebSocket connection on port {}...", tickCounter, DEFAULT_PORT);
            }
        }
    }
}
