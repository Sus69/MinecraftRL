package com.arcmenace.minecraftrl.player;

import com.google.gson.JsonObject;

public record PlayerState(
    float health,
    float maxHealth,
    int foodLevel,
    float saturationLevel,
    int experienceLevel,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    double velocityX,
    double velocityY,
    double velocityZ,
    boolean onGround,
    boolean sprinting,
    boolean sneaking,
    boolean swimming
) {
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        
        JsonObject attributes = new JsonObject();
        attributes.addProperty("health", health);
        attributes.addProperty("max_health", maxHealth);
        attributes.addProperty("food_level", foodLevel);
        attributes.addProperty("saturation_level", saturationLevel);
        attributes.addProperty("experience_level", experienceLevel);
        root.add("attributes", attributes);

        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        root.add("position", position);

        JsonObject rotation = new JsonObject();
        rotation.addProperty("yaw", yaw);
        rotation.addProperty("pitch", pitch);
        root.add("rotation", rotation);

        JsonObject velocity = new JsonObject();
        velocity.addProperty("x", velocityX);
        velocity.addProperty("y", velocityY);
        velocity.addProperty("z", velocityZ);
        root.add("velocity", velocity);

        JsonObject movement = new JsonObject();
        movement.addProperty("on_ground", onGround);
        movement.addProperty("sprinting", sprinting);
        movement.addProperty("sneaking", sneaking);
        movement.addProperty("swimming", swimming);
        root.add("movement_state", movement);

        return root;
    }
}

