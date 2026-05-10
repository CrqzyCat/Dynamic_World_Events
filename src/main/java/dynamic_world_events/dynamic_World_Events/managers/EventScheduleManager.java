package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a fixed schedule from config.yml and triggers events at set times.
 * Runs alongside the random scheduler — both can be active at once.
 *
 * Config format:
 *   schedule:
 *     enabled: true
 *     entries:
 *       - time: "20:00"
 *         event: blood_moon
 *       - time: "12:00"
 *         event: trader_caravan
 *       - time: "08:00"
 *         event: random
 */
public class EventScheduleManager {

    private final Dynamic_World_Events plugin;
    private BukkitTask checkTask;

    private record ScheduleEntry(LocalTime time, String eventId) {}
    private final List<ScheduleEntry> entries = new ArrayList<>();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

    // Track last-triggered minute to avoid double-firing
    private String lastTriggeredMinute = "";

    public EventScheduleManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("schedule.enabled", false)) return;

        loadEntries();
        if (entries.isEmpty()) return;

        plugin.getLogger().info("Event schedule loaded with " + entries.size() + " entries.");

        // Check every 20 seconds — accurate enough without being wasteful
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkSchedule, 20L, 20L * 20L);
    }

    public void stop() {
        if (checkTask != null) { checkTask.cancel(); checkTask = null; }
        entries.clear();
    }

    public void reload() {
        stop();
        start();
    }

    private void loadEntries() {
        entries.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("schedule.entries");
        if (section == null) {
            List<?> rawList = plugin.getConfig().getList("schedule.entries");
            if (rawList == null) return;
            for (Object obj : rawList) {
                if (obj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) obj;
                    String timeStr = map.containsKey("time")  ? String.valueOf(map.get("time"))  : "";
                    String eventId = map.containsKey("event") ? String.valueOf(map.get("event")) : "random";
                    parseAndAdd(timeStr, eventId);
                }
            }
            return;
        }
        for (String key : section.getKeys(false)) {
            String timeStr = section.getString(key + ".time", "");
            String eventId = section.getString(key + ".event", "random");
            parseAndAdd(timeStr, eventId);
        }
    }

    private void parseAndAdd(String timeStr, String eventId) {
        try {
            LocalTime time = LocalTime.parse(timeStr, fmt);
            entries.add(new ScheduleEntry(time, eventId));
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid schedule time format: '" + timeStr + "' — use HH:mm (e.g. 20:00)");
        }
    }

    private void checkSchedule() {
        LocalTime now = LocalTime.now();
        String currentMinute = now.format(fmt);

        // Prevent firing twice in the same minute
        if (currentMinute.equals(lastTriggeredMinute)) return;

        for (ScheduleEntry entry : entries) {
            // Match hour and minute
            if (entry.time().getHour()   == now.getHour()
             && entry.time().getMinute() == now.getMinute()) {

                lastTriggeredMinute = currentMinute;
                triggerScheduledEvent(entry.eventId());
                return; // Only one event per minute
            }
        }
    }

    private void triggerScheduledEvent(String eventId) {
        if (plugin.getEventManager().hasActiveEvent()) {
            plugin.getLogger().info("Scheduled event '" + eventId + "' skipped — event already active.");
            return;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        boolean started;
        if (eventId.equalsIgnoreCase("random")) {
            started = plugin.getEventManager().startRandomEvent();
        } else {
            started = plugin.getEventManager().startEventById(eventId);
        }

        if (started) {
            plugin.getLogger().info("Scheduled event triggered: " + eventId);
        } else {
            plugin.getLogger().warning("Scheduled event '" + eventId + "' could not be started.");
        }
    }

    public List<String> getScheduleSummary() {
        List<String> lines = new ArrayList<>();
        for (ScheduleEntry e : entries) {
            lines.add(e.time().format(fmt) + " → " + e.eventId());
        }
        return lines;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("schedule.enabled", false);
    }
}
