# Sophisticated Building 5.1.2

All files of the mod are now version 5.1.2, for every Minecraft version and every loader, so the whole mod shares one
version number. The file name keeps its loader and Minecraft version, only the version part changes to `5.1.2`.
Nothing to migrate: worlds and configs of 5.1.0 load unchanged.

## Fixes

- **No more "made for a newer Minecraft version" on the built-in data pack (Forge 1.19.4).** Forge 1.19.4 did not read
  the pack format the mod declared, so it judged the mod's data pack by the wrong format and showed it as made for a
  newer Minecraft version in the data pack screen and the pack list. The world
  still loaded and building worked; the pack was only shown as incompatible. The mod now declares the pack formats the
  way Forge 1.19.4 reads them, and the data pack is reported compatible.
- **Server limits reach the client (Minecraft 1.16.5 and 1.16.4).** The 1.16.5 and 1.16.4 files include the fix from
  5.1.1: the limits set by the server (reach and build limits) are now sent to the client when a player joins, and the
  error on join is gone.

## Good to know

- For every other file only the version number changed, the content is the same as before. Nothing to migrate.
- Worlds and configs of 5.1.0 load unchanged, and no config entries were added.
- Nothing network-related changed, so 5.1.0 and 5.1.2 files of the same Minecraft version can be mixed between client
  and server. Updating the Forge 1.19.4 server and client is recommended.

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
