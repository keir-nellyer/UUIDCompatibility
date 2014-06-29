package com.ikeirnez.uuidcompatibility;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class UUIDCompatibility extends JavaPlugin {

    private static UUIDCompatibility instance;

    public static String MESSAGE_PREFIX = ChatColor.AQUA + "[" + ChatColor.GOLD + "UUIDCompatibility" + ChatColor.AQUA + "] " + ChatColor.GREEN;

    public static UUIDCompatibility getInstance() {
        return instance;
    }

    private CustomConfigWrapper nameMappingsWrapper;

    {
        instance = this;
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdir();
        nameMappingsWrapper = new CustomConfigWrapper(new File(getDataFolder(), "nameMappings.yml"));
        getServer().getPluginManager().registerEvents(new LoginListener(), this);
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public CustomConfigWrapper getNameMappingsWrapper() {
        return nameMappingsWrapper;
    }
}
