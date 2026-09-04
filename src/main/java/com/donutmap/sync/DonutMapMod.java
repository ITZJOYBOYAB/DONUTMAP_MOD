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
    public static KeyBinding pinKey;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static int tickCounter = 0;
    private static boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[DonutMap] Loading DonutMap Mod for Minecraft 1.21.1...");

        try {
            pinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.donutmap.pin",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_7,
                    "key.categories.misc"
            ));
        } catch (Throwable t) {
            System.err.println("[DonutMap] Could not register keybind: " + t.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // 1. Send live coordinates once per second
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                int x = (int) Math.floor(mc.player.getX());
                int z = (int) Math.floor(mc.player.getZ());
                sendPayload("{\"type\":\"live\",\"x\":" + x + ",\"z\":" + z + "}");
            }

            // 2. Hardware-level and Keybinding check for Numpad 7
            boolean isDown = false;
            if (pinKey != null && pinKey.isPressed()) {
                isDown = true;
            } else if (mc.currentScreen == null && mc.getWindow() != null) {
                isDown = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_KP_7)
                        || InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_7);
            }

            if (isDown) {
                if (!wasDown) {
                    wasDown = true;
                    int x = (int) Math.floor(mc.player.getX());
                    int z = (int) Math.floor(mc.player.getZ());
                    sendPayload("{\"type\":\"pin\",\"x\":" + x + ",\"z\":" + z + "}");
                    mc.player.sendMessage(Text.literal("§a[DONUTMAP] Pin added at: " + x + ", " + z), false);
                }
            } else {
                wasDown = false;
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
