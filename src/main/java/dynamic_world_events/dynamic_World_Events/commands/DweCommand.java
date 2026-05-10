package dynamic_world_events.dynamic_World_Events.commands;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.gui.EventManagerGui;
import dynamic_world_events.dynamic_World_Events.managers.StatisticsManager;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
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

            case "stats" -> {
                // /dwe stats [player]
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(MessageUtil.color(prefix + "&cPlayer &f" + args[1] + " &cnot found or offline."));
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(MessageUtil.color(prefix + "&cUsage: /dwe stats <player>"));
                    return true;
                }

                StatisticsManager sm = plugin.getStatisticsManager();
                UUID uuid = target.getUniqueId();

                sender.sendMessage(MessageUtil.color(prefix + "&7Stats for &f" + target.getName() + "&7:"));
                sender.sendMessage(MessageUtil.color("  &7Events participated: &e" + sm.getEventsParticipated(uuid)));
                sender.sendMessage(MessageUtil.color("  &7Invasion kills: &e" + sm.getInvasionKills(uuid)));
                sender.sendMessage(MessageUtil.color("  &7Treasures found: &e" + sm.getTreasuresFound(uuid)));
                sender.sendMessage(MessageUtil.color("  &7Blood Moons survived: &e" + sm.getBloodMoonsSurvived(uuid)));
            }

            case "top" -> {
                List<Map.Entry<String, Integer>> board = plugin.getStatisticsManager().getLeaderboard(10);
                sender.sendMessage(MessageUtil.color(prefix + "&6Top Players — Events Participated:"));
                if (board.isEmpty()) {
                    sender.sendMessage(MessageUtil.color("  &7No data yet."));
                } else {
                    for (int i = 0; i < board.size(); i++) {
                        String medal = switch (i) {
                            case 0 -> "&6#1";
                            case 1 -> "&7#2";
                            case 2 -> "&c#3";
                            default -> "&8#" + (i + 1);
                        };
                        sender.sendMessage(MessageUtil.color(
                            "  " + medal + " &f" + board.get(i).getKey() + " &7— &e" + board.get(i).getValue() + " events"
                        ));
                    }
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
                sender.sendMessage(MessageUtil.color(prefix + (changed
                    ? "&eEvent &f" + id + " &edisabled."
                    : "&7Event &f" + id + " &7is already disabled.")));
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
                sender.sendMessage(MessageUtil.color(prefix + (changed
                    ? "&aEvent &f" + id + " &aenabled."
                    : "&7Event &f" + id + " &7was not disabled.")));
            }

            case "list" -> {
                if (!sender.hasPermission("dwe.admin.manage")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission.")); return true;
                }
                Set<String> disabled = plugin.getDisabledEventsManager().getDisabledIds();
                sender.sendMessage(MessageUtil.color(prefix + "&7All events:"));
                for (WorldEvent e : plugin.getEventManager().getRegisteredEvents()) {
                    String status = disabled.contains(e.getId()) ? "&c✗ disabled" : "&a✓ enabled";
                    sender.sendMessage(MessageUtil.color("  &f" + e.getId() + " &8— " + status));
                }
            }

            case "schedule" -> {
                if (!sender.hasPermission("dwe.admin.manage")) {
                    sender.sendMessage(MessageUtil.color(prefix + "0026cNo permission.")); return true;
                }
                if (!plugin.getEventScheduleManager().isEnabled()) {
                    sender.sendMessage(MessageUtil.color(prefix + "00267Schedule is disabled. Enable it in config.yml under schedule.enabled."));
                    return true;
                }
                sender.sendMessage(MessageUtil.color(prefix + "00267Fixed event schedule:"));
                plugin.getEventScheduleManager().getScheduleSummary().forEach(line ->
                    sender.sendMessage(MessageUtil.color("  0026e" + line))
                );
            }

            case "vote" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.color(prefix + "0026cThis subcommand is player-only."));
                    return true;
                }
                if (!plugin.getVotingManager().isVoteActive()) {
                    player.sendMessage(MessageUtil.color(prefix + "00267No vote is currently active."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(MessageUtil.color(prefix + "00267Voting options:"));
                    List<dynamic_world_events.dynamic_World_Events.events.WorldEvent> opts = plugin.getVotingManager().getOptions();
                    for (int i = 0; i < opts.size(); i++) {
                        player.sendMessage(MessageUtil.color("  00268[0026e" + (i + 1) + "00268] 0026f" + opts.get(i).getDisplayName()));
                    }
                    player.sendMessage(MessageUtil.color(prefix + "00267Use 0026f/dwe vote <number>00267 to vote."));
                    return true;
                }
                try {
                    int choice = Integer.parseInt(args[1]);
                    player.sendMessage(MessageUtil.color(plugin.getVotingManager().castVote(player, choice)));
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageUtil.color(prefix + "0026cPlease enter a number."));
                }
            }

            case "history" -> {
                List<String> hist = plugin.getHistoryManager().getLast(10);
                if (hist.isEmpty()) {
                    sender.sendMessage(MessageUtil.color(prefix + "00267No events recorded yet."));
                } else {
                    sender.sendMessage(MessageUtil.color(prefix + "00267Last " + hist.size() + " events:"));
                    hist.forEach(line -> sender.sendMessage(MessageUtil.color("  00268" + line)));
                }
            }

            case "season" -> {
                String activeSeason = plugin.getSeasonalManager().getActiveSeason();
                if (activeSeason == null) {
                    sender.sendMessage(MessageUtil.color(prefix + "00267No seasonal modifier active right now."));
                } else {
                    sender.sendMessage(MessageUtil.color(prefix + "0026aActive season: 0026f" + activeSeason));
                    plugin.getSeasonalManager().getActiveModifiers().forEach((id, mult) -> {
                        String arrow = mult >= 1.0 ? "0026a2191" : "0026c2193";
                        sender.sendMessage(MessageUtil.color("  0026f" + id + " 002682014 " + arrow + " 0026f" + String.format("%.1fx", mult)));
                    });
                }
            }

            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(MessageUtil.color(prefix + "0026cThis subcommand is player-only."));
                    return true;
                }
                if (!player.hasPermission("dwe.admin.gui")) {
                    player.sendMessage(MessageUtil.color(prefix + "0026cNo permission."));
                    return true;
                }
                new EventManagerGui(plugin, player);
            }

            case "chains" -> {
                if (!sender.hasPermission("dwe.admin.manage")) {
                    sender.sendMessage(MessageUtil.color(prefix + "0026cNo permission.")); return true;
                }
                var pending = plugin.getEventChainManager().getPendingChains();
                if (pending.isEmpty()) {
                    sender.sendMessage(MessageUtil.color(prefix + "00267No pending event chains."));
                } else {
                    sender.sendMessage(MessageUtil.color(prefix + "00267Pending chains:"));
                    pending.keySet().forEach(id -> sender.sendMessage(MessageUtil.color("  0026e23f3 0026f" + id)));
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
        sender.sendMessage(MessageUtil.color("&7 /dwe stats [player] &8— &fView your or another player's stats"));
        sender.sendMessage(MessageUtil.color("&7 /dwe top &8— &fTop 10 event leaderboard"));
        sender.sendMessage(MessageUtil.color("0026e /dwe schedule 002682014 0026fView the fixed event schedule"));
        if (sender.hasPermission("dwe.admin.gui"))
            sender.sendMessage(MessageUtil.color("00266 /dwe gui 002682014 0026fOpen the event manager GUI"));
        if (sender.hasPermission("dwe.admin.manage"))
            sender.sendMessage(MessageUtil.color("00267 /dwe chains 002682014 0026fView pending event chains"));
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
            List<String> subs = new ArrayList<>(Arrays.asList("events", "bossbar", "stats", "top", "schedule", "season", "vote", "history", "gui", "chains"));
            sender.sendMessage(MessageUtil.color("0026e /dwe schedule 002682014 0026fView the fixed event schedule"));
        if (sender.hasPermission("dwe.admin.gui"))
            sender.sendMessage(MessageUtil.color("00266 /dwe gui 002682014 0026fOpen the event manager GUI"));
        if (sender.hasPermission("dwe.admin.manage"))
            sender.sendMessage(MessageUtil.color("00267 /dwe chains 002682014 0026fView pending event chains"));
        if (sender.hasPermission("dwe.admin.start"))  subs.add("start");
            if (sender.hasPermission("dwe.admin.stop"))   subs.add("stop");
            if (sender.hasPermission("dwe.admin.reload")) subs.add("reload");
            if (sender.hasPermission("dwe.admin.manage")) { subs.add("disable"); subs.add("enable"); subs.add("list"); }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") && sender.hasPermission("dwe.admin.start")) {
                return plugin.getEventManager().getRegisteredEvents().stream()
                    .map(WorldEvent::getId).filter(id -> id.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("disable") && sender.hasPermission("dwe.admin.manage")) {
                return plugin.getEventManager().getRegisteredEvents().stream()
                    .filter(e -> !plugin.getDisabledEventsManager().isDisabled(e.getId()))
                    .map(WorldEvent::getId).filter(id -> id.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("enable") && sender.hasPermission("dwe.admin.manage")) {
                return plugin.getDisabledEventsManager().getDisabledIds().stream()
                    .filter(id -> id.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("stats")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).filter(n -> n.startsWith(args[1])).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
