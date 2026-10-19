# Sophisticated Building Update – 5.1.1 - Minecraft 1.16.5 and 1.16.4

A hotfix for 1.16.5 and 1.16.4: the server's power level limits did not reach the client. Nothing to migrate: worlds and configs of 5.1.0 load unchanged.

## Artifacts

* `sophisticatedbuilding-fabric-1.16.5-5.1.1.jar` — Fabric, Minecraft 1.16.4 and 1.16.5
* `sophisticatedbuilding-forge-1.16.5-5.1.1.jar` — Forge, Minecraft 1.16.5
* `sophisticatedbuilding-forge-1.16.4-5.1.1.jar` — Forge, Minecraft 1.16.4

## Fixes

* **The server's power level limits were not sent to the client.** Since 5.0.1 the server sends its power level limits
  (reach, blocks placed at once, blocks per axis and mirror radius of every level, from the common config) to the client
  when a player joins, so the client builds with the limits the server checks. On this Minecraft version the client
  never registered that packet (`sophisticatedbuilding:common_config_sync`), so it was dropped on arrival: Forge logged
  the error `Unknown payload sophisticatedbuilding:common_config_sync` on every join, Fabric a warning
  (`Unknown custom packed identifier`). The client then kept using the values of its own common config for reach and
  the build limits of the previews and the build modes, while the server kept checking its own. On a server whose
  limits differ from the client's (for example a server config with lower or higher limits than the defaults), the
  client therefore showed and tried to build with different limits than the server enforced: the server refused or cut
  what went over its limits, and the client did not offer what only the server's higher limits would have allowed. The
  packet is now registered like on every other version, the client receives the server's limits on join and the error
  is gone. Where client and server use the same limits (the defaults) nothing was visible.

## Compatibility

The fix is on the client side: a 5.1.1 client receives the limits from 5.1.0 and 5.1.1 servers alike, a 5.1.0 client keeps the bug on any server. Updating both client and server to 5.1.1 is recommended so every player gets it. The server's checks are unchanged by this fix.
Worlds and configs of 5.1.0 load unchanged, there are no new config entries.

## Requirements

Unchanged from 5.0.1 (see `PATCH_NOTES_5.0.1.md`).
