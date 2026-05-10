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
    private BukkitTask voteTask     = null;

    private int nextEventInSeconds = 0;

    public EventScheduler(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    public void start() { scheduleNextEvent(); }

    public void stop() {
        if (scheduleTask != null) { scheduleTask.cancel(); scheduleTask = null; }
        if (warningTask  != null) { warningTask.cancel();  warningTask  = null; }
        if (voteTask     != null) { voteTask.cancel();     voteTask     = null; }
    }

    private void scheduleNextEvent() {
        int minMinutes  = plugin.getConfig().getInt("settings.min-interval-minutes", 20);
        int maxMinutes  = plugin.getConfig().getInt("settings.max-interval-minutes", 45);
        int warnSeconds = plugin.getConfig().getInt("settings.warning-seconds", 30);
        int voteDuration = plugin.getConfig().getInt("voting.duration-seconds", 60);
        boolean votingEnabled = plugin.getConfig().getBoolean("voting.enabled", true);

        int range = Math.max(1, maxMinutes - minMinutes);
        int intervalSeconds = (minMinutes + random.nextInt(range)) * 60;
        nextEventInSeconds  = intervalSeconds;

        plugin.getBossBarManager().setNextEventCountdown(intervalSeconds);

        long intervalTicks = (long) intervalSeconds * 20L;
        long warnTicks     = (long) Math.max(0, intervalSeconds - warnSeconds) * 20L;

        // Vote starts voteDuration seconds before the event, replacing the warning
        long voteTicks = votingEnabled
            ? (long) Math.max(0, intervalSeconds - voteDuration) * 20L
            : -1L;

        plugin.getLogger().info("Next event in " + intervalSeconds / 60 + "m " + intervalSeconds % 60 + "s.");

        // Warning broadcast (only if voting is disabled)
        if (!votingEnabled) {
            warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getEventManager().hasActiveEvent()) return;
                String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
                String raw = plugin.getConfig()
                    .getString("messages.event-warning", "&eWarning! &7An event starts in &e{seconds}s&7!")
                    .replace("{seconds}", String.valueOf(warnSeconds));
                Bukkit.broadcastMessage(MessageUtil.color(prefix + raw));
            }, warnTicks);
        }

        // Vote trigger
        if (votingEnabled && voteTicks >= 0) {
            voteTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getEventManager().hasActiveEvent()) return;
                boolean voteStarted = plugin.getVotingManager().startVote();
                if (!voteStarted) {
                    // Fallback to normal warning if vote couldn't start
                    String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
                    String raw = plugin.getConfig()
                        .getString("messages.event-warning", "&eWarning! &7An event starts in &e{seconds}s&7!")
                        .replace("{seconds}", String.valueOf(voteDuration));
                    Bukkit.broadcastMessage(MessageUtil.color(prefix + raw));
                }
            }, voteTicks);
        }

        // Actual event trigger — only fires if voting is disabled or vote didn't start
        scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int minPlayers = plugin.getConfig().getInt("settings.min-players", 1);
            if (Bukkit.getOnlinePlayers().size() < minPlayers) {
                plugin.getLogger().info("Not enough players online, rescheduling.");
                scheduleNextEvent();
                return;
            }
            // If voting is active, it handles starting the event — don't double-start
            if (!plugin.getEventManager().hasActiveEvent() && !plugin.getVotingManager().isVoteActive()) {
                plugin.getEventManager().startRandomEvent();
            }
            int duration = plugin.getEventManager().hasActiveEvent()
                ? plugin.getEventManager().getActiveEvent().getDurationSeconds()
                : 120;
            Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent, (long) duration * 20L + 60L);
        }, intervalTicks);
    }

    public int getNextEventInSeconds() { return nextEventInSeconds; }
}
