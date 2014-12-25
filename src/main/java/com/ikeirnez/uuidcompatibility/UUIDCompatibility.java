package com.ikeirnez.uuidcompatibility;

import javassist.*;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class UUIDCompatibility extends JavaPlugin implements Listener {

    private static UUIDCompatibility instance;

    /**
     * Below is used for reflection
     */
    private static final String CRAFT_SERVER_NAME = Bukkit.getServer().getClass().getName();
    public static final String OBC_PACKAGE = CRAFT_SERVER_NAME.substring(0, CRAFT_SERVER_NAME.length() - ".CraftServer".length());

    public static String MESSAGE_PREFIX = ChatColor.AQUA + "[" + ChatColor.GOLD + "UUIDCompatibility" + ChatColor.AQUA + "] " + ChatColor.GREEN;

    public static UUIDCompatibility getInstance() {
        return instance;
    }

    private Metrics metrics;

    private Set<Plugin> compatibilityPlugins = Collections.newSetFromMap(new ConcurrentHashMap<Plugin, Boolean>()); // clunky, I know, but the only way as far as I'm aware
    public Map<UUID, String> playerRealNames = new ConcurrentHashMap<>();
    private Map<Plugin, List<String>> classNameToPluginMap = new ConcurrentHashMap<>();
    private CustomConfigWrapper nameMappingsWrapper, retrievesWrapper;
    private boolean debug = false;

    {
        instance = this;

        if (!Utils.classExists("com.ikeirnez.uuidcompatibility.hax.UUIDCompatibilityMethodCache")){
            try {
                debug("Injecting code");
                ClassPool classPool = ClassPool.getDefault();

                // create class used for containing a reference to the getName method in ExternalAccess, this saves us getting it every time
                // this is effective as the getName() method is run A LOT, especially with certain plugins
                CtClass ctCacheClass = classPool.makeClass("com.ikeirnez.uuidcompatibility.hax.UUIDCompatibilityMethodCache");
                CtField ctCacheField = new CtField(classPool.get(Method.class.getName()), "GET_NAME_METHOD", ctCacheClass);
                ctCacheField.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
                ctCacheClass.addField(ctCacheField, CtField.Initializer.byExpr("Class.forName(\"" + ExternalAccess.class.getName() + "\", true, " + Bukkit.class.getName() + ".getPluginManager().getPlugin(\"" + getDescription().getName() + "\").getClass().getClassLoader()).getDeclaredMethod(\"getPlayerName\", new Class[]{" + HumanEntity.class.getName() + ".class})"));
                ctCacheClass.toClass(Bukkit.class.getClassLoader(), Bukkit.class.getProtectionDomain());

                // hook into the getName method of CraftHumanEntity
                // in the case of this failing, print the stack trace and fallback to default methods
                CtClass ctCraftHumanEntityClass = classPool.get(OBC_PACKAGE + ".entity.CraftHumanEntity");
                CtMethod ctGetNameMethod = ctCraftHumanEntityClass.getDeclaredMethod("getName");
                ctGetNameMethod.setBody("{ try { return (String) com.ikeirnez.uuidcompatibility.hax.UUIDCompatibilityMethodCache.GET_NAME_METHOD.invoke(null, new Object[]{this}); } catch (" + Throwable.class.getName() + " e) { e.printStackTrace(); return getHandle().getName(); } }");

                Class<?> craftServerClass = Bukkit.getServer().getClass();
                ctCraftHumanEntityClass.toClass(craftServerClass.getClassLoader(), craftServerClass.getProtectionDomain());
            } catch (Throwable throwable){
                getLogger().severe("Error whilst injecting code");
                throwable.printStackTrace();
            }
        } else {
            debug("Skipping injection, already injected");
        }
    }

    @Override
    public void onLoad() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (!getConfig().getBoolean("enabled")){ // warning will be shown in onEnable()
            return;
        }

        debug = getConfig().getBoolean("debug");
        debug("Debugging is now enabled");
        nameMappingsWrapper = new CustomConfigWrapper(new File(getDataFolder(), "nameMappings.yml"));
        retrievesWrapper = new CustomConfigWrapper(new File(getDataFolder(), "retrieves.yml"));

        debug("Calculating plugins to enable UUID compatibility for");
        List<String> pluginList = getConfig().getStringList("showOriginalNameIn.plugins");

        if (pluginList.contains("*")){
            compatibilityPlugins.addAll(Arrays.asList(pluginManager.getPlugins()));

            for (String pluginName : pluginList){
                if (pluginName.startsWith("-")){
                    Plugin plugin = pluginManager.getPlugin(pluginName.substring(1, pluginName.length()));

                    if (plugin != null){
                        compatibilityPlugins.remove(plugin);
                    }
                }
            }
        } else {
            for (String pluginName : pluginList){
                Plugin plugin = pluginManager.getPlugin(pluginName);

                if (plugin != null){
                    if (plugin == this){
                        getLogger().warning("Nice try, but UUIDCompatibility cannot be enabled for the plugin \"UUIDCompatibility\"");
                        continue;
                    }

                    compatibilityPlugins.add(plugin);
                }
            }
        }

        debug("The following plugins will have UUID compatibility enabled: " + Arrays.toString(compatibilityPlugins.toArray()));
        debug("Reading plugin jar files to cache class names...");

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()){
            if (!pluginList.contains(plugin.getName())){
                continue;
            }

            List<String> classNames = new ArrayList<>();
            File pluginJar = Utils.getJarForPlugin(plugin);

            if (pluginList.contains(plugin.getName())){
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
        }
    }

    @Override
    public void onEnable(){
        if (!getConfig().getBoolean("enabled")){
            getLogger().severe("The plugin enabled status has not been set to true in the config, disabling...");
            setEnabled(false);
            return;
        }

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new UUIDCompatibilityListener(this), this);
        pluginManager.registerEvents(this, this);

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
                File userDataFolder = new File(essentials.getDataFolder(), "userdata/");

                if (userDataFolder.exists() && userDataFolder.isDirectory()){
                    File[] files = userDataFolder.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".yml");
                        }
                    });

                    if (files != null){
                        for (File file : files){
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
                    } else {
                        getLogger().severe("Something prevented");
                    }
                } else {
                    getLogger().severe("Unable to import from Essentials due to the userdata file not existing or is not a directory");
                }
            }
        }

        try {
            metrics = new Metrics(this);

            Metrics.Graph storedGraph = metrics.createGraph("Player UUIDs <-> Names Stored");
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

    public void debug(String message){
        if (debug){
            getLogger().info(message);
        }
    }

    public String getOriginalName(Player player){
        FileConfiguration nameMappings = getNameMappingsWrapper().getConfig();
        UUID uuid = player.getUniqueId();
        String uuidString = uuid.toString();

        if (!nameMappings.contains(uuidString)){
            String realName = getRealName(player);
            String newRealName = realName;
            int numberSuffix = 1;

            while (nameMappings.getValues(false).containsValue(newRealName)){
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

        // I couldn't use metadata here instead as it makes a call to getName which results in an infinite continuous loop
        if (!playerRealNames.containsKey(uuid)){
            try {
                Object gameProfile = player.getClass().getDeclaredMethod("getProfile").invoke(player);
                String realName = (String) gameProfile.getClass().getMethod("getName").invoke(gameProfile);
                playerRealNames.put(uuid, realName);
                return realName;
            } catch (Throwable e){
                getLogger().severe("Error retrieving real name for " + uuid);
                e.printStackTrace();
                return null;
            }
        } else {
            return playerRealNames.get(uuid);
        }
    }

    public Plugin isCompatibilityEnabledForClass(Class<?> clazz){
        return isCompatibilityEnabledForClass(clazz.getName());
    }

    public Plugin isCompatibilityEnabledForClass(String className){
        for (Plugin plugin : classNameToPluginMap.keySet()){
            List<String> classNames = classNameToPluginMap.get(plugin);

            if (classNames.contains(className)){
                return plugin;
            }
        }

        return null;
    }

    public Set<Plugin> getCompatibilityPlugins() {
        return compatibilityPlugins;
    }

    public CustomConfigWrapper getNameMappingsWrapper() {
        return nameMappingsWrapper;
    }

    public CustomConfigWrapper getRetrievesWrapper() {
        return retrievesWrapper;
    }

}
