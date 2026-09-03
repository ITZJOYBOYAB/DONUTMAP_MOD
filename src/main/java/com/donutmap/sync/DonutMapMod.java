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
    private static int liveTickTimer = 0;

    @Override
    public void onInitializeClient() {
        pinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutmap.pin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_7,
                "category.donutmap"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            int x = (int) Math.floor(mc.player.getX());
            int y = (int) Math.floor(mc.player.getY());
            int z = (int) Math.floor(mc.player.getZ());

            // 1. Send live position once every second (20 ticks = 1 sec)
            liveTickTimer++;
            if (liveTickTimer >= 20) {
                liveTickTimer = 0;
                sendPayload(String.format("{\"type\":\"live\",\"x\":%d,\"y\":%d,\"z\":%d}", x, y, z));
            }

            // 2. Drop permanent pin ONLY when Numpad 7 is pressed
            while (pinKey.wasPressed()) {
                mc.player.sendMessage(Text.literal("§a[DONUTMAP] Pin added: §f" + x + ", " + z), false);
                sendPayload(String.format("{\"type\":\"pin\",\"x\":%d,\"y\":%d,\"z\":%d}", x, y, z));
            }
        });
    }

    private void sendPayload(String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }
}
