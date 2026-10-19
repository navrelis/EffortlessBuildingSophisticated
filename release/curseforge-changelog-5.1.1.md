# Sophisticated Building 5.1.1

A hotfix for Minecraft 1.16.5 and 1.16.4 (Fabric, Forge 1.16.5 and Forge 1.16.4). Nothing to migrate: worlds and configs
of 5.1.0 load unchanged, and every file name keeps its loader and Minecraft version, only `5.1.0` becomes `5.1.1`.

## Fixes

- **Server limits now reach the client.** The server's limits (reach, blocks placed at once, blocks per axis, mirror
  radius of every power level) are sent to the client when you join, so the previews and build modes use the same limits
  the server checks. On 1.16.5 and 1.16.4 the client dropped this message, so it used its own config's values instead.
  On a server with different limits than your own config, the client could show or try more or less than the server
  allowed (the server always enforced its own limits). This is fixed.
- **Error on joining (Forge):** Forge logged an `Unknown payload sophisticatedbuilding:common_config_sync` error on
  every join, Fabric a warning. Both are gone.

## Good to know

- Only the 1.16.5 and 1.16.4 files change. All other Minecraft versions stay on 5.1.0 and are not affected.
- With the default limits on both sides nothing was visible before.
- No config or world changes are needed. The fix is in the client: update the client to get it (updating the server too is fine and recommended, so everyone matches).

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
