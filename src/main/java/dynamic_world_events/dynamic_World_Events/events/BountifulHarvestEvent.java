package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BountifulHarvestEvent extends WorldEvent implements Listener {

    private final Random random = new Random();
    private BukkitTask growTask;
    private BukkitTask reminderTask;
    private World world;

    private static final Set<Material> CROPS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.MELON_STEM, Material.PUMPKIN_STEM,
        Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH
    );

    public BountifulHarvestEvent(Dynamic_World_Events plugin) {
        super(plugin, "bountiful_harvest", "Bountiful Harvest");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&aNature blesses the land! Crops grow faster and animals breed freely."));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.GREEN + "" + ChatColor.BOLD + "Bountiful Harvest!",
                ChatColor.YELLOW + "Nature blesses the land!",
                10, 70, 20
            )
        );

        // Instantly grow all crops in loaded chunks near players
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Block> cropBlocks = new ArrayList<>();
            for (Player p : world.getPlayers()) {
                for (int cx = -3; cx <= 3; cx++) {
                    for (int cz = -3; cz <= 3; cz++) {
                        org.bukkit.Chunk chunk = world.getChunkAt(
                            (p.getLocation().getBlockX() >> 4) + cx,
                            (p.getLocation().getBlockZ() >> 4) + cz
                        );
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                    Block block = chunk.getBlock(x, y, z);
                                    if (CROPS.contains(block.getType())) {
                                        cropBlocks.add(block);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Apply growth on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Block block : cropBlocks) {
                    if (block.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(ageable.getMaximumAge());
                        block.setBlockData(ageable);
                    }
                }
            });
        });

        // Register listener for double drops on crop harvest
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Periodically grow crops every 30 seconds
        growTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCropGrowth, 600L, 600L);

        // Reminder actionbar
        reminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.GREEN + "Bountiful Harvest active — double crop drops!")
            ), 60 * 20L, 60 * 20L
        );
    }

    @EventHandler
    public void onCropHarvest(BlockBreakEvent event) {
        if (!active) return;
        if (!event.getBlock().getWorld().equals(world)) return;
        if (!CROPS.contains(event.getBlock().getType())) return;
        if (!(event.getBlock().getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        // Double the drops
        boolean doubleDrop = plugin.getConfig().getBoolean("events.bountiful_harvest.double-drops", true);
        if (!doubleDrop) return;

        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack drop : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
            world.dropItemNaturally(loc, drop);
        }
    }

    private void tickCropGrowth() {
        for (Player p : world.getPlayers()) {
            org.bukkit.Chunk chunk = p.getLocation().getChunk();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (CROPS.contains(block.getType()) && block.getBlockData() instanceof Ageable ageable) {
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                ageable.setAge(Math.min(ageable.getAge() + 2, ageable.getMaximumAge()));
                                block.setBlockData(ageable);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void end(boolean forced) {
        HandlerList.unregisterAll(this);
        if (growTask     != null) { growTask.cancel();     growTask     = null; }
        if (reminderTask != null) { reminderTask.cancel(); reminderTask = null; }
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&7Nature's blessing fades. Happy farming!"));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&aHarvest blessing ends in &21 minute&a!"));
        }
        if (secondsRemaining == 10) {
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.GREEN + "Bountiful Harvest ends in 10s!")
            );
        }
    }
}
