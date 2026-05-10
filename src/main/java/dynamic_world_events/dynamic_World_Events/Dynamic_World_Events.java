package dynamic_world_events.dynamic_World_Events;

import dynamic_world_events.dynamic_World_Events.api.DWEApi;
import dynamic_world_events.dynamic_World_Events.commands.DweCommand;
import dynamic_world_events.dynamic_World_Events.listeners.PlayerJoinListener;
import dynamic_world_events.dynamic_World_Events.listeners.StatisticsListener;
import dynamic_world_events.dynamic_World_Events.managers.*;
import dynamic_world_events.dynamic_World_Events.util.DiscordWebhook;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dynamic_World_Events extends JavaPlugin {

    private static Dynamic_World_Events instance;

    private DWEApi                 api;
    private DisabledEventsManager  disabledEventsManager;
    private StatisticsManager      statisticsManager;
    private WorldConfigManager     worldConfigManager;
    private SeasonalManager        seasonalManager;
    private EventChainManager      eventChainManager;
    private EventScheduleManager   eventScheduleManager;
    private EventManager           eventManager;
    private BossBarManager         bossBarManager;
    private EventScheduler         eventScheduler;
    private DiscordWebhook         discordWebhook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.disabledEventsManager = new DisabledEventsManager(this);
        this.statisticsManager     = new StatisticsManager(this);
        this.worldConfigManager    = new WorldConfigManager(this);
        this.seasonalManager       = new SeasonalManager(this);
        this.eventChainManager     = new EventChainManager(this);
        this.eventScheduleManager  = new EventScheduleManager(this);
        this.eventManager          = new EventManager(this);
        this.bossBarManager        = new BossBarManager(this);
        this.eventScheduler        = new EventScheduler(this);
        this.discordWebhook        = new DiscordWebhook(this);
        this.api                   = new DWEApi(this);

        eventScheduler.start();
        eventScheduleManager.start();

        DweCommand dweCmd = new DweCommand(this);
        getCommand("dwe").setExecutor(dweCmd);
        getCommand("dwe").setTabCompleter(dweCmd);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new StatisticsListener(this), this);

        getLogger().info(MessageUtil.color("&aDynamicWorldEvents v" + getDescription().getVersion() + " enabled!"));
    }

    @Override
    public void onDisable() {
        if (bossBarManager       != null) bossBarManager.shutdown();
        if (eventScheduler       != null) eventScheduler.stop();
        if (eventScheduleManager != null) eventScheduleManager.stop();
        if (eventChainManager    != null) eventChainManager.cancelAll();
        if (eventManager         != null) eventManager.stopCurrentEvent(true);
        getLogger().info("DynamicWorldEvents disabled.");
    }

    public void reload() {
        reloadConfig();
        bossBarManager.shutdown();
        eventScheduler.stop();
        eventScheduleManager.stop();
        eventChainManager.cancelAll();
        eventManager.stopCurrentEvent(true);

        this.disabledEventsManager = new DisabledEventsManager(this);
        this.statisticsManager     = new StatisticsManager(this);
        this.worldConfigManager    = new WorldConfigManager(this);
        this.seasonalManager       = new SeasonalManager(this);
        this.eventChainManager     = new EventChainManager(this);
        this.eventScheduleManager  = new EventScheduleManager(this);
        this.eventManager          = new EventManager(this);
        this.bossBarManager        = new BossBarManager(this);
        this.eventScheduler        = new EventScheduler(this);
        this.discordWebhook        = new DiscordWebhook(this);
        this.api                   = new DWEApi(this);

        eventScheduler.start();
        eventScheduleManager.start();

        getLogger().info("DynamicWorldEvents reloaded.");
    }

    public static Dynamic_World_Events getInstance()             { return instance; }
    public DWEApi                  getApi()                      { return api; }
    public DisabledEventsManager   getDisabledEventsManager()    { return disabledEventsManager; }
    public StatisticsManager       getStatisticsManager()        { return statisticsManager; }
    public WorldConfigManager      getWorldConfigManager()       { return worldConfigManager; }
    public SeasonalManager         getSeasonalManager()          { return seasonalManager; }
    public EventChainManager       getEventChainManager()        { return eventChainManager; }
    public EventScheduleManager    getEventScheduleManager()     { return eventScheduleManager; }
    public EventManager            getEventManager()             { return eventManager; }
    public EventScheduler          getEventScheduler()           { return eventScheduler; }
    public BossBarManager          getBossBarManager()           { return bossBarManager; }
    public DiscordWebhook          getDiscordWebhook()           { return discordWebhook; }
}
