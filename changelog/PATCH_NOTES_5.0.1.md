# Sophisticated Building Update – 5.0.1 - Minecraft 26.2

Bug fixes on top of 5.0.0; no new features, no changes to worlds or configs needed.

## Fixes

* **Fabric: power level and modifier settings are saved.** They were kept in memory only and lost when the server (or
  the singleplayer world) restarted. They are now saved with the player in the world save and kept on respawn.
* **Build limits checked by the server.** The server now checks every build request against the player's power level
  (placement reach, blocks per axis, blocks placed at once, array and mirror sizes) instead of trusting the client; a
  request outside the limits is refused or cut with a message. The common config values that set these limits are sent
  to the clients, so the preview uses the server's values.
* **Fabric: claim and protection mods are respected.** The mod's breaks fire Fabric API's player break events, and
  placements and breaks ask claim mods that provide the Common Protection API; a protected block is neither placed nor
  broken (and not charged).
* **Array limit.** The array modifier no longer places copies beyond the blocks-per-axis limit of the power level.
* **Offhand randomizer bag as replace filter.** With "Replace filtered by offhand", a randomizer bag in the offhand now
  matches only the blocks in the bag (before, any block could match).
* **Material cost list.** The list of missing materials counts every item of a block (a double slab needs two slabs).
* **Activate Previous Build Mode** returns to the mode you actually used before, also when it was chosen in the radial
  menu (before, it only switched to and from Disabled).
* **Power level hint.** The radial menu's power level summary said to loot power items in dungeons; those items do not
  exist. It now says to craft and consume Reach Upgrades 1, 2 and 3 (operators: `/powerlevel`).
* **Translations.** All player-visible text (Reach Upgrade and Randomizer Bag tooltips and messages, the modifier
  screen, the Omega bag screen, build hints, server messages) now comes from the language file, so it can be
  translated. Unused texts of items that do not exist were removed.
