package com.ikeirnez.uuidcompatibility;

import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class UUIDCompatibility extends JavaPlugin {

    private static UUIDCompatibility instance;

    public static String MESSAGE_PREFIX = ChatColor.AQUA + "[" + ChatColor.GOLD + "UUIDCompatibility" + ChatColor.AQUA + "] " + ChatColor.GREEN;

    public static UUIDCompatibility getInstance() {
        return instance;
    }

    private Metrics metrics;
    private CustomConfigWrapper nameMappingsWrapper, retrievesWrapper;

    {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!getConfig().getBoolean("enabled")){
            getLogger().severe("The plugin enabled status has not been set to true in the config, disabling...");
            setEnabled(false);
            return;
        }

        nameMappingsWrapper = new CustomConfigWrapper(new File(getDataFolder(), "nameMappings.yml"));
        retrievesWrapper = new CustomConfigWrapper(new File(getDataFolder(), "retrieves.yml"));
        getServer().getPluginManager().registerEvents(new LoginListener(), this);

        if (!getRetrievesWrapper().getConfig().getBoolean("retrieved.world-data")){
            getLogger().info("Retrieving UUID <-> Names from player dat files, please wait...");

            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()){
                String uuidString = offlinePlayer.getUniqueId().toString();

                if (!getNameMappingsWrapper().getConfig().contains(uuidString)){
                    getNameMappingsWrapper().getConfig().set(uuidString, offlinePlayer.getName());
                }
            }

            getNameMappingsWrapper().saveConfig();
            getRetrievesWrapper().getConfig().set("retrieved.world-data", true);
            getRetrievesWrapper().saveConfig();
        }

        if (!getRetrievesWrapper().getConfig().getBoolean("retrieved.essentials", false)){
            Plugin essentialsPlugin = getServer().getPluginManager().getPlugin("Essentials");

            if (essentialsPlugin != null){
                getLogger().info("Retrieving UUID <-> Names from Essentials data, please wait...");
                IEssentials essentials = (IEssentials) essentialsPlugin;

                for (File file : new File(essentials.getDataFolder(), "userdata/").listFiles()){
                    String fileName = file.getName();

                    if (fileName.endsWith(".yml")){
                        try {
                            UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                            String uuidString = uuid.toString();

                            if (!getNameMappingsWrapper().getConfig().contains(uuidString)){
                                getNameMappingsWrapper().getConfig().set(uuidString, essentials.getUser(uuid).getLastAccountName());
                            }
                        } catch (IllegalArgumentException e){}
                    }
                }

                getNameMappingsWrapper().saveConfig();
                getRetrievesWrapper().getConfig().set("retrieved.essentials", true);
                getRetrievesWrapper().saveConfig();
            }
        }

        try {
            metrics = new Metrics(this);

            Metrics.Graph storedGraph = metrics.createGraph("UUIDs <-> Player Names Stored");
            storedGraph.addPlotter(new Metrics.Plotter() {
                @Override
                public int getValue() {
                    return getNameMappingsWrapper().getConfig().getKeys(false).size();
                }
            });

            metrics.start();
        } catch (IOException e){}
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public CustomConfigWrapper getNameMappingsWrapper() {
        return nameMappingsWrapper;
    }

    public CustomConfigWrapper getRetrievesWrapper() {
        return retrievesWrapper;
    }
}
