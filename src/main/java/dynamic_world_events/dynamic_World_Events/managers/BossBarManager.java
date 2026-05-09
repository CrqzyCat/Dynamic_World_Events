package dynamic_world_events.dynamic_World_Events.managers;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BossBarManager {

    private final Dynamic_World_Events plugin;
    private BossBar bossBar;
    private BukkitTask updateTask;

    // Players who have opted out
    private final Set<UUID> hiddenFor = new HashSet<>();

    public BossBarManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
        bossBar = Bukkit.createBossBar(
            MessageUtil.color("&6Dynamic World Events"),
            BarColor.YELLOW,
            BarStyle.SOLID
        );
        bossBar.setVisible(true);

        // Add all online players on startup
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);

        // Update every second
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 20L);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private void update() {
        EventManager em = plugin.getEventManager();
        EventScheduler es = plugin.getEventScheduler();

        if (em.hasActiveEvent()) {
            int secs = em.getSecondsRemaining();
            int total = em.getActiveEvent().getDurationSeconds();
            double progress = Math.max(0, Math.min(1.0, (double) secs / total));

            String name = em.getActiveEvent().getDisplayName();
            int m = secs / 60;
            int s = secs % 60;

            bossBar.setTitle(MessageUtil.color(
                "&6" + name + " &7— &e" + m + "m " + s + "s &7remaining"
            ));
            bossBar.setProgress(progress);
            bossBar.setColor(secs <= 30 ? BarColor.RED : BarColor.YELLOW);
            bossBar.setStyle(BarStyle.SEGMENTED_10);

        } else {
            int secs = es.getNextEventInSeconds();
            int m = secs / 60;
            int s = secs % 60;

            // Progress goes from 0 → 1 as time counts down
            int maxInterval = plugin.getConfig().getInt("settings.max-interval-minutes", 45) * 60;
            double progress = Math.max(0, Math.min(1.0, 1.0 - (double) secs / maxInterval));

            bossBar.setTitle(MessageUtil.color(
                "&7Next event in &e" + m + "m " + s + "s"
            ));
            bossBar.setProgress(progress);
            bossBar.setColor(BarColor.GREEN);
            bossBar.setStyle(BarStyle.SOLID);
        }
    }

    // ── Player management ─────────────────────────────────────────────────────

    public void addPlayer(Player player) {
        if (!hiddenFor.contains(player.getUniqueId())) {
            bossBar.addPlayer(player);
        }
    }

    public void removePlayer(Player player) {
        bossBar.removePlayer(player);
    }

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (hiddenFor.contains(uuid)) {
            hiddenFor.remove(uuid);
            bossBar.addPlayer(player);
            player.sendMessage(MessageUtil.color(
                plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                + "&aBoss bar enabled."
            ));
        } else {
            hiddenFor.add(uuid);
            bossBar.removePlayer(player);
            player.sendMessage(MessageUtil.color(
                plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                + "&7Boss bar disabled. Use &f/dwe bossbar&7 to re-enable."
            ));
        }
    }

    public boolean isHidden(Player player) {
        return hiddenFor.contains(player.getUniqueId());
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        bossBar.removeAll();
    }
}
