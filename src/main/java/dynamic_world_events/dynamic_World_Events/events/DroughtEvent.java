package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

public class DroughtEvent extends WorldEvent {

    private World world;
    private BukkitTask weatherTask;

    public DroughtEvent(Dynamic_World_Events plugin) {
        super(plugin, "drought", "Drought");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&7The heat is drying out the fields. Protect your crops!"));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Drought!",
                ChatColor.GOLD + "The fields are withering...",
                10, 70, 20
            )
        );

        if (plugin.getConfig().getBoolean("events.drought.stop-rain", true)) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(getDurationSeconds() * 20 + 100);
        }

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
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&eDrought ends in &61 minute&e."));
        }
    }
}
