package dynamic_world_events.dynamic_World_Events.gui;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI for managing Dynamic World Events.
 * Opens via /dwe gui — commands remain fully available alongside this.
 *
 * Layout (54 slots):
 *   Row 1-3: Event buttons (one per registered event)
 *   Row 5:   Info panel (active event, next event, season)
 *   Row 6:   Action buttons (Stop current, Start random, Close)
 */
public class EventManagerGui implements Listener {

    private final Dynamic_World_Events plugin;
    private final Player player;
    private final Inventory inv;

    private static final String TITLE = ChatColor.DARK_GRAY + "DWE " + ChatColor.GOLD + "Event Manager";

    public EventManagerGui(Dynamic_World_Events plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inv    = Bukkit.createInventory(null, 54, TITLE);

        build();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inv);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        inv.clear();
        fillBorder();
        populateEvents();
        populateInfoPanel();
        populateActionRow();
    }

    private void fillBorder() {
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 36; i < 54; i++) inv.setItem(i, pane); // rows 5-6 as base
    }

    private void populateEvents() {
        List<WorldEvent> events = plugin.getEventManager().getRegisteredEvents();
        WorldEvent active = plugin.getEventManager().getActiveEvent();

        for (int i = 0; i < Math.min(events.size(), 27); i++) {
            WorldEvent event = events.get(i);
            boolean isActive   = event.equals(active);
            boolean isDisabled = plugin.getDisabledEventsManager().isDisabled(event.getId());

            Material mat;
            String statusLine;
            if (isActive) {
                mat = Material.LIME_CONCRETE;
                int secs = plugin.getEventManager().getSecondsRemaining();
                statusLine = ChatColor.GREEN + "▶ ACTIVE — " + secs / 60 + "m " + secs % 60 + "s left";
            } else if (isDisabled) {
                mat = Material.RED_CONCRETE;
                statusLine = ChatColor.RED + "✗ Disabled";
            } else {
                mat = Material.YELLOW_CONCRETE;
                statusLine = ChatColor.YELLOW + "✓ Enabled";
            }

            int weight = plugin.getSeasonalManager().getEffectiveWeight(event);
            String seasonNote = weight != event.getWeight()
                ? ChatColor.LIGHT_PURPLE + "Seasonal weight: " + weight
                : ChatColor.GRAY + "Weight: " + weight;

            List<String> lore = new ArrayList<>();
            lore.add(statusLine);
            lore.add(ChatColor.GRAY + "Duration: " + event.getDurationSeconds() / 60 + "m " + event.getDurationSeconds() % 60 + "s");
            lore.add(seasonNote);
            lore.add("");
            if (!isActive && !isDisabled) {
                lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to start");
                lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to disable");
            } else if (isActive) {
                lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to stop");
            } else {
                lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to re-enable");
            }

            inv.setItem(i, makeItem(mat, ChatColor.GOLD + "" + ChatColor.BOLD + event.getDisplayName(), lore));
        }
    }

    private void populateInfoPanel() {
        // Slot 37: Active event info
        WorldEvent active = plugin.getEventManager().getActiveEvent();
        String activeLine = active != null
            ? ChatColor.GREEN + active.getDisplayName()
            : ChatColor.GRAY + "None";
        inv.setItem(37, makeItem(Material.CLOCK,
            ChatColor.YELLOW + "Active Event",
            List.of(activeLine)
        ));

        // Slot 39: Next event countdown
        int nextSecs = plugin.getEventScheduler().getNextEventInSeconds();
        inv.setItem(39, makeItem(Material.COMPASS,
            ChatColor.YELLOW + "Next Event",
            List.of(ChatColor.WHITE + "In approx. " + nextSecs / 60 + "m " + nextSecs % 60 + "s")
        ));

        // Slot 41: Active season
        String season = plugin.getSeasonalManager().getActiveSeason();
        List<String> seasonLore = new ArrayList<>();
        if (season != null) {
            seasonLore.add(ChatColor.LIGHT_PURPLE + season);
            plugin.getSeasonalManager().getActiveModifiers().forEach((id, mult) -> {
                String arrow = mult >= 1.0 ? ChatColor.GREEN + "↑" : ChatColor.RED + "↓";
                seasonLore.add(arrow + ChatColor.GRAY + " " + id + " ×" + String.format("%.1f", mult));
            });
        } else {
            seasonLore.add(ChatColor.GRAY + "No active season");
        }
        inv.setItem(41, makeItem(Material.FLOWERING_AZALEA,
            ChatColor.YELLOW + "Season", seasonLore
        ));

        // Slot 43: Pending chains
        Map<String, Integer> chains = plugin.getEventChainManager().getPendingChains();
        List<String> chainLore = new ArrayList<>();
        if (chains.isEmpty()) {
            chainLore.add(ChatColor.GRAY + "No pending chains");
        } else {
            chains.keySet().forEach(id -> chainLore.add(ChatColor.AQUA + "⏳ " + id));
        }
        inv.setItem(43, makeItem(Material.TRIPWIRE_HOOK,
            ChatColor.YELLOW + "Pending Chains", chainLore
        ));
    }

    private void populateActionRow() {
        // Slot 46: Stop current event
        inv.setItem(46, makeItem(Material.BARRIER,
            ChatColor.RED + "" + ChatColor.BOLD + "Stop Current Event",
            List.of(ChatColor.GRAY + "Forcefully stops the active event.")
        ));

        // Slot 49: Start random event
        inv.setItem(49, makeItem(Material.NETHER_STAR,
            ChatColor.GREEN + "" + ChatColor.BOLD + "Start Random Event",
            List.of(ChatColor.GRAY + "Starts a weighted random event.")
        ));

        // Slot 52: Close
        inv.setItem(52, makeItem(Material.OAK_DOOR,
            ChatColor.WHITE + "Close",
            List.of(ChatColor.GRAY + "Close this menu.")
        ));
    }

    // ── Click handler ─────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        if (!event.getWhoClicked().equals(player)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        List<WorldEvent> events = plugin.getEventManager().getRegisteredEvents();

        // Event buttons (slots 0-26)
        if (slot < 27 && slot < events.size()) {
            WorldEvent clicked = events.get(slot);
            WorldEvent active  = plugin.getEventManager().getActiveEvent();
            boolean isDisabled = plugin.getDisabledEventsManager().isDisabled(clicked.getId());

            if (clicked.equals(active)) {
                // Left-click on active → stop
                plugin.getEventManager().stopCurrentEvent(true);
                player.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                    + "&aEvent stopped via GUI."));
            } else if (isDisabled) {
                // Left-click on disabled → re-enable
                plugin.getDisabledEventsManager().enable(clicked.getId());
                player.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                    + "&aEvent &f" + clicked.getId() + " &aenabled."));
            } else if (event.isRightClick()) {
                // Right-click on enabled → disable
                plugin.getDisabledEventsManager().disable(clicked.getId());
                player.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                    + "&eEvent &f" + clicked.getId() + " &edisabled."));
            } else {
                // Left-click on enabled → start
                if (plugin.getEventManager().hasActiveEvent()) {
                    player.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                        + "&cAn event is already running!"));
                } else {
                    plugin.getEventManager().startEventById(clicked.getId());
                }
            }
            refresh();
            return;
        }

        // Action buttons
        switch (slot) {
            case 46 -> { // Stop
                if (plugin.getEventManager().hasActiveEvent()) {
                    plugin.getEventManager().stopCurrentEvent(true);
                    player.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                        + "&aEvent stopped."));
                }
            }
            case 49 -> { // Random
                if (plugin.getEventManager().hasActiveEvent()) {
                    player.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r")
                        + "&cAn event is already running!"));
                } else {
                    plugin.getEventManager().startRandomEvent();
                }
            }
            case 52 -> player.closeInventory(); // Close
        }
        refresh();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        if (!event.getPlayer().equals(player)) return;
        HandlerList.unregisterAll(this);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refresh() {
        Bukkit.getScheduler().runTaskLater(plugin, this::build, 1L);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
