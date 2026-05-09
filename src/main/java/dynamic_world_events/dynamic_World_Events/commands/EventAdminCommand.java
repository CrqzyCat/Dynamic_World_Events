package dynamic_world_events.dynamic_World_Events.commands;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EventAdminCommand implements CommandExecutor, TabCompleter {

    private final Dynamic_World_Events plugin;

    public EventAdminCommand(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        switch (label.toLowerCase()) {

            case "eventstart" -> {
                if (!sender.hasPermission("dwe.admin.start")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission."));
                    return true;
                }
                if (plugin.getEventManager().hasActiveEvent()) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cAn event is already running!"));
                    return true;
                }
                if (args.length == 0) {
                    boolean started = plugin.getEventManager().startRandomEvent();
                    sender.sendMessage(MessageUtil.color(prefix + (started
                        ? "&aRandom event started!"
                        : "&cNo events available.")));
                } else {
                    boolean started = plugin.getEventManager().startEventById(args[0]);
                    sender.sendMessage(MessageUtil.color(prefix + (started
                        ? "&aEvent &f" + args[0] + " &astarted!"
                        : "&cEvent &f" + args[0] + " &cnot found or already active.")));
                }
            }

            case "eventstop" -> {
                if (!sender.hasPermission("dwe.admin.stop")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission."));
                    return true;
                }
                if (!plugin.getEventManager().hasActiveEvent()) {
                    sender.sendMessage(MessageUtil.color(prefix + "&7No active event."));
                    return true;
                }
                plugin.getEventManager().stopCurrentEvent(true);
                sender.sendMessage(MessageUtil.color(prefix + "&aEvent stopped."));
            }

            case "eventreload" -> {
                if (!sender.hasPermission("dwe.admin.reload")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cNo permission."));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(MessageUtil.color(prefix + "&aConfig reloaded!"));
            }

            default -> sender.sendMessage(MessageUtil.color(prefix + "&cUnknown command."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("eventstart") && args.length == 1) {
            return plugin.getEventManager().getRegisteredEvents().stream()
                .map(WorldEvent::getId)
                .filter(id -> id.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
