# Sophisticated Building Update – 5.1.2 - Minecraft 1.19.4 (Forge)

A hotfix for Forge on 1.19.4: the mod's built-in data pack was flagged as made for a newer Minecraft version. Nothing to migrate: worlds and configs of 5.1.0 load unchanged.

## Artifacts

* `sophisticatedbuilding-forge-1.19.4-5.1.2.jar` — Forge, Minecraft 1.19.4 (replaces `sophisticatedbuilding-forge-1.19.4-5.1.0.jar`)

The Fabric 1.19.4 jar is unchanged in content and stays on 5.1.0 (the version number of this branch is bumped to 5.1.2 in the source, but no Fabric 5.1.2 file is published).

## Fixes

* **Forge flagged the built-in data pack as made for a newer Minecraft version.** The `pack.mcmeta` of the Forge jar
  declared the format keys `forge:resource_pack_format` and `forge:data_pack_format`. Forge 45 (1.19.4) reads
  `forge:client_resources_pack_format` and `forge:server_data_pack_format` instead (since 1.19.3 the keys are built
  from the pack type name), so it ignored ours and judged the mod's built-in data pack by the vanilla `pack_format` 13.
  The data pack is for pack format 12, so Forge showed it as made for a newer Minecraft version ("too new") in the
  data pack screen and the pack list. The world still loaded and
  building worked in our tests; the pack was only shown as incompatible. `pack.mcmeta` now declares
  `forge:client_resources_pack_format` 13 and `forge:server_data_pack_format` 12. Checked in a Forge 1.19.4 client run:
  the data pack is reported compatible. Only the Forge 1.19.4 jar was affected: Forge for older Minecraft versions
  reads the old keys and 1.20 and newer use other keys.

## Compatibility

Nothing network-related changed, so 5.1.0 and 5.1.2 can be mixed between client and server. Updating the Forge 1.19.4 server and client to 5.1.2 is recommended so the pack is shown as compatible for every player.
Worlds and configs of 5.1.0 load unchanged, there are no new config entries.

## Requirements

Unchanged from 5.0.1 (see `PATCH_NOTES_5.0.1.md`).
