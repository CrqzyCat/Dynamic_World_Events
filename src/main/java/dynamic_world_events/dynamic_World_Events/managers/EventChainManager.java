package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Handles event chains — when one event ends, it can schedule a follow-up event.
 *
 * Config format:
 *
 *   chains:
 *     enabled: true
 *     entries:
 *       blood_moon_to_haunting:
 *         trigger: blood_moon        # Event that triggers the chain
 *         next:    haunting          # Event to start after the delay
 *         delay-minutes: 1440        # Delay before the next event (1440 = 24h)
 *         chance: 0.75               # 0.0-1.0 probability the chain fires
 *         announce: true             # Broadcast a warning before it starts
 *         announce-message: "&5The spirits stirred by last night's Blood Moon are restless..."
 *
 *       invasion_to_blood_moon:
 *         trigger: invasion
 *         next:    blood_moon
 *         delay-minutes: 30
 *         chance: 0.4
 *         announce: true
 *         announce-message: "&cThe invasion has drawn something darker..."
 */
public class EventChainManager {

    private final Dynamic_World_Events plugin;
    private final Random random = new Random();

    // chainId → BukkitTask ID, for cancellation on reload
    private final Map<String, Integer> pendingChains = new HashMap<>();

    public EventChainManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    // ── Called by EventManager when an event ends ─────────────────────────────

    public void onEventEnd(String endedEventId, boolean forced) {
        if (!plugin.getConfig().getBoolean("chains.enabled", true)) return;

        // Forced stops don't trigger chains (admin manually ended it)
        if (forced) return;

        ConfigurationSection entries = plugin.getConfig().getConfigurationSection("chains.entries");
        if (entries == null) return;

        for (String chainId : entries.getKeys(false)) {
            String trigger = entries.getString(chainId + ".trigger", "");
            if (!trigger.equalsIgnoreCase(endedEventId)) continue;

            String nextEvent     = entries.getString(chainId + ".next", "");
            int    delayMinutes  = entries.getInt(chainId + ".delay-minutes", 60);
            double chance        = entries.getDouble(chainId + ".chance", 1.0);
            boolean announce     = entries.getBoolean(chainId + ".announce", true);
            String  announceMsg  = entries.getString(chainId + ".announce-message", "");

            if (nextEvent.isEmpty()) continue;

            // Probability check
            if (random.nextDouble() > chance) {
                plugin.getLogger().info("[DWE Chains] Chain '" + chainId + "' did not fire (chance roll failed).");
                continue;
            }

            // Check if the next event exists and is enabled
            boolean eventExists = plugin.getEventManager().getRegisteredEvents().stream()
                .anyMatch(e -> e.getId().equalsIgnoreCase(nextEvent) && e.isEnabled());
            if (!eventExists) {
                plugin.getLogger().warning("[DWE Chains] Chain event '" + nextEvent + "' not found or disabled — skipping.");
                continue;
            }

            plugin.getLogger().info("[DWE Chains] Chain '" + chainId + "' will fire in " + delayMinutes + " minutes.");

            long delayTicks = (long) delayMinutes * 60L * 20L;

            // Warn players halfway through the delay
            if (announce && !announceMsg.isEmpty() && delayMinutes > 2) {
                long warnTicks = Math.max(20L, delayTicks / 2);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                    Bukkit.broadcastMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r") + announceMsg
                    )), warnTicks);
            }

            // Schedule the follow-up event
            final String finalNextEvent = nextEvent;
            final String finalChainId   = chainId;
            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pendingChains.remove(finalChainId);
                if (plugin.getEventManager().hasActiveEvent()) {
                    plugin.getLogger().info("[DWE Chains] Skipping chain '" + finalChainId + "' — another event is already active.");
                    return;
                }
                plugin.getLogger().info("[DWE Chains] Firing chain event: " + finalNextEvent);
                plugin.getEventManager().startEventById(finalNextEvent);
            }, delayTicks).getTaskId();

            pendingChains.put(chainId, taskId);
        }
    }

    // ── Cancel all pending chains (on reload / shutdown) ─────────────────────

    public void cancelAll() {
        pendingChains.values().forEach(Bukkit.getScheduler()::cancelTask);
        pendingChains.clear();
    }

    public Map<String, Integer> getPendingChains() {
        return Collections.unmodifiableMap(pendingChains);
    }
}
