package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import org.bukkit.World;

/**
 * Base class for every world event.
 * Extend this and implement start(), end() and onTick().
 */
public abstract class WorldEvent {

    protected final Dynamic_World_Events plugin;
    private final String id;
    private final String displayName;
    protected boolean active = false;

    protected WorldEvent(Dynamic_World_Events plugin, String id, String displayName) {
        this.plugin      = plugin;
        this.id          = id;
        this.displayName = displayName;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Called once when the event starts. */
    public abstract void start(World world);

    /** Called once when the event ends. forced = true when stopped via command. */
    public abstract void end(boolean forced);

    /** Called every second while the event is running. */
    public abstract void onTick(int secondsRemaining);

    // ── Config helpers ────────────────────────────────────────────────────────

    public int getDurationSeconds() {
        return plugin.getConfig().getInt("events." + id + ".duration-seconds", 120);
    }

    public int getWeight() {
        return plugin.getConfig().getInt("events." + id + ".weight", 10);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("events." + id + ".enabled", true);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isActive()      { return active; }
    public void setActive(boolean active) { this.active = active; }
}
