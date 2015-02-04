package com.ikeirnez.uuidcompatibility;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class UUIDCompatibilityListener implements Listener {

    private UUIDCompatibility instance;

    public UUIDCompatibilityListener(UUIDCompatibility instance){
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e){
        Player player = e.getPlayer();
        instance.refreshDisplayNames(player, true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        String pName = instance.getRealName(player);
        String originalName = instance.getOriginalName(player);

        if (instance.getConfig().getBoolean("notifyPlayers") && !pName.equals(originalName)){
            player.sendMessage(UUIDCompatibility.MESSAGE_PREFIX + "Your name is " + ChatColor.GOLD + pName + ChatColor.GREEN + " however some parts of this server may refer to you as " + ChatColor.GOLD + originalName);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        instance.playerRealNames.remove(e.getPlayer().getUniqueId());
    }

}
