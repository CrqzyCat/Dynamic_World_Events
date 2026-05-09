package dynamic_world_events.dynamic_World_Events.listeners;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final Dynamic_World_Events plugin;

    public PlayerJoinListener(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getBossBarManager().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBossBarManager().removePlayer(event.getPlayer());
    }
}
