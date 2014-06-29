package com.ikeirnez.uuidcompatibility;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Created by iKeirNez on 29/06/2014.
 */
public class Utils {

    private Utils(){}

    public static boolean containsValue(ConfigurationSection section, Object value){
        for (String key : section.getKeys(false)){
            if (section.get(key).equals(value)){
                return true;
            }
        }

        return false;
    }

}
