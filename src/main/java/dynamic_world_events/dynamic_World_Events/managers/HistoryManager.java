package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Keeps a rolling log of the last N completed events with timestamps.
 * Persisted to history.txt so it survives restarts.
 */
public class HistoryManager {

    private final Dynamic_World_Events plugin;
    private final File historyFile;
    private final int maxEntries;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Deque<String> entries = new ArrayDeque<>();

    public HistoryManager(Dynamic_World_Events plugin) {
        this.plugin      = plugin;
        this.maxEntries  = plugin.getConfig().getInt("history.max-entries", 20);
        this.historyFile = new File(plugin.getDataFolder(), "history.txt");
        load();
    }

    // ── Record ────────────────────────────────────────────────────────────────

    public void record(String eventDisplayName, boolean forced) {
        String timestamp = LocalDateTime.now().format(fmt);
        String suffix    = forced ? " (stopped)" : "";
        String line      = "[" + timestamp + "] " + eventDisplayName + suffix;

        entries.addFirst(line);
        while (entries.size() > maxEntries) entries.removeLast();
        save();
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** Returns the last N entries, newest first. */
    public List<String> getLast(int count) {
        List<String> result = new ArrayList<>(entries);
        return result.subList(0, Math.min(count, result.size()));
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        if (!historyFile.exists()) return;
        try {
            List<String> lines = Files.readAllLines(historyFile.toPath());
            // File is stored newest-first
            for (String line : lines) {
                if (!line.isBlank()) entries.addLast(line);
            }
            // Trim to max
            while (entries.size() > maxEntries) entries.removeLast();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load history.txt: " + e.getMessage());
        }
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            Files.write(historyFile.toPath(), new ArrayList<>(entries),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save history.txt: " + e.getMessage());
        }
    }
}
