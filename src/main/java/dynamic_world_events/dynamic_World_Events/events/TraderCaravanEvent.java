package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TraderCaravanEvent extends WorldEvent {

    private final List<Entity> spawnedEntities = new ArrayList<>();
    private BukkitTask reminderTask;
    private World world;

    public TraderCaravanEvent(Dynamic_World_Events plugin) {
        super(plugin, "trader_caravan", "Trader Caravan");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&7Rare traders have appeared - find them!"));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.GREEN + "" + ChatColor.BOLD + "Trader Caravan!",
                ChatColor.YELLOW + "Rare goods available!",
                10, 70, 20
            )
        );

        int traderCount = plugin.getConfig().getInt("events.trader_caravan.trader-count", 3);
        spawnTraders(traderCount);

        reminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.GREEN + "Trader Caravan active! Use /events")
            ), 60 * 20L, 60 * 20L
        );
    }

    private void spawnTraders(int count) {
        List<? extends Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Random random = new Random();
        for (int i = 0; i < count; i++) {
            Player target = players.get(random.nextInt(players.size()));
            Location base = target.getLocation();

            int offsetX = random.nextInt(61) - 30;
            int offsetZ = random.nextInt(61) - 30;
            Location loc = base.clone().add(offsetX, 0, offsetZ);
            loc.setY(world.getHighestBlockYAt(loc) + 1);

            WanderingTrader trader = (WanderingTrader) world.spawnEntity(loc, EntityType.WANDERING_TRADER);
            trader.setCustomName(ChatColor.GOLD + "Caravan Trader");
            trader.setCustomNameVisible(true);
            trader.setAI(false);
            trader.setInvulnerable(true);

            TraderLlama llama = (TraderLlama) world.spawnEntity(loc.clone().add(1, 0, 0), EntityType.TRADER_LLAMA);
            llama.setCustomName(ChatColor.GRAY + "Caravan Llama");
            llama.setCustomNameVisible(true);

            spawnedEntities.add(trader);
            spawnedEntities.add(llama);
        }
    }

    @Override
    public void end(boolean forced) {
        if (reminderTask != null) { reminderTask.cancel(); reminderTask = null; }

        spawnedEntities.stream()
            .filter(e -> e != null && !e.isDead())
            .forEach(Entity::remove);
        spawnedEntities.clear();
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&eCaravan leaves in &61 minute&e!"));
        }
    }
}
