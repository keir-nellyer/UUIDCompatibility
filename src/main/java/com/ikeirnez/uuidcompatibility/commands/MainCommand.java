package com.ikeirnez.uuidcompatibility.commands;

import com.ikeirnez.uuidcompatibility.UUIDCompatibility;
import com.ikeirnez.uuidcompatibility.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Keir on 04/02/2015.
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private static final Permission RELOAD_PERMISSION = new Permission("uuicompatibility.reload", PermissionDefault.OP);

    private UUIDCompatibility instance;

    public MainCommand(UUIDCompatibility instance){
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0){
            PluginDescriptionFile pluginDescriptionFile = instance.getDescription();
            sender.sendMessage(ChatColor.GOLD + pluginDescriptionFile.getName() + ChatColor.AQUA + " by " + pluginDescriptionFile.getAuthors().get(0));
            sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.GOLD + pluginDescriptionFile.getVersion());
        } else {
            switch (args[0].toLowerCase()){
                default: return false;
                case "reload":
                    if (!Utils.hasPermission(sender, RELOAD_PERMISSION)){
                        return true;
                    }

                    instance.getNameMappingsWrapper().reloadConfig();
                    instance.getRetrievesWrapper().reloadConfig();
                    instance.reloadConfig();

                    instance.loadCompatibilityPlugin();
                    instance.importData();

                    for (Player player : Bukkit.getOnlinePlayers()){
                        instance.refreshDisplayNames(player, false);
                    }

                    break;
            }
        }

        return true;
    }

    private static final List<String> ARG_1_COMPLETIONS = Arrays.asList("reload");

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 0 || args.length == 1){
            return ARG_1_COMPLETIONS;
        }

        return null;
    }
}
