package com.arcmenace.minecraftrl.actions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

public class PlayerController {
    private ActionCommand currentCommand = null;
    private int ticksRemaining = 0;

    public synchronized void setCommand(ActionCommand command) {
        this.currentCommand = command;
        this.ticksRemaining = (command != null) ? command.durationTicks : 0;
    }

    public synchronized void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }

        if (currentCommand == null || ticksRemaining <= 0) {
            resetKeys(client.options);
            return;
        }

        LocalPlayer player = client.player;
        Options options = client.options;

        // Apply movement keys
        options.keyUp.setDown(currentCommand.forward);
        options.keyDown.setDown(currentCommand.backward);
        options.keyLeft.setDown(currentCommand.left);
        options.keyRight.setDown(currentCommand.right);
        options.keyJump.setDown(currentCommand.jump);
        options.keySprint.setDown(currentCommand.sprint);
        options.keyShift.setDown(currentCommand.sneak);

        // Apply combat & interaction keys
        options.keyAttack.setDown(currentCommand.attack || currentCommand.holdAttack);
        options.keyUse.setDown(currentCommand.use || currentCommand.holdUse);

        // Apply rotation
        if (currentCommand.absoluteYaw != null) {
            player.setYRot(currentCommand.absoluteYaw);
        } else if (currentCommand.deltaYaw != 0.0f) {
            player.setYRot(player.getYRot() + currentCommand.deltaYaw);
        }

        if (currentCommand.absolutePitch != null) {
            player.setXRot(clampPitch(currentCommand.absolutePitch));
        } else if (currentCommand.deltaPitch != 0.0f) {
            player.setXRot(clampPitch(player.getXRot() + currentCommand.deltaPitch));
        }

        // Keep previous rotation consistent to avoid camera smoothing lag
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();

        // Apply inventory hotbar slot selection
        if (currentCommand.selectSlot >= 0 && currentCommand.selectSlot <= 8) {
            player.getInventory().selected = currentCommand.selectSlot;
        }

        // Consume delta look and non-held actions after first tick
        currentCommand.deltaYaw = 0.0f;
        currentCommand.deltaPitch = 0.0f;
        if (!currentCommand.holdAttack) currentCommand.attack = false;
        if (!currentCommand.holdUse) currentCommand.use = false;

        ticksRemaining--;
        if (ticksRemaining <= 0) {
            resetKeys(options);
            currentCommand = null;
        }
    }

    public synchronized void reset(Minecraft client) {
        currentCommand = null;
        ticksRemaining = 0;
        if (client != null && client.options != null) {
            resetKeys(client.options);
        }
    }

    private void resetKeys(Options options) {
        if (options == null) return;
        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyLeft.setDown(false);
        options.keyRight.setDown(false);
        options.keyJump.setDown(false);
        options.keySprint.setDown(false);
        options.keyShift.setDown(false);
        options.keyAttack.setDown(false);
        options.keyUse.setDown(false);
    }

    private float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }
}

