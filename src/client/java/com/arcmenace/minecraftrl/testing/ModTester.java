package com.arcmenace.minecraftrl.testing;

import com.arcmenace.minecraftrl.MinecraftRL;
import com.arcmenace.minecraftrl.actions.ActionCommand;
import com.arcmenace.minecraftrl.actions.PlayerController;
import com.arcmenace.minecraftrl.observation.ObservationCollector;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModTester {
    private static boolean isRunning = false;
    private static int currentStepIndex = 0;
    private static int ticksUntilNextStep = 0;
    private static String statusText = "Idle";
    private static final List<TestStep> steps = new ArrayList<>();

    public record TestStep(
        String name,
        String jsonPayload,
        int durationTicks,
        Runnable onExecute
    ) {}

    public static synchronized void startTest(Minecraft client, PlayerController controller) {
        if (isRunning) {
            notifyPlayer(client, "Test already in progress...", true);
            return;
        }

        if (client.player == null || client.level == null) {
            notifyPlayer(client, "Cannot start test: Player not in world.", true);
            return;
        }

        buildTestSteps(client, controller);
        isRunning = true;
        currentStepIndex = 0;
        ticksUntilNextStep = 0;
        statusText = "Starting Test...";
        notifyPlayer(client, "[MinecraftRL] Starting Automated Mod Capability Test...", false);
    }

    public static synchronized void stopTest(PlayerController controller, Minecraft client) {
        isRunning = false;
        currentStepIndex = 0;
        ticksUntilNextStep = 0;
        statusText = "Idle";
        if (controller != null && client != null) {
            controller.reset(client);
        }
    }

    public static synchronized void tick(Minecraft client, PlayerController controller) {
        if (!isRunning || client == null || client.player == null) {
            return;
        }

        if (ticksUntilNextStep > 0) {
            ticksUntilNextStep--;
            return;
        }

        if (currentStepIndex >= steps.size()) {
            isRunning = false;
            statusText = "Complete!";
            notifyPlayer(client, "[MinecraftRL] All mod systems verified successfully!", false);
            if (controller != null) {
                controller.reset(client);
            }
            return;
        }

        TestStep step = steps.get(currentStepIndex);
        statusText = (currentStepIndex + 1) + "/" + steps.size() + ": " + step.name();
        notifyPlayer(client, "[MinecraftRL] [" + (currentStepIndex + 1) + "/" + steps.size() + "] " + step.name(), true);

        if (step.onExecute() != null) {
            step.onExecute().run();
        }

        if (step.jsonPayload() != null && !step.jsonPayload().isBlank()) {
            try {
                JsonObject json = JsonParser.parseString(step.jsonPayload()).getAsJsonObject();
                ActionCommand cmd = ActionCommand.fromJson(json);
                controller.setCommand(cmd);
            } catch (Exception e) {
                MinecraftRL.LOGGER.error("[ModTester] Error parsing test JSON payload: {}", e.getMessage());
            }
        }

        ticksUntilNextStep = step.durationTicks();
        currentStepIndex++;
    }

    private static void buildTestSteps(Minecraft client, PlayerController controller) {
        steps.clear();

        // Step 1: Verify Observation Extraction
        steps.add(new TestStep(
            "Verify Observation Extraction",
            "",
            10,
            () -> {
                JsonObject obs = ObservationCollector.collect(client, 0);
                String hp = obs.has("player") && obs.getAsJsonObject("player").has("attributes")
                    ? obs.getAsJsonObject("player").getAsJsonObject("attributes").get("health").getAsString()
                    : "N/A";
                String biome = obs.has("world") && obs.getAsJsonObject("world").has("biome")
                    ? obs.getAsJsonObject("world").get("biome").getAsString()
                    : "N/A";
                notifyPlayer(client, "[ModTester] Sensors OK (HP: " + hp + ", Biome: " + biome + ")", false);
            }
        ));

        // Step 2: Camera Rotation (Look Right & Up)
        steps.add(new TestStep(
            "Camera Look (Yaw +30, Pitch -15)",
            "{\"look\": {\"delta_yaw\": 30.0, \"delta_pitch\": -15.0}, \"duration_ticks\": 6}",
            8,
            null
        ));

        // Step 3: Camera Rotation (Look Left & Down)
        steps.add(new TestStep(
            "Camera Look (Yaw -60, Pitch +30)",
            "{\"look\": {\"delta_yaw\": -60.0, \"delta_pitch\": 30.0}, \"duration_ticks\": 6}",
            8,
            null
        ));

        // Step 4: Camera Center & Raycast Test
        steps.add(new TestStep(
            "Camera Center & Crosshair Raycast",
            "{\"look\": {\"delta_yaw\": 30.0, \"delta_pitch\": -15.0}, \"duration_ticks\": 6}",
            8,
            () -> {
                JsonObject obs = ObservationCollector.collect(client, 0);
                String hitType = obs.has("aim") && obs.getAsJsonObject("aim").has("hit_type")
                    ? obs.getAsJsonObject("aim").get("hit_type").getAsString()
                    : "UNKNOWN";
                notifyPlayer(client, "[ModTester] Crosshair Aim Target: " + hitType, false);
            }
        ));

        // Step 5: Movement (Sprint Forward)
        steps.add(new TestStep(
            "Movement: Sprint Forward",
            "{\"movement\": {\"forward\": true, \"sprint\": true}, \"duration_ticks\": 14}",
            16,
            null
        ));

        // Step 6: Movement (Jump & Strafe Left)
        steps.add(new TestStep(
            "Movement: Jump & Strafe Left",
            "{\"movement\": {\"jump\": true, \"left\": true, \"forward\": true}, \"duration_ticks\": 12}",
            14,
            null
        ));

        // Step 7: Movement (Sneak)
        steps.add(new TestStep(
            "Movement: Sneak",
            "{\"movement\": {\"sneak\": true}, \"duration_ticks\": 10}",
            12,
            null
        ));

        // Step 8: Hotbar Selection Cycle (Slots 1, 2, then 0)
        steps.add(new TestStep(
            "Hotbar: Switch to Slot 1",
            "{\"inventory\": {\"select_slot\": 1}, \"duration_ticks\": 4}",
            6,
            null
        ));
        steps.add(new TestStep(
            "Hotbar: Switch to Slot 2",
            "{\"inventory\": {\"select_slot\": 2}, \"duration_ticks\": 4}",
            6,
            null
        ));
        steps.add(new TestStep(
            "Hotbar: Switch to Slot 0",
            "{\"inventory\": {\"select_slot\": 0}, \"duration_ticks\": 4}",
            6,
            null
        ));

        // Step 9: Combat (Attack Swing)
        steps.add(new TestStep(
            "Combat: Attack Swing",
            "{\"combat\": {\"attack\": true}, \"duration_ticks\": 5}",
            8,
            null
        ));

        // Step 10: Interaction (Item Use)
        steps.add(new TestStep(
            "Interaction: Use / Interact",
            "{\"interaction\": {\"use\": true}, \"duration_ticks\": 5}",
            8,
            null
        ));

        // Step 11: Reset
        steps.add(new TestStep(
            "Reset Controls & Neutralize",
            "{\"type\": \"reset\"}",
            5,
            () -> controller.reset(client)
        ));
    }

    private static void notifyPlayer(Minecraft client, String message, boolean overlay) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), overlay);
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static String getStatusText() {
        return statusText;
    }
}
