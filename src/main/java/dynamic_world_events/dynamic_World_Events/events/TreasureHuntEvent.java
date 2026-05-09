package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TreasureHuntEvent extends WorldEvent {

    private final List<Location> chestLocations = new ArrayList<>();
    private World world;

    public TreasureHuntEvent(Dynamic_World_Events plugin) {
        super(plugin, "treasure_hunt", "Treasure Hunt");
    }

    @Override
    public void start(World world) {
        this.world  = world;
        this.active = true;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
        Bukkit.broadcastMessage(MessageUtil.color(prefix + "&7Treasure chests have been hidden. Find them!"));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.AQUA + "" + ChatColor.BOLD + "Treasure Hunt!",
                ChatColor.YELLOW + "Find the hidden chests!",
                10, 70, 20
            )
        );

        int count = plugin.getConfig().getInt("events.treasure_hunt.chest-count", 3);
        spawnChests(count);

        for (int i = 0; i < chestLocations.size(); i++) {
            Location loc = chestLocations.get(i);
            Bukkit.broadcastMessage(MessageUtil.color(prefix
                + "&7Chest #" + (i + 1) + " near &e"
                + loc.getBlockX() + ", " + loc.getBlockZ()));
        }
    }

    private void spawnChests(int count) {
        List<? extends Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Random random = new Random();

        // Build reward pool from config
        List<ItemStack> rewardPool = new ArrayList<>();
        List<String> rewardConfig = plugin.getConfig().getStringList("events.treasure_hunt.rewards");
        for (String entry : rewardConfig) {
            String[] parts = entry.split(":");
            Material mat = Material.matchMaterial(parts[0]);
            if (mat != null) {
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                rewardPool.add(new ItemStack(mat, amount));
            }
        }
        // Fallback loot if config is empty
        if (rewardPool.isEmpty()) {
            rewardPool.add(new ItemStack(Material.DIAMOND, 2));
            rewardPool.add(new ItemStack(Material.GOLD_INGOT, 4));
            rewardPool.add(new ItemStack(Material.IRON_INGOT, 6));
            rewardPool.add(new ItemStack(Material.EMERALD, 2));
            rewardPool.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 3));
        }

        for (int i = 0; i < count; i++) {
            Player target = players.get(random.nextInt(players.size()));
            Location base = target.getLocation();

            int offsetX = random.nextInt(101) - 50;
            int offsetZ = random.nextInt(101) - 50;
            Location loc = base.clone().add(offsetX, 0, offsetZ);
            loc.setY(world.getHighestBlockYAt(loc));

            // Set the block and force a state update before filling
            Block block = world.getBlockAt(loc);
            block.setType(Material.CHEST, false);

            // Re-fetch state AFTER setting the type
            BlockState state = block.getState();
            if (state instanceof Chest chest) {
                Inventory inv = chest.getInventory();
                inv.clear();

                // Shuffle and place items into random slots
                Collections.shuffle(rewardPool);
                List<Integer> slots = new ArrayList<>();
                for (int s = 0; s < 27; s++) slots.add(s);
                Collections.shuffle(slots);

                for (int r = 0; r < Math.min(rewardPool.size(), slots.size()); r++) {
                    inv.setItem(slots.get(r), rewardPool.get(r).clone());
                }
            }

            chestLocations.add(loc);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1, 0.5), 30, 0.3, 0.5, 0.3, 0.1);
        }
    }

    @Override
    public void end(boolean forced) {
        for (Location loc : chestLocations) {
            Block block = world.getBlockAt(loc);
            if (block.getType() == Material.CHEST) block.setType(Material.AIR);
        }
        chestLocations.clear();
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 60) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
            Bukkit.broadcastMessage(MessageUtil.color(prefix + "&eTreasure chests disappear in &61 minute&e!"));
        }
    }
}
