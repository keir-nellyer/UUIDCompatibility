package com.ikeirnez.uuidcompatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Created by iKeirNez on 30/06/2014.
 */
public class ExternalAccess {

    private static UUIDCompatibility instance;

    static {
        instance = UUIDCompatibility.getInstance();
    }

    /**
     * This method is used when getName() in the CraftHumanEntity class is replaced
     */
    public static String getPlayerName(UUID uuid){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        for (int i = stackTraceElements.length - 1; i >= 0; i--){
            StackTraceElement stackTraceElement = stackTraceElements[i];
            Plugin owningPlugin = instance.getPluginFromClass(stackTraceElement.getClassName());

            if (owningPlugin != null){
                if (instance.getNonUpdatedPlugins().contains(owningPlugin)){
                    return instance.getOriginalName(uuid);
                }

                break;
            }
        }

        Player player = Bukkit.getPlayer(uuid);

        if (player == null){
            return null;
        }

        return instance.getRealName(player);
    }

}
