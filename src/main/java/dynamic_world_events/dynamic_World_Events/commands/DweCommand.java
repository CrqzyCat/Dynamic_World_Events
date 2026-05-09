package dynamic_world_events.dynamic_World_Events.commands;

import dynamic_world_events.dynamic_World_Events.Dynamic_World_Events;
import dynamic_world_events.dynamic_World_Events.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DweCommand implements CommandExecutor, TabCompleter {

    private final Dynamic_World_Events plugin;

    public DweCommand(Dynamic_World_Events plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DWE&8] &r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.color(prefix + "&cThis command is player-only."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MessageUtil.color(prefix + "&7Usage: &f/dwe bossbar"));
            return true;
        }

        if (args[0].equalsIgnoreCase("bossbar")) {
            plugin.getBossBarManager().toggle(player);
            return true;
        }

        player.sendMessage(MessageUtil.color(prefix + "&cUnknown subcommand. Usage: &f/dwe bossbar"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return Arrays.asList("bossbar");
        return Collections.emptyList();
    }
}
