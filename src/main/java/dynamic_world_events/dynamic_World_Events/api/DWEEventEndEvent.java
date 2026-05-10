package dynamic_world_events.dynamic_World_Events.api;

import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the Bukkit event bus after a DWE world event ends.
 *
 * Listen to this in any plugin:
 *
 *   @EventHandler
 *   public void onDWEEnd(DWEEventEndEvent e) {
 *       if (e.getDWEEvent().getId().equals("treasure_hunt") && e.wasForced()) {
 *           // Admin stopped the treasure hunt early
 *       }
 *   }
 */
public class DWEEventEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final WorldEvent dweEvent;
    private final boolean forced;

    public DWEEventEndEvent(WorldEvent dweEvent, boolean forced) {
        this.dweEvent = dweEvent;
        this.forced   = forced;
    }

    /** The DWE WorldEvent that just ended. */
    public WorldEvent getDWEEvent() { return dweEvent; }

    /** True if the event was stopped via /dwe stop, false if it ended naturally. */
    public boolean wasForced() { return forced; }

    @Override public HandlerList getHandlers()  { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
