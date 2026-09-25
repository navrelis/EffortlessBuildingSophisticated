# Sophisticated Building Update – 5.0.1 - Minecraft 1.21.1

A bug-fix release for 1.21.1 and 1.21: nothing to migrate, worlds and configs of 5.0.0 load unchanged.

## Artifacts

* `sophisticatedbuilding-fabric-1.21.1-5.0.1.jar` — Fabric, Minecraft 1.21 and 1.21.1
* `sophisticatedbuilding-neoforge-1.21.1-5.0.1.jar` — NeoForge, Minecraft 1.21 and 1.21.1
* `sophisticatedbuilding-forge-1.21.1-5.0.1.jar` — Forge, Minecraft 1.21.1
* `sophisticatedbuilding-forge-1.21-5.0.1.jar` — Forge, Minecraft 1.21 (Forge 51 cannot load the 1.21.1 Forge jar)

## Fixes

* **Server checks of build requests:** the server now checks what a client asks it to build against the player's
  power level, as the client already did: the start must be within reach, the build must fit the blocks per axis,
  every block must be within the build mode's reach plus what the player's modifiers can add, and more blocks than
  may be placed at once are cut to that limit. A modified client could build anywhere and any amount before.
* **Common config synced:** the power level limits of the server's common config (reach, blocks at once, blocks per
  axis, mirror radius) are sent to the client when it joins, so the previews and the modifier screen use the
  server's limits instead of the client's own config file.
* **Array limit:** the Array modifier builds at most as many copies as the blocks-per-axis limit allows (before, too
  many copies were only shown in red in the modifier screen); the server caps stored array counts and mirror radii
  to the limits too.
* **Offhand bag filter:** "filtered by offhand" with a Randomizer Bag in the offhand accepted every block (and air)
  as soon as the bag held any block; it now accepts only the bag's blocks.
* **Material cost list:** the HUD list counted a double slab, three candles or four sea pickles as one item; it now
  counts every item of a block, as the server charges, and so does the red missing-item marking.
* **"Activate Previous Build Mode"** now returns to the mode actually used before (also modes picked in the radial
  menu), and "Toggle Disabled / Previous Build Mode" returns to the mode that was active before Disable.
* **Translations:** the remaining English texts (Reach Upgrade and Randomizer Bag tooltips, Omega bag screen,
  modifier screen, build hints, radial menu power level, selection size, server messages) are translation keys now.
  The power level hint no longer tells you to loot power items from dungeons (there are none): it names the Reach
  Upgrades 1, 2 and 3 and `/powerlevel`. Unused texts, models and textures of items that never existed are removed.
* **Fabric: player data kept in the world save:** the power level and the mod's per-player data (modifier settings,
  build state) were kept in memory by UUID: lost when the server restarted and carried over from one singleplayer
  world into the next. They are now saved with the player, like on NeoForge and Forge, and follow the player on
  respawn.
* **Fabric: claim and protection mods:** blocks the mod breaks now fire Fabric API's player block break events
  (claim mods listening to them can refuse a break), and with a mod that provides the Common Protection API installed,
  the mod's breaks and placements ask it first. Refused placements are not charged.

## Requirements

Unchanged from 5.0.0 (see `PATCH_NOTES_5.0.0.md`). Optional on Fabric: a mod that provides the Common Protection API
(1.0.x) is consulted for the mod's breaks and placements when it is installed.
