package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import org.bukkit.World;

import java.util.List;

/**
 * Reads per-world event settings from config.yml.
 * Falls back to global settings if no world-specific config exists.
 */
public class WorldConfigManager {

    private final Dynamic_World_Events plugin;

    public WorldConfigManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    // ── World enabled check ───────────────────────────────────────────────────

    /** Whether DWE events are enabled at all in this world. */
    public boolean isWorldEnabled(World world) {
        String key = "worlds." + world.getName() + ".enabled";
        // If no world section exists, fall back to global default (true)
        if (!plugin.getConfig().isSet("worlds." + world.getName())) return true;
        return plugin.getConfig().getBoolean(key, true);
    }

    /** Whether a specific event is allowed in this world. */
    public boolean isEventAllowedInWorld(World world, String eventId) {
        String worldName = world.getName();

        // No per-world config for this world → allow everything
        if (!plugin.getConfig().isSet("worlds." + worldName)) return true;

        // World is disabled entirely
        if (!isWorldEnabled(world)) return false;

        // Check blacklist
        List<String> blacklist = plugin.getConfig().getStringList("worlds." + worldName + ".disabled-events");
        if (blacklist.stream().anyMatch(id -> id.equalsIgnoreCase(eventId))) return false;

        // Check whitelist (if defined, only listed events are allowed)
        List<String> whitelist = plugin.getConfig().getStringList("worlds." + worldName + ".allowed-events");
        if (!whitelist.isEmpty()) {
            return whitelist.stream().anyMatch(id -> id.equalsIgnoreCase(eventId));
        }

        return true;
    }

    /** Returns the world DWE should use for events (first enabled world). */
    public World getEventWorld() {
        for (World world : plugin.getServer().getWorlds()) {
            if (isWorldEnabled(world)) return world;
        }
        return plugin.getServer().getWorlds().get(0);
    }

    /** Returns per-world min interval override, or global if not set. */
    public int getMinInterval(World world) {
        String key = "worlds." + world.getName() + ".min-interval-minutes";
        if (plugin.getConfig().isSet(key)) return plugin.getConfig().getInt(key);
        return plugin.getConfig().getInt("settings.min-interval-minutes", 20);
    }

    /** Returns per-world max interval override, or global if not set. */
    public int getMaxInterval(World world) {
        String key = "worlds." + world.getName() + ".max-interval-minutes";
        if (plugin.getConfig().isSet(key)) return plugin.getConfig().getInt(key);
        return plugin.getConfig().getInt("settings.max-interval-minutes", 45);
    }
}
