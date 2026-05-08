package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

/**
 * Automatically schedules a new random event after a configurable interval.
 * Also sends a warning broadcast X seconds before the event starts.
 */
public class EventScheduler {

    private final Dynamic_World_Events plugin;
    private final Random random = new Random();

    private BukkitTask scheduleTask = null;
    private BukkitTask warningTask  = null;

    private int nextEventInSeconds = 0;

    public EventScheduler(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    public void start() {
        scheduleNextEvent();
    }

    public void stop() {
        if (scheduleTask != null) { scheduleTask.cancel(); scheduleTask = null; }
        if (warningTask  != null) { warningTask.cancel();  warningTask  = null; }
    }

    // ── Internal scheduling ───────────────────────────────────────────────────

    private void scheduleNextEvent() {
        int minMinutes = plugin.getConfig().getInt("settings.min-interval-minutes", 20);
        int maxMinutes = plugin.getConfig().getInt("settings.max-interval-minutes", 45);
        int warnSeconds = plugin.getConfig().getInt("settings.warning-seconds", 30);

        // Random interval between min and max
        int intervalSeconds = (minMinutes + random.nextInt(maxMinutes - minMinutes + 1)) * 60;
        nextEventInSeconds = intervalSeconds;

        long intervalTicks = (long) intervalSeconds * 20L;
        long warnTicks     = (long) Math.max(0, intervalSeconds - warnSeconds) * 20L;

        plugin.getLogger().info("Next event in " + intervalSeconds / 60 + " min " + intervalSeconds % 60 + " sec.");

        // Warning broadcast
        warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getEventManager().hasActiveEvent()) return;

            String eventName = "???"; // will be resolved at start time
            String raw = plugin.getConfig().getString("messages.event-warning",
                "&eWarnung! &7Ein Ereignis beginnt in &e{seconds} Sekunden&7!");
            String msg = raw.replace("{seconds}", String.valueOf(warnSeconds))
                            .replace("{event}", eventName);
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

            Bukkit.broadcastMessage(MessageUtil.color(prefix + msg));
        }, warnTicks);

        // Actual event trigger
        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int minPlayers = plugin.getConfig().getInt("settings.min-players", 1);
            if (Bukkit.getOnlinePlayers().size() < minPlayers) {
                // Not enough players, reschedule
                plugin.getLogger().info("Not enough players online, rescheduling event.");
                scheduleNextEvent();
                return;
            }

            if (!plugin.getEventManager().hasActiveEvent()) {
                plugin.getEventManager().startRandomEvent();
            }

            // Schedule the next one after the current event would end
            int eventDuration = plugin.getEventManager().hasActiveEvent()
                ? plugin.getEventManager().getActiveEvent().getDurationSeconds()
                : 120;

            Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent,
                (long) eventDuration * 20L + 40L); // small buffer after event ends

        }, intervalTicks);
    }

    // ── Getter ────────────────────────────────────────────────────────────────

    /** Approximate seconds until the next event (decreases over time — not live, snapshot at scheduling). */
    public int getNextEventInSeconds() { return nextEventInSeconds; }
}
