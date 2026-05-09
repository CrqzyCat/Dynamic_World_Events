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
    private final BossBar bossBar;
    private BukkitTask updateTask;

    private final Set<UUID> hiddenFor = new HashSet<>();

    // Snapshot of when the scheduler last set the next-event countdown
    private long nextEventScheduledAt   = 0;
    private int  nextEventTotalSeconds  = 0;

    public BossBarManager(Dynamic_World_Events plugin) {
        this.plugin = plugin;
        bossBar = Bukkit.createBossBar(
            MessageUtil.color("&6Dynamic World Events"),
            BarColor.GREEN,
            BarStyle.SOLID
        );
        bossBar.setVisible(true);
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 20L, 20L);
    }

    // Called by EventScheduler every time a new countdown starts
    public void setNextEventCountdown(int totalSeconds) {
        this.nextEventTotalSeconds = totalSeconds;
        this.nextEventScheduledAt  = System.currentTimeMillis();
    }

    private void update() {
        EventManager em = plugin.getEventManager();

        if (em.hasActiveEvent()) {
            int secs  = em.getSecondsRemaining();
            int total = em.getActiveEvent().getDurationSeconds();
            double progress = Math.max(0.0, Math.min(1.0, (double) secs / total));

            int m = secs / 60;
            int s = secs % 60;
            bossBar.setTitle(MessageUtil.color(
                "&6" + em.getActiveEvent().getDisplayName() + " &7\u2014 &e" + m + "m " + s + "s &7remaining"
            ));
            bossBar.setProgress(progress);
            bossBar.setColor(secs <= 30 ? BarColor.RED : BarColor.YELLOW);
            bossBar.setStyle(BarStyle.SEGMENTED_10);

        } else {
            // Calculate live remaining seconds based on wall-clock elapsed time
            long elapsed = (System.currentTimeMillis() - nextEventScheduledAt) / 1000L;
            int remaining = (int) Math.max(0, nextEventTotalSeconds - elapsed);

            int m = remaining / 60;
            int s = remaining % 60;

            // Progress: 0.0 right after scheduling → 1.0 when event fires
            double progress = nextEventTotalSeconds > 0
                ? Math.max(0.0, Math.min(1.0, (double) elapsed / nextEventTotalSeconds))
                : 0.0;

            bossBar.setTitle(MessageUtil.color("&7Next event in &e" + m + "m " + s + "s"));
            bossBar.setProgress(progress);
            bossBar.setColor(BarColor.GREEN);
            bossBar.setStyle(BarStyle.SOLID);
        }
    }

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
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        if (hiddenFor.contains(uuid)) {
            hiddenFor.remove(uuid);
            bossBar.addPlayer(player);
            player.sendMessage(MessageUtil.color(prefix + "&aBoss bar enabled."));
        } else {
            hiddenFor.add(uuid);
            bossBar.removePlayer(player);
            player.sendMessage(MessageUtil.color(prefix + "&7Boss bar disabled. Use &f/dwe bossbar&7 to re-enable."));
        }
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        bossBar.removeAll();
    }
}
