package dynamic_world_events.dynamic_World_Events;

import dynamic_world_events.dynamic_World_Events.commands.EventAdminCommand;
import dynamic_world_events.dynamic_World_Events.commands.EventCommand;
import dynamic_world_events.dynamic_World_Events.managers.EventManager;
import dynamic_world_events.dynamic_World_Events.managers.EventScheduler;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dynamic_World_Events extends JavaPlugin {

    private static Dynamic_World_Events instance;

    private EventManager eventManager;
    private EventScheduler eventScheduler;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config.yml if it doesn't exist yet
        saveDefaultConfig();

        // Initialize managers
        this.eventManager   = new EventManager(this);
        this.eventScheduler = new EventScheduler(this);

        // Start the automatic event scheduler
        eventScheduler.start();

        // Register commands
        EventAdminCommand adminCmd = new EventAdminCommand(this);
        getCommand("events").setExecutor(new EventCommand(this));
        getCommand("eventstart").setExecutor(adminCmd);
        getCommand("eventstart").setTabCompleter(adminCmd);
        getCommand("eventstop").setExecutor(adminCmd);
        getCommand("eventreload").setExecutor(adminCmd);

        getLogger().info(MessageUtil.color("&a DynamicWorldEvents v" + getDescription().getVersion() + " enabled!"));
    }

    @Override
    public void onDisable() {
        if (eventScheduler != null) eventScheduler.stop();
        if (eventManager   != null) eventManager.stopCurrentEvent(true);

        getLogger().info("DynamicWorldEvents disabled.");
    }

    // ── Reload (called by /eventreload) ──────────────────────────────────────

    public void reload() {
        reloadConfig();

        eventScheduler.stop();
        eventManager.stopCurrentEvent(true);

        this.eventManager   = new EventManager(this);
        this.eventScheduler = new EventScheduler(this);
        eventScheduler.start();

        getLogger().info("DynamicWorldEvents reloaded.");
    }

    // ── Static accessor ───────────────────────────────────────────────────────

    public static Dynamic_World_Events getInstance() { return instance; }

    public EventManager   getEventManager()   { return eventManager;   }
    public EventScheduler getEventScheduler() { return eventScheduler; }
}
