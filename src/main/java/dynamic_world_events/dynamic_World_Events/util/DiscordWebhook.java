package dynamic_world_events.dynamic_World_Events.util;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final Dynamic_World_Events plugin;

    public DiscordWebhook(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    public void sendEventStart(String eventName) {
        send("\uD83D\uDD34 **Event started:** " + eventName);
    }

    public void sendEventEnd(String eventName) {
        send("\u2705 **Event ended:** " + eventName);
    }

    private void send(String message) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) return;

        // Run async so we never block the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String serverName = plugin.getConfig().getString("discord.server-name", "My Server");
                String json = "{\"username\":\"" + serverName + " Events\","
                    + "\"avatar_url\":\"https://i.imgur.com/a7Qg8uf.png\","
                    + "\"content\":\"" + message + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    plugin.getLogger().warning("Discord webhook returned HTTP " + code);
                }
                conn.disconnect();

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }
}
