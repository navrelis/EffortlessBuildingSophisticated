# Sophisticated Building Update – 5.1.2 - Minecraft 1.16.5 and 1.16.4

A version alignment release for 1.16.5 and 1.16.4: there is no code change in these jars compared with 5.1.1, only the version number, so that all files of the mod share 5.1.2. The 5.1.1 fix is already part of them (see Included). Nothing to migrate: worlds and configs of 5.1.1 load unchanged. Client and server compatibility is unchanged: the same-version rule from 5.1.0 still applies, and 5.1.1 and 5.1.2 jars of this branch can be mixed, since nothing network-related changed.

## Artifacts

* `sophisticatedbuilding-fabric-1.16.5-5.1.2.jar` — Fabric, Minecraft 1.16.4 and 1.16.5
* `sophisticatedbuilding-forge-1.16.5-5.1.2.jar` — Forge, Minecraft 1.16.5
* `sophisticatedbuilding-forge-1.16.4-5.1.2.jar` — Forge, Minecraft 1.16.4

## Included

The 5.1.1 fix is part of this version: the server's power level limits (sent to the client when a player joins) were never registered on the client of this Minecraft version and were dropped on arrival, which logged an error on every join. The client now receives the server's limits and the error is gone. Details in `PATCH_NOTES_5.1.1.md`.

## Requirements

Unchanged from 5.0.1 (see `PATCH_NOTES_5.0.1.md`).
