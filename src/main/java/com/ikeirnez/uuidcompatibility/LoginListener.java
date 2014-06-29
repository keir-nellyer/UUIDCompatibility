package com.ikeirnez.uuidcompatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class LoginListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e){
        final Player player = e.getPlayer();
        final String pName = player.getName();
        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();

        CustomConfigWrapper nameMappingsWrapper = UUIDCompatibility.getInstance().getNameMappingsWrapper();
        FileConfiguration nameMappings = nameMappingsWrapper.getConfig();

        player.setMetadata("RealName", new FixedMetadataValue(UUIDCompatibility.getInstance(), pName)); // allows other plugins to bypass this system and get a players real name

        if (!nameMappings.contains(uuidString)){
            String newName = pName;

            // handles if another player with the same name has joined before to add a prefix to their name
            final AtomicInteger suffixNumber = new AtomicInteger(0); // used atomic integer so this could be set to final and referenced below
            while (Utils.containsValue(nameMappings, newName)){
                newName = pName + "_" + suffixNumber.incrementAndGet();
            }

            nameMappings.set(uuidString, newName);
            nameMappingsWrapper.saveConfig();

            if (suffixNumber.get() == 0){ // no need to apply name hack if name hasn't changed
                return;
            } else {
                Bukkit.getScheduler().runTaskLater(UUIDCompatibility.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        player.sendMessage(UUIDCompatibility.MESSAGE_PREFIX + "Someone else with the same name as you has joined before, therefore we suffixed your name with: " + ChatColor.GOLD + "_" + suffixNumber);
                    }
                }, 20L);
            }
        }

        final String originalName = nameMappings.getString(uuidString);

        if (!pName.equals(originalName)){
            try {
                // reflection crap
                Object gameProfile = player.getClass().getMethod("getProfile").invoke(player);
                Field nameField = gameProfile.getClass().getDeclaredField("name");
                nameField.setAccessible(true);

                // the below code lets us set the "name" field in GameProfile even whilst its final
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(nameField, nameField.getModifiers() &~Modifier.FINAL);
                nameField.set(gameProfile, originalName);

                UUIDCompatibility.getInstance().getLogger().info("Set name for " + pName + " to " + originalName + " for backwards compatibility with non-updated plugins");

                Bukkit.getScheduler().runTaskLater(UUIDCompatibility.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        player.sendMessage(UUIDCompatibility.MESSAGE_PREFIX + "We have detected that your name is " + ChatColor.GOLD + pName + ChatColor.GREEN + " whilst this server recognizes you as " + ChatColor.GOLD + originalName);
                        player.sendMessage(UUIDCompatibility.MESSAGE_PREFIX + "As a result of this, we will now refer to you as: " + ChatColor.GOLD + originalName);
                    }
                }, 20L);

                player.setDisplayName(originalName); // set display name for consistency
            } catch (Throwable throwable){
                UUIDCompatibility.getInstance().getLogger().severe("Something went wrong whilst applying \"hacks\" for player " + pName);
                throwable.printStackTrace();
            }
        }
    }

}
