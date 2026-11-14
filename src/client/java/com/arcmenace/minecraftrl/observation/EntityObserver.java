package com.arcmenace.minecraftrl.observation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class EntityObserver {

    public static JsonArray extract(ClientLevel level, Player player, double radius) {
        JsonArray array = new JsonArray();
        if (level == null || player == null) return array;

        List<Entity> entities = level.getEntities(player, player.getBoundingBox().inflate(radius));
        for (Entity e : entities) {
            if (e == player || !e.isAlive()) continue;

            JsonObject entityObj = new JsonObject();
            entityObj.addProperty("id", e.getId());
            entityObj.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
            entityObj.addProperty("distance", Math.round(player.distanceTo(e) * 100.0) / 100.0);

            JsonObject pos = new JsonObject();
            pos.addProperty("x", Math.round(e.getX() * 100.0) / 100.0);
            pos.addProperty("y", Math.round(e.getY() * 100.0) / 100.0);
            pos.addProperty("z", Math.round(e.getZ() * 100.0) / 100.0);
            entityObj.add("position", pos);

            if (e instanceof LivingEntity living) {
                entityObj.addProperty("health", living.getHealth());
                entityObj.addProperty("max_health", living.getMaxHealth());
            }

            entityObj.addProperty("line_of_sight", player.hasLineOfSight(e));
            array.add(entityObj);
        }

        return array;
    }
}

