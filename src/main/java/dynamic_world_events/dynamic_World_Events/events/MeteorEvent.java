package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class MeteorEvent extends WorldEvent {

    private final Random random = new Random();
    private BukkitTask meteorTask;
    private World world;
    private int meteorsLeft;

    public MeteorEvent(Dynamic_World_Events plugin) {
        super(plugin, "meteor", "☄ Meteoritenschauer");
    }

    @Override
    public void start(World world) {
        this.world = world;
        this.active = true;
        this.meteorsLeft = plugin.getConfig().getInt("events.meteor.meteor-count", 5);

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEreignis gestartet: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.GOLD + "" + ChatColor.BOLD + "☄ Meteoritenschauer!",
                ChatColor.YELLOW + "Suche Deckung!",
                10, 60, 20
            )
        );

        world.playSound(world.getSpawnLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1f, 0.5f);

        int totalDuration = getDurationSeconds();
        int interval = Math.max(20, (totalDuration * 20) / meteorsLeft);

        meteorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnMeteor, 60L, interval);
    }

    private void spawnMeteor() {
        if (meteorsLeft <= 0 || world == null) {
            if (meteorTask != null) meteorTask.cancel();
            return;
        }

        List<? extends Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(random.nextInt(players.size()));
        Location center = target.getLocation();

        int offsetX = random.nextInt(81) - 40;
        int offsetZ = random.nextInt(81) - 40;

        Location spawnLoc = new Location(world,
            center.getX() + offsetX,
            world.getMaxHeight() - 5,
            center.getZ() + offsetZ
        );

        Fireball fireball = (Fireball) world.spawnEntity(spawnLoc, EntityType.FIREBALL);
        fireball.setDirection(new Vector(0, -1, 0).normalize());
        fireball.setYield((float) plugin.getConfig().getDouble("events.meteor.explosion-power", 2.5));
        fireball.setIsIncendiary(false);

        world.spawnParticle(Particle.FLAME, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1);
        meteorsLeft--;
    }

    @Override
    public void end(boolean forced) {
        if (meteorTask != null) { meteorTask.cancel(); meteorTask = null; }
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEreignis beendet: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 10) {
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.RED + "☄ Meteoritenschauer endet in 10 Sekunden!")
            );
        }
    }
}
