package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HauntingEvent extends WorldEvent {

    private final Random random = new Random();
    private final List<Entity> spawnedPhantoms = new ArrayList<>();
    private BukkitTask spawnTask;
    private BukkitTask ambientTask;
    private World world;

    public HauntingEvent(Dynamic_World_Events plugin) {
        super(plugin, "haunting", "Haunting");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&5Something is watching from the darkness..."));

        world.getPlayers().forEach(p -> {
            p.sendTitle(
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Haunting!",
                ChatColor.LIGHT_PURPLE + "Something watches from the dark...",
                10, 80, 20
            );
            // Apply blindness and darkness briefly for atmosphere
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false));
        });

        // Set to night for atmosphere
        if (world.getTime() < 13000 || world.getTime() > 23000) {
            world.setTime(14000);
        }

        int phantomsPerPlayer = plugin.getConfig().getInt("events.haunting.phantoms-per-player", 2);
        int spawnInterval     = plugin.getConfig().getInt("events.haunting.spawn-interval-ticks", 100);

        // Initial phantom wave
        spawnPhantoms(phantomsPerPlayer);

        // Keep spawning waves throughout the event
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
            spawnPhantoms(1), spawnInterval, spawnInterval
        );

        // Eerie ambient sounds
        ambientTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<? extends Player> players = world.getPlayers();
            if (players.isEmpty()) return;
            Player p = players.get(random.nextInt(players.size()));
            Sound[] eerieBoSounds = {
                Sound.AMBIENT_CAVE,
                Sound.ENTITY_PHANTOM_AMBIENT,
                Sound.ENTITY_PHANTOM_FLAP,
                Sound.ENTITY_ENDERMAN_STARE
            };
            Sound sound = eerieBoSounds[random.nextInt(eerieBoSounds.length)];
            p.playSound(p.getLocation(), sound, SoundCategory.AMBIENT, 0.6f, 0.7f + random.nextFloat() * 0.3f);
        }, 60L, 80L);
    }

    private void spawnPhantoms(int perPlayer) {
        List<? extends Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        for (Player player : players) {
            for (int i = 0; i < perPlayer; i++) {
                Location base = player.getLocation();
                int offsetX = random.nextInt(21) - 10;
                int offsetZ = random.nextInt(21) - 10;
                Location spawnLoc = new Location(world,
                    base.getX() + offsetX,
                    base.getY() + 10 + random.nextInt(5),
                    base.getZ() + offsetZ
                );

                Phantom phantom = (Phantom) world.spawnEntity(spawnLoc, EntityType.PHANTOM);
                phantom.setCustomName(ChatColor.DARK_PURPLE + "Specter");
                phantom.setCustomNameVisible(true);
                phantom.setTarget(player);

                // Make phantoms larger for more menace
                int size = plugin.getConfig().getInt("events.haunting.phantom-size", 2);
                phantom.setSize(size);

                spawnedPhantoms.add(phantom);
            }
        }
    }

    @Override
    public void end(boolean forced) {
        if (spawnTask   != null) { spawnTask.cancel();   spawnTask   = null; }
        if (ambientTask != null) { ambientTask.cancel(); ambientTask = null; }

        spawnedPhantoms.stream()
            .filter(e -> e != null && !e.isDead())
            .forEach(Entity::remove);
        spawnedPhantoms.clear();

        // Remove potion effects from all players
        world.getPlayers().forEach(p -> {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            p.removePotionEffect(PotionEffectType.DARKNESS);
        });

        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&aThe spirits have retreated. For now."));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&5The haunting grows weaker — &d1 minute&5 remaining."));
        }
        if (secondsRemaining == 30 || secondsRemaining == 10) {
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.DARK_PURPLE + "Haunting ends in " + secondsRemaining + "s!")
            );
        }
    }
}
