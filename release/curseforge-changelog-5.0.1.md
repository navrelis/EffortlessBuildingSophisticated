# Sophisticated Building 5.0.1

A bug-fix release on top of 5.0.0. Nothing to migrate: worlds and configs load unchanged, and every file name keeps
its loader and Minecraft version, only `5.0.0` becomes `5.0.1`.

## Fixes

- **The server now checks every build request against your power level:** the start must be within reach, the build
  must fit the blocks-per-axis limit, and more blocks than you may place at once are cut to that limit. A modified
  client could build anywhere and any amount before; this also fixes the server's power level limits not reaching
  the client, so previews and the modifier screen now match what the server allows.
- **Array limit:** the Array modifier no longer builds more copies than your blocks-per-axis limit allows (before,
  extra copies were only shown in red in the modifier screen).
- **Offhand bag filter:** "Filter By Offhand" with a Randomizer Bag in the offhand now matches only the blocks in
  the bag (it accepted any block before).
- **Material cost list:** the HUD list now counts every item a block needs (a double slab needs two slabs, three
  candles need three), matching what the server charges.
- **"Activate Previous Build Mode"** now returns to the mode you actually used before, including modes chosen from
  the radial menu (before, it only toggled Disable).
- **The power level hint** no longer tells you to loot power items from dungeons; it names the Reach Upgrades and
  `/powerlevel`. Remaining hard-coded English text (tooltips, the Omega bag screen, the modifier screen, build
  hints, server messages) is now translatable.
- **Fabric: your power level and modifier settings are now saved with the player** in the world save, so they
  survive a server restart and no longer carry over between singleplayer worlds (they already did on NeoForge and
  Forge).
- **Fabric: claim and protection mods are respected.** Blocks the mod breaks fire Fabric API's player block break
  events, so a claim mod listening to them can refuse the break. On Minecraft 1.18 and newer, placements and
  breaks also ask an installed mod that provides the Common Protection API, so claims are checked on placement too
  (refused blocks are not charged). The API needs Java 17, so on 1.16.x (Java 8) and 1.17.1 (Java 16) only the
  break event applies.

## Good to know

- No config or world changes are needed; the common config values used for the server checks above are the same
  ones you already had set.
- Fabric users on 1.18 and newer who want claims checked for building: install a mod that provides the Common
  Protection API (for example a claim mod built on it); it is entirely optional.

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
