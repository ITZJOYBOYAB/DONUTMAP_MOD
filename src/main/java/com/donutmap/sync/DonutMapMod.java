package com.donutmap.sync;

import net.fabricmc.api.ModInitializer;
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

public class DonutMapMod implements ModInitializer {
    public static KeyBinding pinKey;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static int tickCounter = 0;
    private static boolean wasKeyPressed = false;

    @Override
    public void onInitialize() {
        System.out.println("========================================");
        System.out.println("[DonutMap] MOD INITIALIZING FOR 1.21.4!");
        System.out.println("========================================");

        try {
            pinKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.donutmap.pin",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_7,
                    KeyBinding.MISC_CATEGORY
            ));
            System.out.println("[DonutMap] Key registered in MISC category.");
        } catch (Throwable t) {
            System.err.println("[DonutMap] Error registering keybind: " + t.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            // 1. Live coordinates sent every 20 ticks (1 sec)
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                int x = (int) Math.floor(mc.player.getX());
                int z = (int) Math.floor(mc.player.getZ());
                sendToServer("{\"type\":\"live\",\"x\":" + x + ",\"z\":" + z + "}");
            }

            // 2. Key trigger: KeyBinding with GLFW hardware fallback
            boolean isDown = false;
            if (pinKey != null && pinKey.isPressed()) {
                isDown = true;
            } else if (mc.currentScreen == null && mc.getWindow() != null) {
                isDown = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_KP_7);
            }

            if (isDown) {
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
