package dynamic_world_events.dynamic_World_Events.commands;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.command.*;

import java.util.*;
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
                    sender.sendMessage(MessageUtil.color(prefix + "&cKeine Berechtigung."));
                    return true;
                }
                if (plugin.getEventManager().hasActiveEvent()) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cEin Ereignis läuft bereits!"));
                    return true;
                }
                if (args.length == 0) {
                    // Start random event
                    boolean started = plugin.getEventManager().startRandomEvent();
                    sender.sendMessage(MessageUtil.color(prefix + (started ? "&aZufälliges Ereignis gestartet!" : "&cKeine Events verfügbar.")));
                } else {
                    boolean started = plugin.getEventManager().startEventById(args[0]);
                    sender.sendMessage(MessageUtil.color(prefix + (started
                        ? "&aEreignis &f" + args[0] + " &agestartet!"
                        : "&cEreignis &f" + args[0] + " &cnicht gefunden oder bereits aktiv.")));
                }
            }

            case "eventstop" -> {
                if (!sender.hasPermission("dwe.admin.stop")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cKeine Berechtigung."));
                    return true;
                }
                if (!plugin.getEventManager().hasActiveEvent()) {
                    sender.sendMessage(MessageUtil.color(prefix + "&7Kein aktives Ereignis."));
                    return true;
                }
                plugin.getEventManager().stopCurrentEvent(true);
                sender.sendMessage(MessageUtil.color(prefix + "&aEreignis gestoppt."));
            }

            case "eventreload" -> {
                if (!sender.hasPermission("dwe.admin.reload")) {
                    sender.sendMessage(MessageUtil.color(prefix + "&cKeine Berechtigung."));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(MessageUtil.color(prefix + "&aConfig neu geladen!"));
            }

            default -> sender.sendMessage(MessageUtil.color(prefix + "&cUnbekannter Befehl."));
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
