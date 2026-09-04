package com.donutmap.sync;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DonutMapMod implements ClientModInitializer {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static int tickCounter = 0;
    private static boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[DonutMap] Client mod initialized for Minecraft 1.21.1.");

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // 1. Send live coordinates once every second (20 ticks)
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                int x = (int) Math.floor(mc.player.getX());
                int z = (int) Math.floor(mc.player.getZ());
                sendPayload("{\"type\":\"live\",\"x\":" + x + ",\"z\":" + z + "}");
            }

            // 2. Hardware key check (Numpad 7 or regular 7)
            if (mc.currentScreen == null && mc.getWindow() != null) {
                boolean isNumpad7 = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_KP_7);
                boolean isRegular7 = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_7);

                if (isNumpad7 || isRegular7) {
                    if (!wasDown) {
                        wasDown = true;
                        int x = (int) Math.floor(mc.player.getX());
                        int z = (int) Math.floor(mc.player.getZ());
                        sendPayload("{\"type\":\"pin\",\"x\":" + x + ",\"z\":" + z + "}");
                        mc.player.sendMessage(Text.literal("§a[DONUTMAP] Pin added: " + x + ", " + z), false);
                    }
                } else {
                    wasDown = false;
                }
            }
        });
    }

    private static void sendPayload(String jsonPayload) {
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
