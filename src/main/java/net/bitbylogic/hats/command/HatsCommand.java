package net.bitbylogic.hats.command;

import lombok.RequiredArgsConstructor;
import net.bitbylogic.hats.Hats;
import net.bitbylogic.hats.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class HatsCommand implements TabExecutor {

    public final Hats plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!sender.hasPermission("hats.reload")) {
            sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.No-Permission")));
            return true;
        }

        if (args.length == 0) {
            plugin.reloadConfig();
            plugin.getHatManager().loadHats();
            sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.Successfully-Reloaded")));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 1) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.Give-Command.Invalid-Player")));
                return true;
            }

            if (args.length == 2) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.Give-Command.Missing-ID")));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.Give-Command.Invalid-Player")));
                return true;
            }

            plugin.getHatManager().getHatByID(args[2]).ifPresentOrElse(hat -> {
                target.getInventory().addItem(hat.getItem()).forEach((integer, extraItem) -> target.getWorld().dropItemNaturally(target.getLocation(), extraItem));
                sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.Give-Command.Success").replace("%player%", target.getName()).replace("%hat%", hat.getId())));
            }, () -> {
                sender.sendMessage(Utils.color(plugin.getConfig().getString("Messages.Give-Command.Invalid-ID")));
            });

            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Collections.singletonList("give"), new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), new ArrayList<>());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return StringUtil.copyPartialMatches(args[2], plugin.getHatManager().getLoadedHats().descendingKeySet(), new ArrayList<>());
        }

        return new ArrayList<>();
    }
}
