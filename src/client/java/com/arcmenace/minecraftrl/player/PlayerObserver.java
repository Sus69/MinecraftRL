package com.arcmenace.minecraftrl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class PlayerObserver {

    public static PlayerState extract(LocalPlayer player) {
        if (player == null) {
            return null;
        }

        Vec3 velocity = player.getDeltaMovement();

        return new PlayerState(
            player.getHealth(),
            player.getMaxHealth(),
            player.getFoodData().getFoodLevel(),
            player.getFoodData().getSaturationLevel(),
            player.experienceLevel,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            velocity.x,
            velocity.y,
            velocity.z,
            player.onGround(),
            player.isSprinting(),
            player.isShiftKeyDown(),
            player.isSwimming()
        );
    }
}

