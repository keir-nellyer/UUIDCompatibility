package com.ikeirnez.uuidcompatibility;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.util.HotSwapper;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class UUIDCompatibility extends JavaPlugin implements Listener {

    private static UUIDCompatibility instance;

    public static String MESSAGE_PREFIX = ChatColor.AQUA + "[" + ChatColor.GOLD + "UUIDCompatibility" + ChatColor.AQUA + "] " + ChatColor.GREEN;

    public static UUIDCompatibility getInstance() {
        return instance;
    }

    private Set<Plugin> nonUpdatedPlugins = new HashSet<>();
    public Map<UUID, String> playerRealNames = new HashMap<>();
    private Map<Plugin, List<String>> classNameToPluginMap = new HashMap<>();
    private CustomConfigWrapper nameMappingsWrapper, retrievesWrapper;

    {
        instance = this;
    }

    @Override
    public void onEnable() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (!getConfig().getBoolean("enabled")){
            getLogger().severe("The plugin enabled status has not been set to true in the config, disabling...");
            setEnabled(false);
            return;
        }

        nameMappingsWrapper = new CustomConfigWrapper(new File(getDataFolder(), "nameMappings.yml"));
        retrievesWrapper = new CustomConfigWrapper(new File(getDataFolder(), "retrieves.yml"));
        pluginManager.registerEvents(new UUIDCompatibilityListener(this), this);
        pluginManager.registerEvents(this, this);

        List<String> allowedList = getConfig().getStringList("showOriginalNameIn.plugins");

        if (allowedList.contains("*")){
            nonUpdatedPlugins.addAll(Arrays.asList(pluginManager.getPlugins()));

            for (String pluginName : allowedList){
                if (pluginName.startsWith("-")){
                    Plugin plugin = pluginManager.getPlugin(pluginName.substring(1, pluginName.length()));

                    if (plugin != null){
                        nonUpdatedPlugins.remove(plugin);
                    }
                }
            }
        } else {
            for (String pluginName : allowedList){
                Plugin plugin = pluginManager.getPlugin(pluginName);

                if (plugin != null){
                    nonUpdatedPlugins.add(plugin);
                }
            }
        }

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

        getLogger().info("Reading plugin jar files to retrieve class names...");

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()){
            if (!allowedList.contains(plugin.getName())){
                continue;
            }

            List<String> classNames = new ArrayList<>();
            File pluginJar = Utils.getJarForPlugin(plugin);

            try {
                if (pluginJar.getName().endsWith(".jar")){
                    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(pluginJar));
                    ZipEntry zipEntry = zipInputStream.getNextEntry();

                    while (zipEntry != null){
                        String entryName = zipEntry.getName();

                        if (!zipEntry.isDirectory() && entryName.endsWith(".class")){
                            StringBuilder className = new StringBuilder();

                            for (String part : entryName.split("/")){
                                if (className.length() != 0){
                                    className.append(".");
                                }

                                className.append(part);

                                if (part.endsWith(".class")){
                                    className.setLength(className.length() - ".class".length());
                                }
                            }

                            classNames.add(className.toString());
                        }

                        zipEntry = zipInputStream.getNextEntry();
                    }

                    classNameToPluginMap.put(plugin, classNames);
                }
            } catch (Throwable throwable){
                getLogger().severe("Error caching class names for plugin " + plugin.getName());
                throwable.printStackTrace();
            }
        }

        try {
            getLogger().info("Writing modified version of CraftHumanEntity");
            String craftServerClassName = Bukkit.getServer().getClass().getName();
            final String className = craftServerClassName.substring(0, craftServerClassName.length() - "CraftServer".length()) + "entity.CraftHumanEntity";

            ClassLoader classLoader = getClass().getClassLoader();
            Class.forName(className, true, classLoader); // init class so it can be replaced

            ClassPool classPool = ClassPool.getDefault();

            CtClass ctClass = classPool.get(className);
            CtMethod ctMethod = ctClass.getDeclaredMethod("getName");

            /**
             * The below code creates a method to return a different name depending on a players UUID
             * It has to be this complex as the ExternalAccess class is in a different class-loader from CraftBukkit
             * Class names have full paths so we don't need to import anything
             * If for some reason we are unable to get a name, it defaults to standard behavior
             */
            ctMethod.setBody("{ try { return (String) Class.forName(\"" + ExternalAccess.class.getName() + "\", true, " + Bukkit.class.getName() + ".getPluginManager().getPlugin(\"" + getDescription().getName() + "\").getClass().getClassLoader()).getDeclaredMethod(\"getPlayerName\", new Class[]{" + UUID.class.getName() + ".class}).invoke(null, new Object[]{getUniqueId()}); } catch (" + Throwable.class.getName() + " e) { e.printStackTrace(); return getHandle().getName(); } }");
            // how was that for a one liner

            getLogger().info("Compiling modified CraftHumanEntity to bytecode");
            final byte[] classFile = ctClass.toBytecode();

            // the below code was done in a scheduler so that the HotSwapper would use Bukkit's class-loader as there currently isn't a way to define which class-loader is used
            Bukkit.getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        getLogger().info("HotSwapping in modified version of CraftHumanEntity");
                        HotSwapper hotSwapper = new HotSwapper(8000);
                        hotSwapper.reload(className, classFile);
                    } catch (Throwable throwable){
                        getLogger().severe("Error hot-swapping CraftHumanEntity class");
                        throwable.printStackTrace();
                    }
                }
            });
        } catch (Throwable throwable){
            getLogger().severe("Error applying patch for getName() method");
            throwable.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public String getOriginalName(Player player){
        FileConfiguration nameMappings = getNameMappingsWrapper().getConfig();
        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();

        if (!nameMappings.contains(uuidString)){
            String realName = getRealName(player);
            String newRealName = realName;
            int numberSuffix = 1;

            while (Utils.containsValue(nameMappings, newRealName)){
                newRealName = realName + "_" + numberSuffix++;
            }

            nameMappings.set(uuidString, newRealName);
            getNameMappingsWrapper().saveConfig();
            return newRealName;
        }

        return nameMappings.getString(uuidString);
    }

    public String getRealName(Player player){
        UUID uuid = player.getUniqueId();

        if (!playerRealNames.containsKey(uuid)){
            try {
                Object gameProfile = player.getClass().getDeclaredMethod("getProfile").invoke(player);
                String realName = (String) gameProfile.getClass().getMethod("getName").invoke(gameProfile);
                return playerRealNames.put(uuid, realName);
            } catch (Throwable e){
                getLogger().severe("Error retrieving real name for " + uuid);
                e.printStackTrace();
            }
        } else {
            return playerRealNames.get(uuid);
        }

        return null;
    }

    public Plugin getPluginFromClass(Class<?> clazz){
        return getPluginFromClass(clazz.getName());
    }

    public Plugin getPluginFromClass(String className){
        for (Plugin plugin : classNameToPluginMap.keySet()){
            List<String> classNames = classNameToPluginMap.get(plugin);

            if (classNames.contains(className)){
                return plugin;
            }
        }

        return null;
    }

    public Set<Plugin> getNonUpdatedPlugins() {
        return nonUpdatedPlugins;
    }

    public CustomConfigWrapper getNameMappingsWrapper() {
        return nameMappingsWrapper;
    }

    public CustomConfigWrapper getRetrievesWrapper() {
        return retrievesWrapper;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        playerRealNames.remove(e.getPlayer().getUniqueId());
    }

}
