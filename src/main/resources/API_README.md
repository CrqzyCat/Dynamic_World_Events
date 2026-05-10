# Dynamic World Events — Developer API

## Setup

Add DWE as a dependency in your `plugin.yml`:

```yaml
depend: [Dynamic_World_Events]
# or soft-depend if optional:
softdepend: [Dynamic_World_Events]
```

Get the API instance in your plugin's `onEnable`:

```java
Plugin dwePlugin = Bukkit.getPluginManager().getPlugin("Dynamic_World_Events");
if (dwePlugin instanceof Dynamic_World_Events dwe) {
    DWEApi api = dwe.getApi();
}
```

---

## Registering a Custom Event

Extend `WorldEvent` and implement the three lifecycle methods:

```java
public class MyCustomEvent extends WorldEvent {

    public MyCustomEvent(JavaPlugin plugin) {
        // (Dynamic_World_Events plugin instance, unique id, display name)
        super((Dynamic_World_Events) Bukkit.getPluginManager().getPlugin("Dynamic_World_Events"),
              "my_custom_event", "My Custom Event");
    }

    @Override
    public void start(World world) {
        // Called once when the event begins
        Bukkit.broadcastMessage("My custom event started!");
    }

    @Override
    public void end(boolean forced) {
        // Called once when the event ends
        // forced = true if stopped via /dwe stop
        Bukkit.broadcastMessage("My custom event ended!");
    }

    @Override
    public void onTick(int secondsRemaining) {
        // Called every second while active
        if (secondsRemaining == 10) {
            Bukkit.broadcastMessage("Event ends in 10 seconds!");
        }
    }
}
```

Register it in your `onEnable`:

```java
api.registerEvent(new MyCustomEvent(this));
```

It will now appear in the random pool and be configurable via `config.yml`:

```yaml
events:
  my_custom_event:
    enabled: true
    weight: 15
    duration-seconds: 120
```

---

## Listening to Event Lifecycle

DWE fires standard Bukkit events you can listen to:

```java
@EventHandler
public void onDWEStart(DWEEventStartEvent event) {
    String id = event.getDWEEvent().getId();
    World world = event.getWorld();

    // Cancel the event before it starts
    if (id.equals("blood_moon") && someCondition) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onDWEEnd(DWEEventEndEvent event) {
    String id   = event.getDWEEvent().getId();
    boolean forced = event.wasForced();
    // React after an event ends
}
```

---

## Controlling Events Programmatically

```java
// Start a specific event
api.startEvent("blood_moon");

// Start a weighted random event
api.startRandomEvent();

// Stop the current event
api.stopCurrentEvent(true);

// Query state
boolean active = api.hasActiveEvent();
Optional<WorldEvent> current = api.getActiveEvent();
int secondsLeft = api.getSecondsRemaining();
int secondsUntilNext = api.getSecondsUntilNextEvent();

// Find a registered event by ID
Optional<WorldEvent> event = api.getEventById("meteor");

// Check if an event is runtime-disabled
boolean disabled = api.isEventDisabled("haunting");
```

---

## Available Imports

```java
import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.api.DWEApi;
import dynamic_world_events.dynamic_World_Events.api.DWEEventStartEvent;
import dynamic_world_events.dynamic_World_Events.api.DWEEventEndEvent;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
```
