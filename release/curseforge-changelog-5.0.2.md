# Sophisticated Building 5.0.2

A hotfix for NeoForge on Minecraft 1.21.1 to 26.2 (1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2 and
26.2). Nothing to migrate: worlds and configs load unchanged, and every file name keeps its loader and Minecraft
version, only `5.0.1` becomes `5.0.2`.

## Fixes

- **NeoForge: crash when another mod adds drops.** Breaking blocks with the mod (a build mode in survival, survival
  replace, or undo) could crash the server or singleplayer world with `java.lang.UnsupportedOperationException` when
  another mod added drops to the broken block. It showed with the Boon enchantment of Apothic Enchanting, so
  Apotheosis modpacks were hit. This is fixed.
- **Drops from other mods now apply:** enchantments and mods that change drops (extra drops, auto-smelt,
  telekinesis-like, drop removal) now also work on blocks the mod breaks; before, they never saw the real drops. You
  receive exactly the drops that remain, where the mod's drops always went (inventory or backpack). If another mod
  cancels the drops, the block still breaks but gives no items and no experience, as with normal block breaking.
- **Fabric and Forge:** no change in behaviour; these files are only rebuilt because shared code changed.

## Good to know

- Minecraft 1.16.3 to 1.20.4 stay on 5.0.1: they are not affected (NeoForge 1.20.4 has no such event).
- No config or world changes are needed.

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
