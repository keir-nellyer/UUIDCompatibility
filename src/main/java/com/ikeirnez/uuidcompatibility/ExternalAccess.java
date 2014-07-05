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
     * CRITICAL - Player.getName() ABSOLUTELY CANNOT be used inside this method, it will result in a infinite continuous loop
     */
    public static String getPlayerName(UUID uuid){
        instance.debug("Detected getName() usage");
        Player player = Bukkit.getPlayer(uuid);

        if (player == null){
            throw new RuntimeException("UUID " + uuid + " is either not online or is not a player");
        }

        String realName = instance.getRealName(player);
        String originalName = instance.getOriginalName(player);

        instance.debug("Is for player " + instance.getRealName(player) + " (" + originalName + ")");

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        for (int i = stackTraceElements.length - 1; i >= 0; i--){
            StackTraceElement stackTraceElement = stackTraceElements[i];
            Plugin owningPlugin = instance.getPluginFromClass(stackTraceElement.getClassName());

            if (owningPlugin != null){
                instance.debug("Usage is from plugin \"" + owningPlugin + "\"");

                if (instance.getCompatibilityPlugins().contains(owningPlugin)){
                    instance.debug("Returning players original name");
                    return originalName;
                }

                break;
            }
        }

        instance.debug("Returning players real name");
        return realName;
    }

}
