package dynamic_world_events.dynamic_World_Events;

import dynamic_world_events.dynamic_World_Events.commands.DweCommand;
import dynamic_world_events.dynamic_World_Events.listeners.PlayerJoinListener;
import dynamic_world_events.dynamic_World_Events.managers.BossBarManager;
import dynamic_world_events.dynamic_World_Events.managers.EventManager;
import dynamic_world_events.dynamic_World_Events.managers.EventScheduler;
import dynamic_world_events.dynamic_World_Events.util.DiscordWebhook;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dynamic_World_Events extends JavaPlugin {

    private static Dynamic_World_Events instance;

    private EventManager   eventManager;
    private EventScheduler eventScheduler;
    private BossBarManager bossBarManager;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.eventManager   = new EventManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.eventScheduler = new EventScheduler(this);
        this.discordWebhook = new DiscordWebhook(this);

        eventScheduler.start();

        DweCommand dweCmd = new DweCommand(this);
        getCommand("dwe").setExecutor(dweCmd);
        getCommand("dwe").setTabCompleter(dweCmd);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info(MessageUtil.color("&aDynamicWorldEvents v" + getDescription().getVersion() + " enabled!"));
    }

    @Override
    public void onDisable() {
        if (bossBarManager != null) bossBarManager.shutdown();
        if (eventScheduler != null) eventScheduler.stop();
        if (eventManager   != null) eventManager.stopCurrentEvent(true);
        getLogger().info("DynamicWorldEvents disabled.");
    }

    public void reload() {
        reloadConfig();
        bossBarManager.shutdown();
        eventScheduler.stop();
        eventManager.stopCurrentEvent(true);

        this.eventManager   = new EventManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.eventScheduler = new EventScheduler(this);
        this.discordWebhook = new DiscordWebhook(this);
        eventScheduler.start();

        getLogger().info("DynamicWorldEvents reloaded.");
    }

    public static Dynamic_World_Events getInstance() { return instance; }
    public EventManager   getEventManager()   { return eventManager;   }
    public EventScheduler getEventScheduler() { return eventScheduler; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public DiscordWebhook getDiscordWebhook() { return discordWebhook; }
}
