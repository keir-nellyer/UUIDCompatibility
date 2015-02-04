package com.ikeirnez.uuidcompatibility.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class written by Keir Nellyer (GitHub @iKeirNez)
 */
public class CustomConfigWrapper {

    private FileConfiguration config;
    private final File configFile;

    public CustomConfigWrapper(File configFile){
        this.configFile = configFile;

        try {
            configFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defConfigStream = getClass().getResourceAsStream("/" + configFile.getName());
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            this.reloadConfig();
        }

        return config;
    }

    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        try {
            getConfig().save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
