# Sophisticated Building Update – 5.0.2 - Minecraft 1.21.4

A hotfix on top of 5.0.1 for NeoForge: nothing to migrate, worlds and configs of 5.0.1 load unchanged.

## Artifacts

* `sophisticatedbuilding-fabric-1.21.4-5.0.2.jar` — Fabric
* `sophisticatedbuilding-neoforge-1.21.4-5.0.2.jar` — NeoForge
* `sophisticatedbuilding-forge-1.21.4-5.0.2.jar` — Forge

## Fixes

* **NeoForge: crash when another mod adds drops.** When the mod broke blocks for a player (survival breaking with a
  build mode, survival replace, undo that mines blocks back), it posted NeoForge's block drops event
  (`BlockDropsEvent`) with a read-only empty list. A mod that adds drops in that event crashed the server or
  singleplayer world with `java.lang.UnsupportedOperationException`; it was reported with the Boon enchantment of
  Apothic Enchanting (Apotheosis modpacks). The event now carries the real drops in a changeable list, and the player
  receives exactly the drops that remain after the other mods ran, delivered where the mod's drops always went
  (inventory or backpack). Mods and enchantments that change drops (extra drops, auto-smelt, telekinesis-like, drop
  removal) now also work on blocks the mod breaks; before, they never saw the real drops. If a mod cancels the event,
  the block still breaks but drops no items and no experience, the same as NeoForge's own block breaking.
* **Fabric and Forge:** no change in behaviour. These jars are rebuilt only because shared code changed.
