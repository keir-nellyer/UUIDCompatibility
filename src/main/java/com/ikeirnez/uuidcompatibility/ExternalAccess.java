package com.ikeirnez.uuidcompatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Created by iKeirNez on 30/06/2014.
 */
public class ExternalAccess {

    private static UUIDCompat instance;

    static {
        instance = UUIDCompat.getInstance();
    }

    /**
     * This method is used when getName() in the CraftHumanEntity class is replaced
     */
    public static String getPlayerName(UUID uuid){
        Player player = Bukkit.getPlayer(uuid);

        if (player == null){
            throw new RuntimeException("UUID " + uuid + " is either not online or is not a player");
        }

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        for (int i = stackTraceElements.length - 1; i >= 0; i--){
            StackTraceElement stackTraceElement = stackTraceElements[i];
            Plugin owningPlugin = instance.getPluginFromClass(stackTraceElement.getClassName());

            if (owningPlugin != null){
                if (instance.getNonUpdatedPlugins().contains(owningPlugin)){
                    return instance.getOriginalName(player);
                }

                break;
            }
        }

        return instance.getRealName(player);
    }

}
