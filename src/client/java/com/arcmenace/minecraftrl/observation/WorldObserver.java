package com.arcmenace.minecraftrl.observation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class WorldObserver {

    public static JsonObject extract(ClientLevel level, Player player, int blockRadius) {
        JsonObject json = new JsonObject();
        if (level == null || player == null) return json;

        json.addProperty("game_time", level.getGameTime());
        json.addProperty("day_time", level.getDayTime());
        json.addProperty("is_raining", level.isRaining());
        json.addProperty("is_thundering", level.isThundering());

        BlockPos playerPos = player.blockPosition();
        Holder<Biome> biomeHolder = level.getBiome(playerPos);
        String biomeName = biomeHolder.unwrapKey()
            .map(key -> key.location().toString())
            .orElse("minecraft:unknown");
        json.addProperty("biome", biomeName);

        if (blockRadius > 0) {
            JsonArray blocksArray = new JsonArray();
            int px = playerPos.getX();
            int py = playerPos.getY();
            int pz = playerPos.getZ();

            for (int dx = -blockRadius; dx <= blockRadius; dx++) {
                for (int dy = -blockRadius; dy <= blockRadius; dy++) {
                    for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                        BlockPos pos = new BlockPos(px + dx, py + dy, pz + dz);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            JsonObject b = new JsonObject();
                            b.addProperty("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                            b.addProperty("x", pos.getX());
                            b.addProperty("y", pos.getY());
                            b.addProperty("z", pos.getZ());
                            b.addProperty("rel_x", dx);
                            b.addProperty("rel_y", dy);
                            b.addProperty("rel_z", dz);
                            blocksArray.add(b);
                        }
                    }
                }
            }
            json.add("nearby_blocks", blocksArray);
        }

        return json;
    }
}

