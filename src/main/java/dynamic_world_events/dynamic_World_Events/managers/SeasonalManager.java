package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies seasonal weight modifiers to events based on the current real-world date.
 *
 * Instead of changing the base weight in config.yml, this multiplies it
 * during the random selection — so seasonal events appear more often
 * without permanently breaking the balance.
 *
 * Config format:
 *
 *   seasonal:
 *     enabled: true
 *     seasons:
 *       halloween:
 *         start: "10-01"   # MM-dd
 *         end:   "11-01"
 *         modifiers:
 *           haunting:    3.0   # 3x more likely
 *           blood_moon:  2.0
 *           drought:     0.2   # 5x less likely
 *       winter:
 *         start: "12-01"
 *         end:   "01-06"
 *         modifiers:
 *           treasure_hunt: 2.5
 *           trader_caravan: 2.0
 *       spring:
 *         start: "03-20"
 *         end:   "06-21"
 *         modifiers:
 *           bountiful_harvest: 3.0
 *           drought:           0.5
 */
public class SeasonalManager {

    private final Dynamic_World_Events plugin;

    // eventId → combined multiplier from all active seasons
    private final Map<String, Double> activeModifiers = new HashMap<>();

    public SeasonalManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
        recalculate();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the effective weight for an event after seasonal modifiers.
     * Falls back to the base config weight if no modifier is active.
     */
    public int getEffectiveWeight(WorldEvent event) {
        double base = event.getWeight();
        double multiplier = activeModifiers.getOrDefault(event.getId(), 1.0);
        return Math.max(1, (int) Math.round(base * multiplier));
    }

    /** Returns the name of the currently active season, or null if none. */
    public String getActiveSeason() {
        if (!plugin.getConfig().getBoolean("seasonal.enabled", true)) return null;

        LocalDate today = LocalDate.now();
        var seasons = plugin.getConfig().getConfigurationSection("seasonal.seasons");
        if (seasons == null) return null;

        for (String name : seasons.getKeys(false)) {
            String startStr = seasons.getString(name + ".start", "");
            String endStr   = seasons.getString(name + ".end", "");
            if (isInSeason(today, startStr, endStr)) return name;
        }
        return null;
    }

    /** Returns all active modifiers as a readable summary. */
    public Map<String, Double> getActiveModifiers() {
        return new HashMap<>(activeModifiers);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Recalculates active modifiers based on today's date.
     * Called on startup and reload.
     */
    public void recalculate() {
        activeModifiers.clear();
        if (!plugin.getConfig().getBoolean("seasonal.enabled", true)) return;

        var seasons = plugin.getConfig().getConfigurationSection("seasonal.seasons");
        if (seasons == null) return;

        LocalDate today = LocalDate.now();

        for (String seasonName : seasons.getKeys(false)) {
            String startStr = seasons.getString(seasonName + ".start", "");
            String endStr   = seasons.getString(seasonName + ".end", "");

            if (!isInSeason(today, startStr, endStr)) continue;

            plugin.getLogger().info("[DWE] Seasonal modifier active: " + seasonName);

            var modSection = seasons.getConfigurationSection(seasonName + ".modifiers");
            if (modSection == null) continue;

            for (String eventId : modSection.getKeys(false)) {
                double modifier = modSection.getDouble(eventId, 1.0);
                // Stack modifiers if multiple seasons overlap
                activeModifiers.merge(eventId, modifier, (a, b) -> a * b);
            }
        }

        if (!activeModifiers.isEmpty()) {
            plugin.getLogger().info("[DWE] Active seasonal weight modifiers: " + activeModifiers);
        }
    }

    private boolean isInSeason(LocalDate today, String startStr, String endStr) {
        try {
            MonthDay start = MonthDay.parse("--" + startStr);
            MonthDay end   = MonthDay.parse("--" + endStr);
            MonthDay now   = MonthDay.from(today);

            // Handle wrap-around (e.g. Dec 01 → Jan 06)
            if (start.compareTo(end) <= 0) {
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                // Season wraps around year boundary
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[DWE] Invalid seasonal date format: '" + startStr + "' or '" + endStr + "' — use MM-dd");
            return false;
        }
    }
}
