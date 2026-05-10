package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.*;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class EventManager {

    private final Dynamic_World_Events plugin;
    private final List<WorldEvent> registeredEvents = new ArrayList<>();
    private final Random random = new Random();

    private WorldEvent activeEvent   = null;
    private BukkitTask tickTask      = null;
    private int secondsRemaining     = 0;

    public EventManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
        registerDefaultEvents();
    }

    private void registerDefaultEvents() {
        registeredEvents.add(new MeteorEvent(plugin));
        registeredEvents.add(new InvasionEvent(plugin));
        registeredEvents.add(new TraderCaravanEvent(plugin));
        registeredEvents.add(new DroughtEvent(plugin));
        registeredEvents.add(new TreasureHuntEvent(plugin));
        registeredEvents.add(new BloodMoonEvent(plugin));
        registeredEvents.add(new ThunderstormEvent(plugin));
        registeredEvents.add(new HauntingEvent(plugin));
        registeredEvents.add(new BountifulHarvestEvent(plugin));
    }

    public void registerEvent(WorldEvent event) {
        registeredEvents.add(event);
    }

    public boolean startRandomEvent() {
        List<WorldEvent> pool = registeredEvents.stream()
            .filter(WorldEvent::isEnabled)
            .toList();
        if (pool.isEmpty()) return false;

        int totalWeight = pool.stream().mapToInt(WorldEvent::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        WorldEvent chosen = null;
        for (WorldEvent e : pool) {
            cursor += e.getWeight();
            if (roll < cursor) { chosen = e; break; }
        }
        if (chosen == null) chosen = pool.get(0);
        return startEvent(chosen);
    }

    public boolean startEventById(String id) {
        if (activeEvent != null) return false;
        for (WorldEvent e : registeredEvents) {
            if (e.getId().equalsIgnoreCase(id)) return startEvent(e);
        }
        return false;
    }

    private boolean startEvent(WorldEvent event) {
        if (activeEvent != null) return false;

        World world = Bukkit.getWorlds().get(0);
        activeEvent = event;
        secondsRemaining = event.getDurationSeconds();

        try {
            event.start(world);
            event.setActive(true);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Error starting event " + event.getId(), ex);
            activeEvent = null;
            return false;
        }

        plugin.getDiscordWebhook().sendEventStart(event.getDisplayName());

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            secondsRemaining--;
            try { event.onTick(secondsRemaining); }
            catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Tick error: " + event.getId(), ex); }
            if (secondsRemaining <= 0) stopCurrentEvent(false);
        }, 20L, 20L);

        return true;
    }

    public void stopCurrentEvent(boolean forced) {
        if (activeEvent == null) return;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }

        // Record participation for all online players
        String eventId = activeEvent.getId();
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getStatisticsManager().recordEventParticipation(p.getUniqueId(), p.getName(), eventId);
        }

        try { activeEvent.end(forced); }
        catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Error ending event " + activeEvent.getId(), ex); }

        plugin.getDiscordWebhook().sendEventEnd(activeEvent.getDisplayName());

        activeEvent.setActive(false);
        activeEvent = null;
        secondsRemaining = 0;
    }

    public WorldEvent getActiveEvent()            { return activeEvent; }
    public boolean    hasActiveEvent()            { return activeEvent != null; }
    public int        getSecondsRemaining()       { return secondsRemaining; }
    public List<WorldEvent> getRegisteredEvents() { return registeredEvents; }
}
