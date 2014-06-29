#UUIDCompatibility

##WARNING

This plugin uses dangerous code which could potentially break plugins, cause loss of data and/or damage of data. Most plugins should react fine to this but there are no guarantees. Please remember to keep regular backups in the case of this ever happening.

##How is this any different from similar plugins?

Unlike similar plugins, UUIDCompatibility **DOES NOT** prevent players who have changed their name from joining.

##How does it work?

When a player first joins the server, we store their UUID and name in a data file. When the player next joins, if their name has been changed, we apply some "hacks" which tricks other plugins into believing that the players name hasn't changed.

##Example

Player #1 joins the server with the name "Notch", they then leave the server and change their name to "jeb_", when they rejoin the server, UUIDCompatibility tricks all other plugins that "jeb_" is still called "Notch".

##When should I install this?

This plugin should be installed ASAP so that it can log as many players usernames as possible, I will soon be adding features to retrieve UUIDs from other sources so that players don't have to join to get their current username logged.

##Why do I see players real names in chat, tab list and such like?

This is due to the players **DISPLAY** name being set to their real name on join, this was done to show a players real name in as many places as possible.

##I want my plugin to retrieve the players real name

To bypass this plugins username change, you can use the following code to get a players real name

```java
String realName = player.getMetadata("RealName").get(0).asString();
```

##Licensing

We're GPL v3, for more details, see [here](LICENSE.txt)