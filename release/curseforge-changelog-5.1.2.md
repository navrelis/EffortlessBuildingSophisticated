# Sophisticated Building 5.1.2

A hotfix for Forge on Minecraft 1.19.4. Nothing to migrate: worlds and configs of 5.1.0 load unchanged, and the file
name keeps its loader and Minecraft version, only `5.1.0` becomes `5.1.2`.

## Fixes

- **No more "made for a newer Minecraft version" on the built-in data pack (Forge 1.19.4).** Forge 1.19.4 did not read
  the pack format the mod declared, so it judged the mod's data pack by the wrong format and showed it as made for a
  newer Minecraft version in the data pack screen and the pack list. The world
  still loaded and building worked; the pack was only shown as incompatible. The mod now declares the pack formats the
  way Forge 1.19.4 reads them, and the data pack is reported compatible.

## Good to know

- Only the Forge 1.19.4 file changes. The Fabric 1.19.4 file is unchanged and stays on 5.1.0. All other Minecraft
  versions stay on their current version (5.1.0; 1.16.5 and 1.16.4 are on 5.1.1) and are not affected.
- No config or world changes are needed. Nothing network-related changed, so 5.1.0 and 5.1.2 can be mixed, but
  updating the Forge 1.19.4 server and client is recommended.

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
