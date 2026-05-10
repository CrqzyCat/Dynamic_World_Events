package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class ThunderstormEvent extends WorldEvent {

    private final Random random = new Random();
    private BukkitTask strikeTask;
    private World world;

    public ThunderstormEvent(Dynamic_World_Events plugin) {
        super(plugin, "thunderstorm", "Thunderstorm");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&7A violent storm is rolling in. Seek shelter!"));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Thunderstorm!",
                ChatColor.GRAY + "Seek shelter!",
                10, 70, 20
            )
        );

        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration(getDurationSeconds() * 20 + 100);
        world.setThunderDuration(getDurationSeconds() * 20 + 100);

        int intervalTicks = plugin.getConfig().getInt("events.thunderstorm.strike-interval-ticks", 60);

        strikeTask = Bukkit.getScheduler().runTaskTimer(plugin, this::strikeNearPlayer, 40L, intervalTicks);
    }

    private void strikeNearPlayer() {
        List<? extends Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(random.nextInt(players.size()));
        Location base = target.getLocation();

        int offsetX = random.nextInt(41) - 20;
        int offsetZ = random.nextInt(41) - 20;
        Location strikeLoc = base.clone().add(offsetX, 0, offsetZ);
        strikeLoc.setY(world.getHighestBlockYAt(strikeLoc));

        boolean damaging = plugin.getConfig().getBoolean("events.thunderstorm.damaging-strikes", false);

        if (damaging) {
            world.strikeLightning(strikeLoc);
        } else {
            world.strikeLightningEffect(strikeLoc);
        }

        // Play thunder sound for all players
        world.getPlayers().forEach(p ->
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.8f,
                0.8f + random.nextFloat() * 0.4f)
        );
    }

    @Override
    public void end(boolean forced) {
        if (strikeTask != null) { strikeTask.cancel(); strikeTask = null; }
        this.active = false;

        // Clear the storm
        world.setStorm(false);
        world.setThundering(false);

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&aThe storm has passed."));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&bThe storm is weakening — &e1 minute&b remaining."));
        }
        if (secondsRemaining == 10) {
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.AQUA + "Thunderstorm ends in 10s!")
            );
        }
    }
}
