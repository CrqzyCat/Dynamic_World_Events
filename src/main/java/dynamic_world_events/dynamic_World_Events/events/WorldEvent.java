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
     * An event is eligible if BOTH conditions are true:
     * 1. enabled: true in config.yml
     * 2. NOT runtime-disabled via /dwe disable
     */
    public boolean isEnabled() {
        boolean configEnabled  = plugin.getConfig().getBoolean("events." + id + ".enabled", true);
        boolean runtimeEnabled = !plugin.getDisabledEventsManager().isDisabled(id);
        return configEnabled && runtimeEnabled;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isActive()      { return active; }
    public void setActive(boolean active) { this.active = active; }
}
