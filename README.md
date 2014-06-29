#UUIDCompatibility

##WARNING

This plugin uses dangerous code which could potentially break plugins, cause loss of data and/or damage of data. Most plugins should react fine to this but there are no guarantees. Please remember to keep regular backups in the case of this ever happening.

##Requirements

This plugin requires **Java 7**.
Due to the way the Reflection code is written, it should work on most recent builds of CraftBukkit.

##How is this any different from similar plugins?

Unlike similar plugins, UUIDCompatibility **DOES NOT** prevent players who have changed their name from joining the server.

##How does it work?

When a player first joins the server, we store their UUID and name in a data file. When the player next joins, if their name has been changed, we apply some magic (reflection) which tricks other plugins into believing that the players name hasn't changed.

##Example

Player #1 joins the server with the name "Notch", they then leave the server and change their name to "jeb_", when they rejoin the server, UUIDCompatibility tricks all other plugins that "jeb_" is still called "Notch".

##I want my plugin to retrieve the players real name

To bypass this plugins username change, you can use the following code to get a players real name

```java
String realName = player.getMetadata("RealName").get(0).asString();
```

##Licensing

We're GPL v3, for more details, see [here](LICENSE.txt)