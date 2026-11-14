package com.arcmenace.minecraftrl.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryObserver {

    public static JsonObject extract(Player player) {
        JsonObject json = new JsonObject();
        if (player == null) return json;

        Inventory inv = player.getInventory();
        json.addProperty("selected_slot", inv.selected);

        json.add("main_hand", serializeItemStack(player.getMainHandItem()));
        json.add("off_hand", serializeItemStack(player.getOffhandItem()));

        JsonObject armor = new JsonObject();
        armor.add("head", serializeItemStack(player.getItemBySlot(EquipmentSlot.HEAD)));
        armor.add("chest", serializeItemStack(player.getItemBySlot(EquipmentSlot.CHEST)));
        armor.add("legs", serializeItemStack(player.getItemBySlot(EquipmentSlot.LEGS)));
        armor.add("feet", serializeItemStack(player.getItemBySlot(EquipmentSlot.FEET)));
        json.add("armor", armor);

        JsonArray hotbar = new JsonArray();
        for (int i = 0; i < 9; i++) {
            hotbar.add(serializeItemStack(inv.getItem(i)));
        }
        json.add("hotbar", hotbar);

        JsonArray mainInventory = new JsonArray();
        for (int i = 0; i < inv.items.size(); i++) {
            mainInventory.add(serializeItemStack(inv.items.get(i)));
        }
        json.add("main_inventory", mainInventory);

        return json;
    }

    public static JsonObject serializeItemStack(ItemStack stack) {
        JsonObject item = new JsonObject();
        if (stack == null || stack.isEmpty()) {
            item.addProperty("id", "minecraft:air");
            item.addProperty("count", 0);
            return item;
        }

        item.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        item.addProperty("count", stack.getCount());
        if (stack.isDamageableItem()) {
            item.addProperty("durability", stack.getMaxDamage() - stack.getDamageValue());
            item.addProperty("max_durability", stack.getMaxDamage());
        }
        return item;
    }
}

