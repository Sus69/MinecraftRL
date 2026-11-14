package com.arcmenace.minecraftrl.communication;

import com.arcmenace.minecraftrl.MinecraftRL;
import com.arcmenace.minecraftrl.actions.ActionCommand;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RLWebSocketServer extends WebSocketServer {
    private final ConcurrentLinkedQueue<ActionCommand> actionQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean resetRequested = false;

    public RLWebSocketServer(int port) {
        super(new InetSocketAddress("127.0.0.1", port));
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        MinecraftRL.LOGGER.info("[RLServer] Client connected: {}", conn.getRemoteSocketAddress());
        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "connection");
        welcome.addProperty("status", "connected");
        welcome.addProperty("message", "MinecraftRL WebSocket Server active.");
        conn.send(welcome.toString());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        MinecraftRL.LOGGER.info("[RLServer] Client disconnected: {} (code: {}, reason: {})", conn.getRemoteSocketAddress(), code, reason);
        actionQueue.clear();
        resetRequested = true;
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "action";

            if ("action".equalsIgnoreCase(type)) {
                ActionCommand cmd = ActionCommand.fromJson(json);
                actionQueue.offer(cmd);
            } else if ("reset".equalsIgnoreCase(type)) {
                resetRequested = true;
                actionQueue.clear();
            } else {
                JsonObject err = new JsonObject();
                err.addProperty("type", "error");
                err.addProperty("message", "Unknown message type: " + type);
                conn.send(err.toString());
            }
        } catch (Exception e) {
            MinecraftRL.LOGGER.warn("[RLServer] Error parsing incoming message: {}", e.getMessage());
            JsonObject err = new JsonObject();
            err.addProperty("type", "error");
            err.addProperty("message", "Malformed JSON or invalid schema: " + e.getMessage());
            conn.send(err.toString());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        MinecraftRL.LOGGER.error("[RLServer] WebSocket error: {}", ex.getMessage());
    }

    @Override
    public void onStart() {
        MinecraftRL.LOGGER.info("[RLServer] WebSocket Server started on port {}", getPort());
    }

    public ActionCommand pollNextAction() {
        return actionQueue.poll();
    }

    public boolean consumeResetRequest() {
        if (resetRequested) {
            resetRequested = false;
            return true;
        }
        return false;
    }

    public void broadcastObservation(JsonObject observation) {
        if (getConnections().isEmpty()) return;
        String payload = observation.toString();
        broadcast(payload);
    }

    public boolean hasConnectedClients() {
        return !getConnections().isEmpty();
    }
}

