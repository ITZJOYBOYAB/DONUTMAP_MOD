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
import java.time.Duration;

public class DonutMapMod implements ClientModInitializer {
    private static KeyBinding pinKey;
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final String SERVER_URL = "http://localhost:3000";

    @Override
    public void onInitializeClient() {
        // Register keybind (default: Numpad 7)
        pinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutmap.pin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_7,
                "category.donutmap"
        ));

        // Listen for key presses only
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // ONLY runs when the key is actually clicked
            while (pinKey.wasPressed()) {
                int x = (int) Math.floor(mc.player.getX());
                int y = (int) Math.floor(mc.player.getY());
                int z = (int) Math.floor(mc.player.getZ());

                // Show in chat
                mc.player.sendMessage(Text.literal("§a[DONUTMAP] Pin added: §f" + x + ", " + z), false);

                // Send to local server in a background thread (no lag/anticheat trigger)
                String json = String.format("{\"x\":%d,\"y\":%d,\"z\":%d}", x, y, z);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            }
        });
    }
}
