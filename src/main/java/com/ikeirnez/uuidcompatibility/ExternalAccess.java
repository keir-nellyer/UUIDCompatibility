package com.ikeirnez.uuidcompatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Created by iKeirNez on 30/06/2014.
 */
public class ExternalAccess {

    private static UUIDCompatibility instance;

    static {
        instance = UUIDCompatibility.getInstance();
    }

    /**
     * This method is used when getName() in the CraftHumanEntity class is replaced
     * CRITICAL - Player.getName() ABSOLUTELY CANNOT be used inside this method, it will result in a infinite continuous loop
     */
    public static String getPlayerName(HumanEntity humanEntity){
        instance.debug("-------------------------------------------");
        instance.debug("Detected getName() usage");

        if (!(humanEntity instanceof Player)){
            instance.debug("HumanEntity was not a Player, maybe an NPC?");
            return getOriginalBehavior(humanEntity);
        }

        Player player = (Player) humanEntity;
        String realName = instance.getRealName(player);
        String originalName = instance.getOriginalName(player);

        instance.debug("Is for player " + instance.getRealName(player) + " (" + originalName + ")");

        if (realName.equals(originalName)){
            instance.debug("Real name and original name equal, won't bother matching to plugin");
            return realName;
        }

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement callingElement = null;

        for (int i = 0; i < stackTraceElements.length; i++){
            callingElement = stackTraceElements[i];

            if (callingElement.getClassName().equals(UUIDCompatibility.HUMAN_ENTITY_CLASS)){
                callingElement = stackTraceElements[i++];

                // if getPlayer call, find root caller
                StackTraceElement higherElement = stackTraceElements[i++];
                if (higherElement.getClassName().equals(UUIDCompatibility.CRAFT_SERVER_CLASS_NAME) && (higherElement.getMethodName().equals("getPlayer") || higherElement.getMethodName().equals("getPlayerExact"))){
                    callingElement = stackTraceElements[i++];

                    if (callingElement.getClassName().equals(Bukkit.class.getName()) && (callingElement.getMethodName().equals("getPlayer") || callingElement.getMethodName().equals("getPlayerExact"))){
                        callingElement = stackTraceElements[i++];
                    }
                }

                break;
            }
        }

        if (callingElement != null){ // should never be null, but just in case
            String className = callingElement.getClassName();
            Plugin plugin = instance.isCompatibilityEnabledForClass(className);

            if (plugin != null){
                instance.debug("Compatibility is enabled for class " + className + " (" + plugin.getName() + ")");
                instance.debug("Returning players original name");
                return originalName;
            }
        } else {
            Logger logger = instance.getLogger();
            logger.severe("Couldn't find calling class, stacktrace dump:");

            for (StackTraceElement stackTraceElement : stackTraceElements){
                logger.severe(stackTraceElement.toString());
            }

            logger.severe("Will return real name");
            logger.severe("----------------------------");
            return realName;
        }

        instance.debug("Call was made internally/from plugin with UUID compatibility disabled");
        instance.debug("Returning players real name");
        return realName;
    }

    /**
     * Returns what getName() would usually return, used as a fallback
     */
    public static String getOriginalBehavior(HumanEntity humanEntity){
        try {
            Class<?> clazz = Class.forName(UUIDCompatibility.OBC_PACKAGE + ".entity.CraftHumanEntity");
            Method getHandleMethod = clazz.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);

            Object entityHuman = getHandleMethod.invoke(humanEntity);
            Method getNameMethod = entityHuman.getClass().getDeclaredMethod("getName");
            getNameMethod.setAccessible(true);

            return (String) getNameMethod.invoke(entityHuman);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

}
