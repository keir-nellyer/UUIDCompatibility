package com.ikeirnez.uuidcompatibility.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class Utils {

    private Utils(){}

    public static File getJarForPlugin(Plugin plugin){
        if (plugin instanceof JavaPlugin){
            try {
                Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
                getFileMethod.setAccessible(true);
                return (File) getFileMethod.invoke(plugin);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static boolean classExists(String className){
        try {
            Class clazz = Class.forName(className, false, Utils.class.getClassLoader()); // the false is important, prevents it getting initialized and throwing errors on reloads
            return clazz != null; // probably isn't needed, could probably get away with "true"
        } catch (ClassNotFoundException e){
            return false;
        }
    }

    public static boolean hasPermission(CommandSender commandSender, Permission permission){
        boolean hasPermission = commandSender.hasPermission(permission);

        if (!hasPermission){
            commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
        }

        return hasPermission;
    }

}
