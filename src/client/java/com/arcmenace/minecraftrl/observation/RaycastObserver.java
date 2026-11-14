package com.arcmenace.minecraftrl.observation;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class RaycastObserver {

    public static JsonObject extract(Minecraft client, Player player) {
        JsonObject aim = new JsonObject();
        if (client == null || player == null) return aim;

        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            aim.addProperty("hit_type", "MISS");
            return aim;
        }

        aim.addProperty("hit_type", hit.getType().name());

        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);

            JsonObject blockObj = new JsonObject();
            blockObj.addProperty("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            blockObj.addProperty("x", pos.getX());
            blockObj.addProperty("y", pos.getY());
            blockObj.addProperty("z", pos.getZ());
            blockObj.addProperty("side", blockHit.getDirection().getName());
            blockObj.addProperty("distance", Math.round(player.getEyePosition().distanceTo(blockHit.getLocation()) * 100.0) / 100.0);
            aim.add("block", blockObj);
        } else if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();

            JsonObject entityObj = new JsonObject();
            entityObj.addProperty("id", target.getId());
            entityObj.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
            entityObj.addProperty("distance", Math.round(player.getEyePosition().distanceTo(target.position()) * 100.0) / 100.0);
            entityObj.addProperty("visible", player.hasLineOfSight(target));
            if (target instanceof LivingEntity living) {
                entityObj.addProperty("health", living.getHealth());
            }
            aim.add("entity", entityObj);
        }

        return aim;
    }
}

