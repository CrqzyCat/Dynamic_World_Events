package dynamic_world_events.dynamic_World_Events.commands;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DweCommand implements CommandExecutor, TabCompleter {

    private final Dynamic_World_Events plugin;

    public DweCommand(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        if (args.length == 0) { sendHelp(sender, prefix); return true; }

        switch (args[0].toLowerCase()) {

            // ── Player commands ──────────────────────────────────────────────
            case "bossbar" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cThis subcommand is player-only."));
                    return true;
                }
                plugin.getBossBarManager().toggle(player);
            }

            case "events" -> {
                WorldEvent active = plugin.getEventManager().getActiveEvent();
                if (active != null) {
                    int secs = plugin.getEventManager().getSecondsRemaining();
                    sender.sendMessage(MessageUtil.color(prefix
                        + "&7Active event: &f" + active.getDisplayName()
                        + " &7(&e" + secs / 60 + "m " + secs % 60 + "s &7remaining)"));
                } else {
                    sender.sendMessage(MessageUtil.color(prefix
                        + plugin.getConfig().getString("messages.no-active-event", "&7No active event right now.")));
                    int min = plugin.getEventScheduler().getNextEventInSeconds() / 60;
                    String msg = plugin.getConfig()
                        .getString("messages.next-event", "&7Next event in approx. &e{minutes} min&7.")
                        .replace("{minutes}", String.valueOf(min));
                    sender.sendMessage(MessageUtil.color(prefix + msg));
                }
            }

            // ── Admin commands ───────────────────────────────────────────────
            case "start" -> {
                if (!sender.hasPermission("dwe.admin.start")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                if (plugin.getEventManager().hasActiveEvent()) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cAn event is already running!")); return true;
                }
                if (args.length < 2) {
                    boolean ok = plugin.getEventManager().startRandomEvent();
                    sender.sendMessage(MessageUtil.color(prefix + (ok ? "&aRandom event started!" : "&cNo events available.")));
                } else {
                    boolean ok = plugin.getEventManager().startEventById(args[1]);
                    sender.sendMessage(MessageUtil.color(prefix + (ok
                        ? "&aEvent &f" + args[1] + " &astarted!"
                        : "&cEvent &f" + args[1] + " &cnot found or already active.")));
                }
            }

            case "stop" -> {
                if (!sender.hasPermission("dwe.admin.stop")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                if (!plugin.getEventManager().hasActiveEvent()) {
                    sender.sendMessage(MessageUtil.color(prefix + "&7No active event.")); return true;
                }
                plugin.getEventManager().stopCurrentEvent(true);
                sender.sendMessage(MessageUtil.color(prefix + "&aEvent stopped."));
            }

            case "reload" -> {
                if (!sender.hasPermission("dwe.admin.reload")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                plugin.reload();
                sender.sendMessage(MessageUtil.color(prefix + "&aConfig reloaded!"));
            }

            case "disable" -> {
                if (!sender.hasPermission("dwe.admin.manage")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cUsage: /dwe disable <id>")); return true;
                }
                String id = args[1].toLowerCase();
                boolean exists = plugin.getEventManager().getRegisteredEvents().stream()
                    .anyMatch(e -> e.getId().equalsIgnoreCase(id));
                if (!exists) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cUnknown event: &f" + id)); return true;
                }
                boolean changed = plugin.getDisabledEventsManager().disable(id);
                if (changed) {
                    sender.sendMessage(MessageUtil.color(prefix + "&eEvent &f" + id + " &edisabled. It will not trigger automatically."));
                } else {
                    sender.sendMessage(MessageUtil.color(prefix + "&7Event &f" + id + " &7is already disabled."));
                }
            }

            case "enable" -> {
                if (!sender.hasPermission("dwe.admin.manage")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cUsage: /dwe enable <id>")); return true;
                }
                String id = args[1].toLowerCase();
                boolean changed = plugin.getDisabledEventsManager().enable(id);
                if (changed) {
                    sender.sendMessage(MessageUtil.color(prefix + "&aEvent &f" + id + " &aenabled."));
                } else {
                    sender.sendMessage(MessageUtil.color(prefix + "&7Event &f" + id + " &7was not disabled."));
                }
            }

            case "list" -> {
                if (!sender.hasPermission("dwe.admin.manage")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                Set<String> disabled = plugin.getDisabledEventsManager().getDisabledIds();
                sender.sendMessage(MessageUtil.color(prefix + "&7All events:"));
                for (WorldEvent e : plugin.getEventManager().getRegisteredEvents()) {
                    boolean isDisabled = disabled.contains(e.getId());
                    String status = isDisabled ? "&c✗ disabled" : "&a✓ enabled";
                    sender.sendMessage(MessageUtil.color("  &f" + e.getId() + " &8— " + status));
                }
            }

            default -> sendHelp(sender, prefix);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage(MessageUtil.color(prefix + "&7Available commands:"));
        sender.sendMessage(MessageUtil.color("&7 /dwe events &8— &fShow active event info"));
        sender.sendMessage(MessageUtil.color("&7 /dwe bossbar &8— &fToggle boss bar (player only)"));
        if (sender.hasPermission("dwe.admin.start"))
            sender.sendMessage(MessageUtil.color("&7 /dwe start [id] &8— &fStart an event"));
        if (sender.hasPermission("dwe.admin.stop"))
            sender.sendMessage(MessageUtil.color("&7 /dwe stop &8— &fStop current event"));
        if (sender.hasPermission("dwe.admin.reload"))
            sender.sendMessage(MessageUtil.color("&7 /dwe reload &8— &fReload config"));
        if (sender.hasPermission("dwe.admin.manage")) {
            sender.sendMessage(MessageUtil.color("&7 /dwe disable <id> &8— &fDisable an event from the pool"));
            sender.sendMessage(MessageUtil.color("&7 /dwe enable <id> &8— &fRe-enable a disabled event"));
            sender.sendMessage(MessageUtil.color("&7 /dwe list &8— &fList all events and their status"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("events", "bossbar"));
            if (sender.hasPermission("dwe.admin.start"))  subs.add("start");
            if (sender.hasPermission("dwe.admin.stop"))   subs.add("stop");
            if (sender.hasPermission("dwe.admin.reload")) subs.add("reload");
            if (sender.hasPermission("dwe.admin.manage")) {
                subs.add("disable");
                subs.add("enable");
                subs.add("list");
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission("dwe.admin.start") && args[0].equalsIgnoreCase("start")) {
            return plugin.getEventManager().getRegisteredEvents().stream()
                .map(WorldEvent::getId)
                .filter(id -> id.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission("dwe.admin.manage")) {
            if (args[0].equalsIgnoreCase("disable")) {
                // Only suggest currently enabled events
                return plugin.getEventManager().getRegisteredEvents().stream()
                    .filter(e -> !plugin.getDisabledEventsManager().isDisabled(e.getId()))
                    .map(WorldEvent::getId)
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("enable")) {
                // Only suggest currently disabled events
                return plugin.getDisabledEventsManager().getDisabledIds().stream()
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
