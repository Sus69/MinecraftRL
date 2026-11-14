package com.arcmenace.minecraftrl.actions;

import com.google.gson.JsonObject;

public class ActionCommand {
    // Movement
    public boolean forward;
    public boolean backward;
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean sprint;
    public boolean sneak;

    // Look
    public float deltaYaw;
    public float deltaPitch;
    public Float absoluteYaw;
    public Float absolutePitch;

    // Combat & Interaction
    public boolean attack;
    public boolean holdAttack;
    public boolean use;
    public boolean holdUse;

    // Inventory
    public int selectSlot = -1; // -1 means do not change

    // Duration in ticks
    public int durationTicks = 1;

    public static ActionCommand fromJson(JsonObject json) {
        ActionCommand cmd = new ActionCommand();
        if (json == null) return cmd;

        if (json.has("movement") && json.get("movement").isJsonObject()) {
            JsonObject m = json.getAsJsonObject("movement");
            cmd.forward = getBool(m, "forward");
            cmd.backward = getBool(m, "backward");
            cmd.left = getBool(m, "left");
            cmd.right = getBool(m, "right");
            cmd.jump = getBool(m, "jump");
            cmd.sprint = getBool(m, "sprint");
            cmd.sneak = getBool(m, "sneak");
        }

        if (json.has("look") && json.get("look").isJsonObject()) {
            JsonObject l = json.getAsJsonObject("look");
            if (l.has("delta_yaw")) cmd.deltaYaw = l.get("delta_yaw").getAsFloat();
            if (l.has("delta_pitch")) cmd.deltaPitch = l.get("delta_pitch").getAsFloat();
            if (l.has("yaw")) cmd.absoluteYaw = l.get("yaw").getAsFloat();
            if (l.has("pitch")) cmd.absolutePitch = l.get("pitch").getAsFloat();
        }

        if (json.has("combat") && json.get("combat").isJsonObject()) {
            JsonObject c = json.getAsJsonObject("combat");
            cmd.attack = getBool(c, "attack");
            cmd.holdAttack = getBool(c, "hold_attack");
        }

        if (json.has("interaction") && json.get("interaction").isJsonObject()) {
            JsonObject i = json.getAsJsonObject("interaction");
            cmd.use = getBool(i, "use");
            cmd.holdUse = getBool(i, "hold_use");
        }

        if (json.has("inventory") && json.get("inventory").isJsonObject()) {
            JsonObject inv = json.getAsJsonObject("inventory");
            if (inv.has("select_slot")) {
                cmd.selectSlot = inv.get("select_slot").getAsInt();
            }
        }

        if (json.has("duration_ticks")) {
            cmd.durationTicks = Math.max(1, json.get("duration_ticks").getAsInt());
        }

        return cmd;
    }

    private static boolean getBool(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).getAsBoolean();
    }
}

