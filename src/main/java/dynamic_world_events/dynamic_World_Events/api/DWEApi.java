package dynamic_world_events.dynamic_World_Events.api;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;

import java.util.List;
import java.util.Optional;

/**
 * Public API for Dynamic World Events.
 *
 * Other plugins can use this to:
 *   - Register custom events
 *   - Start / stop events programmatically
 *   - Query the current event state
 *   - Listen to event lifecycle via Bukkit events
 *
 * Usage example (in another plugin's onEnable):
 *
 *   Plugin dwePlugin = Bukkit.getPluginManager().getPlugin("Dynamic_World_Events");
 *   if (dwePlugin instanceof Dynamic_World_Events dwe) {
 *       DWEApi api = dwe.getApi();
 *       api.registerEvent(new MyCustomEvent(this));
 *   }
 */
public class DWEApi {

    private final Dynamic_World_Events plugin;

    public DWEApi(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Register a custom event so it can appear in the random pool.
     * Call this in your plugin's onEnable, after DWE has loaded.
     *
     * @param event Your WorldEvent implementation.
     * @throws IllegalArgumentException if an event with the same ID is already registered.
     */
    public void registerEvent(WorldEvent event) {
        boolean duplicate = plugin.getEventManager().getRegisteredEvents().stream()
            .anyMatch(e -> e.getId().equalsIgnoreCase(event.getId()));
        if (duplicate) {
            throw new IllegalArgumentException(
                "[DWE API] Event ID '" + event.getId() + "' is already registered."
            );
        }
        plugin.getEventManager().registerEvent(event);
        plugin.getLogger().info("[DWE API] External event registered: " + event.getId()
            + " by " + event.getClass().getSimpleName());
    }

    /**
     * Unregister a previously registered custom event by ID.
     * Has no effect if the event is not registered or currently active.
     *
     * @return true if the event was found and removed.
     */
    public boolean unregisterEvent(String eventId) {
        boolean removed = plugin.getEventManager().getRegisteredEvents()
            .removeIf(e -> e.getId().equalsIgnoreCase(eventId)
                && !e.isActive());
        if (removed) plugin.getLogger().info("[DWE API] Event unregistered: " + eventId);
        return removed;
    }

    // ── Control ───────────────────────────────────────────────────────────────

    /**
     * Start a specific event by ID.
     * @return true if the event was started successfully.
     */
    public boolean startEvent(String eventId) {
        return plugin.getEventManager().startEventById(eventId);
    }

    /**
     * Start a weighted random event from the enabled pool.
     * @return true if an event was started.
     */
    public boolean startRandomEvent() {
        return plugin.getEventManager().startRandomEvent();
    }

    /**
     * Stop the currently active event.
     * @param forced true = forced stop (same as /dwe stop), false = natural end
     */
    public void stopCurrentEvent(boolean forced) {
        plugin.getEventManager().stopCurrentEvent(forced);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /** @return true if any event is currently running. */
    public boolean hasActiveEvent() {
        return plugin.getEventManager().hasActiveEvent();
    }

    /**
     * @return The currently active event, or empty if none is running.
     */
    public Optional<WorldEvent> getActiveEvent() {
        return Optional.ofNullable(plugin.getEventManager().getActiveEvent());
    }

    /** @return Seconds remaining in the current event, or 0 if none. */
    public int getSecondsRemaining() {
        return plugin.getEventManager().getSecondsRemaining();
    }

    /** @return All registered events (built-in and custom). */
    public List<WorldEvent> getRegisteredEvents() {
        return plugin.getEventManager().getRegisteredEvents();
    }

    /**
     * @return A registered event by ID, or empty if not found.
     */
    public Optional<WorldEvent> getEventById(String eventId) {
        return plugin.getEventManager().getRegisteredEvents().stream()
            .filter(e -> e.getId().equalsIgnoreCase(eventId))
            .findFirst();
    }

    /** @return true if the given event ID is currently disabled via /dwe disable. */
    public boolean isEventDisabled(String eventId) {
        return plugin.getDisabledEventsManager().isDisabled(eventId);
    }

    /** @return Seconds until the next scheduled random event. */
    public int getSecondsUntilNextEvent() {
        return plugin.getEventScheduler().getNextEventInSeconds();
    }
}
