package dynamic_world_events.dynamic_World_Events.commands;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.events.WorldEvent;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class EventCommand implements CommandExecutor {

    private final Dynamic_World_Events plugin;

    public EventCommand(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        WorldEvent active = plugin.getEventManager().getActiveEvent();

        if (active != null) {
            int secs = plugin.getEventManager().getSecondsRemaining();
            int min  = secs / 60;
            int sec  = secs % 60;
            sender.sendMessage(MessageUtil.color(prefix
                + "&7Aktives Ereignis: &f" + active.getDisplayName()
                + " &7(&e" + min + "m " + sec + "s &7verbleibend)"));
        } else {
            sender.sendMessage(MessageUtil.color(prefix
                + plugin.getConfig().getString("messages.no-active-event", "&7Aktuell kein aktives Ereignis.")));

            int nextSecs = plugin.getEventScheduler().getNextEventInSeconds();
            int min = nextSecs / 60;
            String msg = plugin.getConfig()
                .getString("messages.next-event", "&7Nächstes Ereignis in ca. &e{minutes} Minuten&7.")
                .replace("{minutes}", String.valueOf(min));
            sender.sendMessage(MessageUtil.color(prefix + msg));
        }
        return true;
    }
}
