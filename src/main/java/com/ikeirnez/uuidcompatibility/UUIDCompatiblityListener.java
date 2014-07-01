package com.ikeirnez.uuidcompatibility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class UUIDCompatiblityListener implements Listener {

    private UUIDCompatibility instance;

    public UUIDCompatiblityListener(UUIDCompatibility instance){
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e){
        FileConfiguration configuration = instance.getConfig();
        CustomConfigWrapper nameMappingsWrapper = instance.getNameMappingsWrapper();

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();

        if (!nameMappingsWrapper.getConfig().contains(uuidString)){
            nameMappingsWrapper.getConfig().set(uuidString, instance.getRealName(player));
            nameMappingsWrapper.saveConfig();
        }

        String pName = player.getName();

        if (configuration.getBoolean("showOriginalNameIn.DisplayName")){
            player.setDisplayName(pName);
        }

        if (configuration.getBoolean("showOriginalNameIn.TabList")){
            player.setPlayerListName(pName);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        UUIDCompatibility.getInstance().playerRealNames.remove(e.getPlayer().getUniqueId());
    }

}
