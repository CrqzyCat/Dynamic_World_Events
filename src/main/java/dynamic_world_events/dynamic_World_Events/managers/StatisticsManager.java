package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Tracks per-player event statistics and persists them to stats.yml.
 */
public class StatisticsManager {

    private final Dynamic_World_Events plugin;
    private final File statsFile;
    private YamlConfiguration stats;

    public StatisticsManager(Dynamic_World_Events plugin) {
        this.plugin    = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    /** Called when a player is online during a completed event. */
    public void recordEventParticipation(UUID uuid, String playerName, String eventId) {
        String base = "players." + uuid + ".";
        stats.set(base + "name", playerName);
        int current = stats.getInt(base + "events-participated", 0);
        stats.set(base + "events-participated", current + 1);

        // Per-event counter
        String eventKey = base + "events." + eventId;
        stats.set(eventKey, stats.getInt(eventKey, 0) + 1);

        save();
    }

    /** Called when a player survives the Blood Moon event. */
    public void recordBloodMoonSurvived(UUID uuid, String playerName) {
        String key = "players." + uuid + ".blood-moons-survived";
        stats.set("players." + uuid + ".name", playerName);
        stats.set(key, stats.getInt(key, 0) + 1);
        save();
    }

    /** Called when a player opens a Treasure Hunt chest. */
    public void recordTreasureFound(UUID uuid, String playerName) {
        String key = "players." + uuid + ".treasures-found";
        stats.set("players." + uuid + ".name", playerName);
        stats.set(key, stats.getInt(key, 0) + 1);
        save();
    }

    /** Called when a player kills an Invasion mob. */
    public void recordInvasionKill(UUID uuid, String playerName) {
        String key = "players." + uuid + ".invasion-kills";
        stats.set("players." + uuid + ".name", playerName);
        stats.set(key, stats.getInt(key, 0) + 1);
        save();
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    public int getEventsParticipated(UUID uuid) {
        return stats.getInt("players." + uuid + ".events-participated", 0);
    }

    public int getEventCount(UUID uuid, String eventId) {
        return stats.getInt("players." + uuid + ".events." + eventId, 0);
    }

    public int getBloodMoonsSurvived(UUID uuid) {
        return stats.getInt("players." + uuid + ".blood-moons-survived", 0);
    }

    public int getTreasuresFound(UUID uuid) {
        return stats.getInt("players." + uuid + ".treasures-found", 0);
    }

    public int getInvasionKills(UUID uuid) {
        return stats.getInt("players." + uuid + ".invasion-kills", 0);
    }

    /** Returns top N players sorted by events-participated. */
    public List<Map.Entry<String, Integer>> getLeaderboard(int topN) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();

        if (!stats.isConfigurationSection("players")) return entries;

        for (String uuidStr : stats.getConfigurationSection("players").getKeys(false)) {
            String name  = stats.getString("players." + uuidStr + ".name", uuidStr);
            int    count = stats.getInt("players." + uuidStr + ".events-participated", 0);
            entries.add(Map.entry(name, count));
        }

        entries.sort((a, b) -> b.getValue() - a.getValue());
        return entries.subList(0, Math.min(topN, entries.size()));
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!statsFile.exists()) {
            plugin.getDataFolder().mkdirs();
        }
        stats = YamlConfiguration.loadConfiguration(statsFile);
    }

    private void save() {
        try { stats.save(statsFile); }
        catch (IOException e) { plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage()); }
    }
}
