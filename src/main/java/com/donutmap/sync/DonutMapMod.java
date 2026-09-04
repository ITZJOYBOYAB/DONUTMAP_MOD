package com.donutmap.sync;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DonutMapMod implements ClientModInitializer {
    public static KeyBinding pinKeyBinding;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        // Register keybind with standard category so it appears in Options -> Controls -> Key Binds
        pinKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutmap.pin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_7,
                "category.donutmap"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // 1. Live location: stream player coordinates every 20 ticks (1 second)
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                int x = (int) Math.floor(mc.player.getX());
                int z = (int) Math.floor(mc.player.getZ());
                sendToServer("{\"type\":\"live\",\"x\":" + x + ",\"z\":" + z + "}");
            }

            // 2. Pin keybind: checks if the rebindable key was pressed
            if (pinKeyBinding != null) {
                while (pinKeyBinding.wasPressed()) {
                    int x = (int) Math.floor(mc.player.getX());
                    int z = (int) Math.floor(mc.player.getZ());
                    
                    // Post coordinate to local server (and forwardable to Discord/Web)
                    sendToServer("{\"type\":\"pin\",\"x\":" + x + ",\"z\":" + z + "}");
                    
                    // Confirm in player chat
                    mc.player.sendMessage(Text.literal("§a[DONUTMAP] Pin added at X: " + x + ", Z: " + z), false);
                }
            }
        });
    }

    private static void sendToServer(String jsonPayload) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:3000/"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            client.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }
}
