# Sophisticated Building Update – 5.1.0 - Minecraft 1.21.5

A feature release for 1.21.5: optional balanced survival breaking and a fix for reach upgrades after respawn. Nothing to migrate: worlds and configs of 5.0.2 load unchanged.

## Artifacts

* `sophisticatedbuilding-fabric-1.21.5-5.1.0.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.5-5.1.0.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.5-5.1.0.jar` — Forge

## New: balanced survival breaking (optional, off by default)

With it off (the default) nothing changes. With it on, mass breaking costs a survival player more, so it is no longer
a shortcut past mining.

* **What it covers:** build-mode mass breaking in survival and the mining done by survival replace. Operations that
  mine a single block, creative players and placing are never affected.
* **Where it is switched on:** on a dedicated server only the server config decides: `BalancedBreaking.enabled`
  (server config) forces it for all players. In singleplayer, and on a LAN world you host, the player switches it on
  with the new **Balanced Breaking** toggle in the player settings (new "Gameplay" group; client config
  `Gameplay.balancedBreaking`, default off); the server config's `BalancedBreaking.enabled` counts there too.
* **Durability:** every mined block costs the tool `durabilityMultiplier` uses (default 5, range 1 - 20) instead of
  one. Unbreaking applies like in vanilla; with `SurvivalBreaking.stopBeforeToolBreaks` a tool is only used while it
  has more uses left than that.
* **Hunger:** `exhaustionPerBlock` (default 0.075, range 0.0 - 1.0) per mined block instead of the normal 0.005. An
  operation is refused while the food level is below `minFoodLevel` (default 7, range 0 - 20, so at 6 or lower; 0 =
  never refused).
* **Cooldown:** after each operation the player has to wait. The base is 15 s. The operations of the last 30 minutes
  of real time are counted: the first 10 keep the 15 s, every further one adds 5 s, up to 120 s (the 11th gives 20 s,
  the 31st and every one after it 120 s). Operations that are older than the window drop out, so the cooldown goes
  back down by itself. It is real time: it keeps running while the player is offline or the server is stopped, and it
  survives relog, restart, death and respawn. A refused request is not counted.
* **Feedback:** when an operation is refused, an action-bar message tells why (cooldown with the seconds left, or too
  hungry with the food level needed); the HUD shows the remaining cooldown below the break countdown.

All values are in the server config, section `BalancedBreaking` (the cooldown is checked on the server, so only the
server's values count):

| Option | Default | Range |
|---|---|---|
| `enabled` | false | |
| `durabilityMultiplier` | 5 | 1 - 20 |
| `exhaustionPerBlock` | 0.075 | 0.0 - 1.0 |
| `minFoodLevel` | 7 | 0 - 20 |
| `cooldownBaseSeconds` | 15 | 0 - 3600 |
| `cooldownWindowMinutes` | 30 | 1 - 1440 |
| `cooldownFreeUses` | 10 | 0 - 1000 |
| `cooldownStepSeconds` | 5 | 0 - 3600 |
| `cooldownMaxSeconds` | 120 | 0 - 86400 |

The client config gets one new entry, `Gameplay.balancedBreaking` (default off).

## Fixes

* **Reach upgrades looked lost after dying.** After death and respawn the client forgot the player's power level
  (its new player object started at 0) until the next relog or dimension change, so reach and the build limits shown
  and used by the previews were shorter and smaller. The server always kept the level; it now sends it to the client
  again right after the respawn. Reach upgrades are permanent: once used, they are saved with the player and kept
  through death, relog and server restart.

## Compatibility

Client and server need 5.1.0 together: the balanced breaking state travels in a new network packet. Worlds and
configs of 5.0.2 load unchanged; the new config entries are added with their defaults.

## Requirements

Unchanged from 5.0.2 (see `PATCH_NOTES_5.0.2.md`).
