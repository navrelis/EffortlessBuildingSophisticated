# Sophisticated Building 5.1.0

A feature release for every Minecraft version from 1.16.3 to 26.2, on Fabric, NeoForge and Forge. It adds an optional
way to make mass breaking in Survival more demanding, and fixes reach upgrades that looked lost after dying. Nothing
to migrate: worlds and configs of 5.0.x load unchanged, and every file name keeps its loader and Minecraft version,
only the version becomes `5.1.0`. Install 5.1.0 on the client **and** the server: they need to match.

## New: Balanced survival breaking (optional, off by default)

With it off, nothing changes. With it on, build-mode mass breaking and the mining of survival replace cost more:

- **Tool durability:** every block costs the tool 5 uses instead of 1 (Unbreaking still helps; with "stop before tool
  breaks" on, a tool is only used while it has more than 5 uses left).
- **Hunger:** 0.075 exhaustion per block instead of 0.005, and you cannot start while your food level is 6 or lower.
- **Cooldown:** after each break or replace you wait 15 seconds. The last 30 minutes are counted: after 10 operations
  every further one adds 5 seconds, up to 120 seconds. It goes back down by itself as old operations drop out of the
  window. It runs in real time, also while you are offline, and survives relog, restart and death.
- **Feedback:** a message above the hotbar tells you when you are refused (cooldown seconds left, or too hungry), and
  the HUD shows the remaining cooldown below the break countdown.

Breaking a single block, Creative mode and placing are never affected.

**How to switch it on:** in singleplayer (and on a LAN world you host), use the new **Balanced Breaking** toggle in the
Player Settings. On a server, only the server config decides: set `BalancedBreaking.enabled = true` and it applies to
all players. All numbers above can be changed in the server config, section `BalancedBreaking`.

## Fixes

- **Reach upgrades looked lost after dying.** After you died and respawned, your reach and build limits were short
  again until you relogged or changed dimension. The server had always kept your upgrades; now your game gets them
  back right after the respawn. Reach upgrades are permanent: once used, they are saved with you and kept through
  death, relog and server restarts.

## Good to know

- Minecraft 1.16.3 to 1.20.4 come from 5.0.1 directly (5.0.2 was a NeoForge fix for 1.21.1 and newer).
- New config entries are added with their defaults the first time the game starts.

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
