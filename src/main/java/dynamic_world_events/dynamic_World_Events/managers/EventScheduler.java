package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class EventScheduler {

    private final Dynamic_World_Events plugin;
    private final Random random = new Random();

    private BukkitTask scheduleTask = null;
    private BukkitTask warningTask  = null;

    private int nextEventInSeconds = 0;

    public EventScheduler(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleNextEvent();
    }

    public void stop() {
        if (scheduleTask != null) { scheduleTask.cancel(); scheduleTask = null; }
        if (warningTask  != null) { warningTask.cancel();  warningTask  = null; }
    }

    private void scheduleNextEvent() {
        int minMinutes  = plugin.getConfig().getInt("settings.min-interval-minutes", 20);
        int maxMinutes  = plugin.getConfig().getInt("settings.max-interval-minutes", 45);
        int warnSeconds = plugin.getConfig().getInt("settings.warning-seconds", 30);

        // Ensure max >= min to avoid nextInt(0) crash
        int range = Math.max(1, maxMinutes - minMinutes);
        int intervalSeconds = (minMinutes + random.nextInt(range)) * 60;
        nextEventInSeconds  = intervalSeconds;

        long intervalTicks = (long) intervalSeconds * 20L;
        long warnTicks     = (long) Math.max(0, intervalSeconds - warnSeconds) * 20L;

        plugin.getLogger().info("Next event in " + intervalSeconds / 60 + "m " + intervalSeconds % 60 + "s.");

        // Warning broadcast
        warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getEventManager().hasActiveEvent()) return;

            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            String raw = plugin.getConfig().getString("messages.event-warning",
                "&eWarning! &7An event starts in &e{seconds}s&7!")
                .replace("{seconds}", String.valueOf(warnSeconds));
            Bukkit.broadcastMessage(MessageUtil.color(prefix + raw));
        }, warnTicks);

        // Event trigger
        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int minPlayers = plugin.getConfig().getInt("settings.min-players", 1);
            if (Bukkit.getOnlinePlayers().size() < minPlayers) {
                plugin.getLogger().info("Not enough players online, rescheduling.");
                scheduleNextEvent();
                return;
            }

            // Only start if no event is already running
            if (!plugin.getEventManager().hasActiveEvent()) {
                plugin.getEventManager().startRandomEvent();
            }

            // Schedule next event after current one ends
            int duration = plugin.getEventManager().hasActiveEvent()
                ? plugin.getEventManager().getActiveEvent().getDurationSeconds()
                : 120;

            Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent,
                (long) duration * 20L + 60L);

        }, intervalTicks);
    }

    public int getNextEventInSeconds() { return nextEventInSeconds; }
}
