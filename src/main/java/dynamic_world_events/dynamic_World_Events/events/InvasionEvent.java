package dynamic_world_events.dynamic_World_Events.events;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InvasionEvent extends WorldEvent {

    private final Random random = new Random();
    private final List<Entity> spawnedMobs = new ArrayList<>();
    private BukkitTask spawnTask;
    private World world;
    private int mobsLeft;

    public InvasionEvent(Dynamic_World_Events plugin) {
        super(plugin, "invasion", "Mob Invasion");
    }

    @Override
    public void start(World world) {
        this.world   = world;
        this.active  = true;
        this.mobsLeft = plugin.getConfig().getInt("events.invasion.mob-count", 15);

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-start", "&6&lEvent started: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));

        world.getPlayers().forEach(p ->
            p.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "Mob Invasion!",
                ChatColor.YELLOW + "Fight together!",
                10, 70, 20
            )
        );

        world.playSound(world.getSpawnLocation(), Sound.EVENT_RAID_HORN, SoundCategory.HOSTILE, 1f, 1f);

        int interval = Math.max(10, (getDurationSeconds() * 20) / mobsLeft);
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnMob, 40L, interval);
    }

    private void spawnMob() {
        if (mobsLeft <= 0 || world == null) {
            if (spawnTask != null) spawnTask.cancel();
            return;
        }

        List<? extends Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(random.nextInt(players.size()));
        Location base = target.getLocation();

        int offsetX = random.nextInt(41) - 20;
        int offsetZ = random.nextInt(41) - 20;
        Location spawnLoc = base.clone().add(offsetX, 0, offsetZ);
        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

        String mobTypeName = plugin.getConfig().getString("events.invasion.mob-type", "ZOMBIE").toUpperCase();
        EntityType type;
        try { type = EntityType.valueOf(mobTypeName); }
        catch (IllegalArgumentException e) { type = EntityType.ZOMBIE; }

        LivingEntity mob = (LivingEntity) world.spawnEntity(spawnLoc, type);
        mob.setCustomName(ChatColor.RED + "Invader");
        mob.setCustomNameVisible(true);

        boolean giveArmor   = plugin.getConfig().getBoolean("events.invasion.give-armor", true);
        boolean giveWeapons = plugin.getConfig().getBoolean("events.invasion.give-weapons", true);

        EntityEquipment eq = mob.getEquipment();
        if (eq != null) {
            if (giveArmor) {
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                eq.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                eq.setBoots(new ItemStack(Material.IRON_BOOTS));
                eq.setHelmetDropChance(0f);
                eq.setChestplateDropChance(0f);
                eq.setLeggingsDropChance(0f);
                eq.setBootsDropChance(0f);
            }
            if (giveWeapons && mob instanceof Zombie) {
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                sword.addEnchantment(Enchantment.SHARPNESS, 1);
                eq.setItemInMainHand(sword);
                eq.setItemInMainHandDropChance(0f);
            }
        }

        spawnedMobs.add(mob);
        mobsLeft--;
    }

    @Override
    public void end(boolean forced) {
        if (spawnTask != null) { spawnTask.cancel(); spawnTask = null; }

        spawnedMobs.stream()
            .filter(e -> e != null && !e.isDead())
            .forEach(Entity::remove);
        spawnedMobs.clear();
        this.active = false;

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");
        String raw    = plugin.getConfig().getString("messages.event-end", "&aEvent ended: &f{event}");
        Bukkit.broadcastMessage(MessageUtil.color(prefix + raw.replace("{event}", getDisplayName())));
    }

    @Override
    public void onTick(int secondsRemaining) {
        if (secondsRemaining == 30 || secondsRemaining == 10) {
            world.getPlayers().forEach(p ->
                p.sendActionBar(ChatColor.RED + "Invasion ends in " + secondsRemaining + "s!")
            );
        }
    }
}
