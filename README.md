#UUIDCompatibility

##WARNING

This plugin uses dangerous code which could potentially have negative side effects, remember to keep regular backups of the server in the case of this ever happening.

##Requirements

* **Java 7** or higher.
* The server must be started using the following arguments

    ```
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000
    ```
  
    For Example, old startup script:
  
    ```
    java -jar spigot.jar
    ```
    
    New startup script:
  
    ```
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar spigot.jar
    ```


* Due to the way the Reflection code is written, it should work on most recent builds of CraftBukkit.

##How is this any different from similar plugins?

Unlike similar plugins, UUIDCompatibility **DOES NOT** prevent players who have changed their name from joining the server.

##Features

* Only plugins you specify will be fooled into thinking a players name is their original name
* Importing UUIDs/usernames from player dat files
* Importing UUIDs/usernames from Essentials

##How does it work?

The plugin starts up and injects some code into a CraftBukkit class, this method allows us to see which plugin is attempting to get a players name, if you specified the plugin requesting the players name in the config, we will give the plugin the players original name, in all other cases plugins are given the players real name.

##Example

Player #1 joins the server with the name "Notch", they then leave the server and change their name to "jeb_", when they rejoin the server, UUIDCompatibility tricks plugins you specify that "jeb_" is still called "Notch".

##Why should I pay for this?

There are no other plugins like this, this buys your server more time to update your plugins/wait for developers to update them. Without this, if a user changes their name, they lose all data saved by plugins not updated. Updated plugins will continue to function as intended. I spent many hours testing, updating and adding features to this plugin to get it near perfect.