# Sophisticated Building Update – 5.0.1 - Minecraft 1.18.2

## Artifacts

* `sophisticatedbuilding-fabric-1.18.2-5.0.1.jar` — Fabric, Minecraft 1.18.2
* `sophisticatedbuilding-forge-1.18.2-5.0.1.jar` — Forge, Minecraft 1.18.2

## Fixes

* **Fabric:** the power level and the mod's other per-player data are now saved with the player in the world save
  (previously they were kept in memory only and reset when the server restarted); they carry over on death and when
  returning from the End, as on Forge.
* **Server checks of build requests:** the server now checks every build and break request against the player's power
  level limits: a start out of reach or an extent over the blocks per axis is refused, blocks beyond the build mode's
  and the player's modifiers' reach are refused, and more blocks than may be placed at once are cut to that limit (the
  action bar says why). Stored modifiers are capped to the limits too (array count and offset, mirror radius). The
  server's power level limits are sent to the client when joining, so the client builds with the limits the server
  checks.
* **Array limit:** the array modifier no longer creates more copies than the blocks-per-axis limit allows.
* **Fabric, claim and protection mods:** the mod's breaks now fire Fabric API's player block break events (a listener
  can cancel them), and claim mods that provide the Common Protection API are asked before the mod breaks or places a
  block.
* **Offhand randomizer bag:** a bag in the offhand only supplies the blocks that are in it (it accepted any block
  before).
* **Material cost list:** counts every item a block state needs (e.g. three candles) instead of one per block, and
  marks missing items with the same count.
* **"Activate Previous Build Mode"** returns to the mode that was actually used before (it could jump to a mode used
  earlier, or stay on Disabled).
* The mod's messages, tooltips and on-screen texts that were hard-coded in English (Reach Upgrade and randomizer bag
  tooltips, the Omega bag weight tooltip and Reset button, the modifier settings, the placing/breaking hints, the power
  level summary of the radial menu, the server's build messages) are now translatable; the power level hint names the
  Reach Upgrades and `/powerlevel`. Unused translations and item models/textures of items that never existed in the mod
  were removed.
