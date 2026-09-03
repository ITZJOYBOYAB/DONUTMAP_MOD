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
    private static KeyBinding pinKey;
    private static final HttpClient client = HttpClient.newHttpClient();
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
            if (mc.currentScreen != null) return;

            int x = (int) Math.floor(mc.player.getX());
            int y = (int) Math.floor(mc.player.getY());
            int z = (int) Math.floor(mc.player.getZ());

            liveTickTimer++;
            if (liveTickTimer >= 20) {
                liveTickTimer = 0;
                sendPayload(String.format("{\"type\":\"LIVE\",\"x\":%d,\"y\":%d,\"z\":%d}", x, y, z));
            }

            while (pinKey.wasPressed()) {
                mc.keyboard.setClipboard(String.format("%d %d %d", x, y, z));
                mc.player.sendMessage(Text.of("§a[DONUTMAP] Pin added: " + x + ", " + z), false);
                sendPayload(String.format("{\"type\":\"PIN\",\"x\":%d,\"y\":%d,\"z\":%d}", x, y, z));
            }
        });
    }

    private static void sendPayload(String json) {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                client.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {}
        }).start();
    }
}
