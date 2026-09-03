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
    private static boolean wasKeyPressed = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[DonutMap] Initializing DonutMap Mod for 1.21.4...");

        // Register KeyBinding under custom category
        try {
            pinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.donutmap.pin",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_7,
                    "category.donutmap"
            ));
            System.out.println("[DonutMap] KeyBinding registered successfully.");
        } catch (Exception e) {
            System.err.println("[DonutMap] Failed to register keybind via helper: " + e.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // 1. Send live coordinates every 20 ticks (1 sec)
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                int x = (int) Math.floor(mc.player.getX());
                int z = (int) Math.floor(mc.player.getZ());
                sendToServer("{\"type\":\"live\",\"x\":" + x + ",\"z\":" + z + "}");
            }

            // 2. Check if key was pressed via KeyBinding or direct GLFW
            boolean isPressed = (pinKey != null && pinKey.isPressed());
            
            // Fallback: check Numpad 7 directly if window is focused and no screen is open
            if (!isPressed && mc.currentScreen == null && mc.getWindow() != null) {
                isPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_KP_7);
            }

            if (isPressed) {
                if (!wasKeyPressed) {
                    wasKeyPressed = true;
                    int x = (int) Math.floor(mc.player.getX());
                    int z = (int) Math.floor(mc.player.getZ());
                    sendToServer("{\"type\":\"pin\",\"x\":" + x + ",\"z\":" + z + "}");
                    mc.player.sendMessage(Text.literal("§a[DONUTMAP] Pin added: " + x + ", " + z), false);
                }
            } else {
                wasKeyPressed = false;
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
