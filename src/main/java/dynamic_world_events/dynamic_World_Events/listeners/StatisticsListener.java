package dynamic_world_events.dynamic_World_Events.listeners;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.InvasionEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class StatisticsListener implements Listener {

    private final Dynamic_World_Events plugin;

    public StatisticsListener(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        // Check if this mob is an Invader (has "Invader" custom name set by InvasionEvent)
        if (entity.getCustomName() != null && entity.getCustomName().contains("Invader")) {
            // Only count if invasion is the active event
            if (plugin.getEventManager().getActiveEvent() instanceof InvasionEvent) {
                plugin.getStatisticsManager().recordInvasionKill(
                    killer.getUniqueId(), killer.getName()
                );
            }
        }
    }
}
