package dynamic_world_events.dynamic_World_Events.api;

import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the Bukkit event bus just before a DWE world event starts.
 * Can be cancelled to prevent the event from running.
 *
 * Listen to this in any plugin:
 *
 *   @EventHandler
 *   public void onDWEStart(DWEEventStartEvent e) {
 *       if (e.getDWEEvent().getId().equals("blood_moon")) {
 *           e.setCancelled(true); // Block blood moon
 *       }
 *   }
 */
public class DWEEventStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final WorldEvent dweEvent;
    private final World world;
    private boolean cancelled = false;

    public DWEEventStartEvent(WorldEvent dweEvent, World world) {
        this.dweEvent = dweEvent;
        this.world    = world;
    }

    /** The DWE WorldEvent that is about to start. */
    public WorldEvent getDWEEvent() { return dweEvent; }

    /** The world the event will run in. */
    public World getWorld() { return world; }

    @Override public boolean isCancelled()            { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers()         { return HANDLERS; }
    public static HandlerList getHandlerList()         { return HANDLERS; }
}
