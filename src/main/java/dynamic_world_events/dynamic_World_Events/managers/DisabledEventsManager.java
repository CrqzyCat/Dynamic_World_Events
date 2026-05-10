package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists the set of runtime-disabled event IDs to disk.
 * Survives server restarts. Separate from config.yml so admins
 * can toggle events live without editing files manually.
 */
public class DisabledEventsManager {

    private final Dynamic_World_Events plugin;
    private final File dataFile;
    private final Set<String> disabledIds = new HashSet<>();

    public DisabledEventsManager(Dynamic_World_Events plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "disabled_events.txt");
        load();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isDisabled(String id) {
        return disabledIds.contains(id.toLowerCase());
    }

    /** Returns true if the event was disabled (false if it was already disabled). */
    public boolean disable(String id) {
        boolean added = disabledIds.add(id.toLowerCase());
        if (added) save();
        return added;
    }

    /** Returns true if the event was re-enabled (false if it wasn't disabled). */
    public boolean enable(String id) {
        boolean removed = disabledIds.remove(id.toLowerCase());
        if (removed) save();
        return removed;
    }

    public Set<String> getDisabledIds() {
        return new HashSet<>(disabledIds);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) return;
        try {
            Files.lines(dataFile.toPath())
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .forEach(disabledIds::add);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load disabled_events.txt: " + e.getMessage());
        }
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            Files.write(dataFile.toPath(), disabledIds,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save disabled_events.txt: " + e.getMessage());
        }
    }
}
