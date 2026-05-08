package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

public class DroughtEvent extends WorldEvent {

    private World world;
    private WeatherType previousWeather;
    private BukkitTask weatherTask;

    public DroughtEvent(Dynamic_World_Events plugin) {
        super(plugin, "drought", "☀ Dürre");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        previousWeather = world.hasStorm() ? WeatherType.DOWNFALL : WeatherType.CLEAR;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEreignis gestartet: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&7Die Hitze trocknet die Felder aus. Sichert eure Ernte!"));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.YELLOW + "" + ChatColor.BOLD + "☀ Dürre!",
                ChatColor.GOLD + "Die Felder verdorren...",
                10, 70, 20
            )
        );

        if (plugin.getConfig().getBoolean("events.drought.stop-rain", true)) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(getDurationSeconds() * 20 + 100);
        }

        // Keep weather clear for the whole duration (Paper resets it sometimes)
        weatherTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            world.setStorm(false);
            world.setThundering(false);
        }, 0L, 100L);
    }

    @Override
    public void end(boolean forced) {
        if (weatherTask != null) { weatherTask.cancel(); weatherTask = null; }
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEreignis beendet: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&eDie Dürre endet in &61 Minute&e."));
        }
    }
}
