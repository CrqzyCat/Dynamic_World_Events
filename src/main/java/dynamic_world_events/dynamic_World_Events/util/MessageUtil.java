package dynamic_world_events.dynamic_World_Events.util;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {}

    /** Translates &-color codes into Bukkit chat colors. */
    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /** Sends a color-formatted message to a player. */
    public static void send(Player player, String prefix, String msg) {
        player.sendMessage(color(prefix + msg));
    }
}
