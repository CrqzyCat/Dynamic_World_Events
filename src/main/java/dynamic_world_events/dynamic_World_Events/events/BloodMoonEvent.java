package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class BloodMoonEvent extends WorldEvent implements Listener {

    private final List<Entity> buffedMobs = new ArrayList<>();
    private BukkitTask noSunriseTask;
    private World world;

    public BloodMoonEvent(Dynamic_World_Events plugin) {
        super(plugin, "blood_moon", "Blood Moon");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&cMobs are stronger and more numerous tonight. Survive until dawn!"));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "Blood Moon!",
                ChatColor.RED + "Survive the night...",
                10, 80, 20
            )
        );

        // Set time to midnight if it isn't already night
        if (world.getTime() < 13000) {
            world.setTime(13000);
        }

        // Register spawn listener so we can buff newly spawned mobs
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Buff existing mobs in loaded chunks
        buffExistingMobs();

        // Prevent mobs from burning at sunrise
        noSunriseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Entity e : buffedMobs) {
                if (e != null && !e.isDead() && e instanceof LivingEntity le) {
                    le.setFireTicks(0);
                }
            }
        }, 0L, 20L);
    }

    private void buffExistingMobs() {
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            for (Entity e : chunk.getEntities()) {
                if (e instanceof Monster mob) {
                    buffMob(mob);
                }
            }
        }
    }

    private void buffMob(LivingEntity mob) {
        // Double max health
        AttributeInstance maxHp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            double newMax = maxHp.getBaseValue() * 2.0;
            maxHp.setBaseValue(newMax);
            mob.setHealth(newMax);
        }
        // Speed boost
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        // Glowing red eyes effect via particle would need NMS; glow is enough
        mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        buffedMobs.add(mob);
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!active) return;
        if (!event.getLocation().getWorld().equals(world)) return;
        if (event.getEntity() instanceof Monster mob) {
            // Small delay so the entity is fully initialized
            Bukkit.getScheduler().runTaskLater(plugin, () -> buffMob(mob), 1L);
        }
    }

    @Override
    public void end(boolean forced) {
        HandlerList.unregisterAll(this);
        if (noSunriseTask != null) { noSunriseTask.cancel(); noSunriseTask = null; }

        // Restore mob health to normal (remove buffs)
        for (Entity e : buffedMobs) {
            if (e != null && !e.isDead() && e instanceof LivingEntity le) {
                AttributeInstance maxHp = le.getAttribute(Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    double restored = maxHp.getBaseValue() / 2.0;
                    maxHp.setBaseValue(restored);
                    if (le.getHealth() > restored) le.setHealth(restored);
                }
                le.removePotionEffect(PotionEffectType.SPEED);
                le.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
        buffedMobs.clear();
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&aDawn breaks. The Blood Moon is over."));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&cBlood Moon ends in &41 minute&c!"));
        }
        if (secondsRemaining == 30 || secondsRemaining == 10) {
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.DARK_RED + "Blood Moon ends in " + secondsRemaining + "s!")
            );
        }
    }
}
