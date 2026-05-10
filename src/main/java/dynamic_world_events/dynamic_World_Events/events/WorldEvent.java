package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import org.bukkit.World;

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

    public abstract void start(World world);
    public abstract void end(boolean forced);
    public abstract void onTick(int secondsRemaining);

    public int getDurationSeconds() {
        return plugin.getConfig().getInt("events." + id + ".duration-seconds", 120);
    }

    public int getWeight() {
        return plugin.getConfig().getInt("events." + id + ".weight", 10);
    }

    /**
     * Checks all three layers:
     * 1. config.yml enabled flag
     * 2. runtime disable via /dwe disable
     * 3. per-world config for the current event world
     */
    public boolean isEnabled() {
        if (!plugin.getConfig().getBoolean("events." + id + ".enabled", true)) return false;
        if (plugin.getDisabledEventsManager().isDisabled(id)) return false;

        World eventWorld = plugin.getWorldConfigManager().getEventWorld();
        if (!plugin.getWorldConfigManager().isEventAllowedInWorld(eventWorld, id)) return false;

        return true;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isActive()      { return active; }
    public void setActive(boolean active) { this.active = active; }
}
