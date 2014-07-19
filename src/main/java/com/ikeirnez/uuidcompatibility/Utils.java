package com.ikeirnez.uuidcompatibility;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class Utils {

    private Utils(){}

    public static File getJarForPlugin(Plugin plugin){
        if (plugin instanceof JavaPlugin){
            try {
                Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
                getFileMethod.setAccessible(true);
                return (File) getFileMethod.invoke(plugin);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}
