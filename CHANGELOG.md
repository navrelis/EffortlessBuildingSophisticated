# Changelog

One section per mod version. Every version before 5.0.0 shipped only for Minecraft 1.21.1
(NeoForge + Fabric); full unabridged patch notes for those live in `docs/history/PATCH_NOTES_*.md`.
Starting with 5.0.0, the mod is multi-version: each Minecraft version lives on its own `mc/<version>`
branch with its own `changelog/PATCH_NOTES_<mod version>.md`, which lists what differs on that Minecraft
version. This file summarises them; `README.md`'s support matrix lists every jar with its Minecraft
range, minimum loader version and Sophisticated Backpacks support. (4.3.0 was the working name of this
release while it was built; it was never published, so everything since 4.2.1 is 5.0.0.)

## 5.0.1 — bug fixes on top of 5.0.0

A bug-fix release for all 48 jars of 5.0.0 (1.16.3 to 26.2, Fabric/NeoForge/Forge): nothing to migrate, worlds and
configs of 5.0.0 load unchanged. Same file names with `5.0.1` in place of `5.0.0`; full per-branch notes are in
each branch's `changelog/PATCH_NOTES_5.0.1.md`.

### Fixes

* **Server checks of build requests:** the server now checks what a client asks it to build against the player's
  power level (start within reach, extent within the blocks-per-axis limit, every block within the build mode's
  reach plus the player's modifiers, more blocks than may be placed at once cut to that limit). A modified client
  could build anywhere and any amount before.
* **Common config synced:** the server's power level limits (reach, blocks at once, blocks per axis, mirror radius)
  are sent to the client on join, so previews and the modifier screen use the server's limits instead of the
  client's own config file.
* **Array limit:** the Array modifier builds at most as many copies as the blocks-per-axis limit allows (before,
  too many copies were only shown in red in the modifier screen); stored array counts and mirror radii are capped
  to the limits too.
* **Offhand bag filter:** "filtered by offhand" with a Randomizer Bag in the offhand accepted every block (and air)
  as soon as the bag held any block; it now accepts only the bag's blocks.
* **Material cost list:** the HUD list counted a double slab, three candles or four sea pickles as one item; it now
  counts every item of a block, as the server charges, and so does the red missing-item marking.
* **"Activate Previous Build Mode"** now returns to the mode actually used before (also modes picked in the radial
  menu), and "Toggle Disabled / Previous Build Mode" returns to the mode that was active before Disable.
* **Translations:** the remaining hard-coded English (Reach Upgrade and Randomizer Bag tooltips, Omega bag screen,
  modifier screen, build hints, radial menu power level, selection size, server messages) is translation keys now.
  The power level hint no longer tells you to loot power items from dungeons (there are none): it names the Reach
  Upgrades 1, 2 and 3 and `/powerlevel`. Unused texts, models and textures of items that never existed are removed.
* **Fabric: player data kept in the world save.** The power level and the mod's per-player data (modifier settings,
  build state) were kept in memory by UUID on every Fabric branch: lost when the server restarted and carried over
  from one singleplayer world into the next. They are now saved with the player, like on NeoForge and Forge, and
  follow the player on respawn.
* **Fabric: claim and protection mods.** Blocks the mod breaks now fire Fabric API's player block break events on
  every Fabric branch, so a claim mod listening to them can refuse a break.

### Per-branch differences

* **Common Protection API (optional, Fabric only):** on Fabric 1.18 and newer, placements and breaks also ask an
  installed mod that provides the Common Protection API before going through, so a claim mod that registers a
  provider can refuse a placement too (refused placements are not charged). The API needs Java 17, so on the
  1.16.3, 1.16.4/1.16.5 (Java 8) and 1.17.1 (Java 16) branches only the Fabric API break event applies; placements
  are not checked against claims on Fabric there (NeoForge and Forge already cover placements through their own
  place event).

## 5.0.0 — every Minecraft version from 1.16.3 to 26.2

Sophisticated Building now runs on every Minecraft version that has a Sophisticated Backpacks release, from
1.16.3 to 26.2, on Fabric, NeoForge and Forge wherever that loader exists for the version: 48 jars. Pick the jar
for your Minecraft version and loader (the file name says both, e.g. `sophisticatedbuilding-neoforge-1.21.1-5.0.0.jar`).
All building features of 4.2.1 are there on every version, plus a real Player Settings screen and a long list of
fixes. Sophisticated Backpacks stays optional: where it is installed and exists for your loader and version, the
Building Upgrade feeds your builds from your backpacks.

### Supported versions and loaders

| Minecraft | Fabric | NeoForge | Forge | Sophisticated Backpacks integration |
|---|---|---|---|---|
| 26.2 | yes | yes | yes | NeoForge |
| 26.1.2 | yes (one jar for 26.1-26.1.2) | yes | yes (one jar for 26.1-26.1.2) | NeoForge |
| 26.1, 26.1.1 | yes (the 26.1-26.1.2 jar) | yes (own jar, for the NeoForge beta builds) | yes (the 26.1-26.1.2 jar) | NeoForge |
| 1.21.11, 1.21.10, 1.21.8, 1.21.5, 1.21.4 | yes | yes | yes | NeoForge |
| 1.21.1 | yes (one jar for 1.21-1.21.1) | yes (one jar for 1.21-1.21.1) | yes | Fabric and NeoForge |
| 1.21 | yes (the 1.21-1.21.1 jar) | yes (the 1.21-1.21.1 jar) | yes (own jar) | NeoForge |
| 1.20.4 | yes | yes | yes | Fabric and NeoForge |
| 1.20.1 | yes | the Forge jar | yes | Fabric, Forge and NeoForge |
| 1.19.4 | yes | - | yes | Fabric |
| 1.19.2 | yes (one jar for 1.19-1.19.2) | - | yes | Fabric and Forge |
| 1.19, 1.19.1 | yes (the 1.19-1.19.2 jar) | - | yes (one jar for 1.19-1.19.1) | Forge |
| 1.18.2 | yes | - | yes | Forge |
| 1.18.1, 1.18 | yes (one jar for both) | - | yes (one jar each) | Forge |
| 1.17.1 | yes | - | yes | Forge |
| 1.16.5, 1.16.4 | yes (one jar for both) | - | yes (one jar each) | Forge |
| 1.16.3 | yes | - | yes | Forge |

Where Sophisticated Backpacks has no build for a loader and version (for example Fabric 1.21.4 or any Forge
version from 1.20.4 on), the mod works as usual without it; the Building Upgrade items are then placeholders
without recipes. The "Fabric" integration uses the unofficial Fabric port of Sophisticated Backpacks; worn
backpacks are found in Curios slots (Forge, NeoForge) and Trinkets slots (Fabric) as well.

### New

* **Player Settings screen.** It was an unfinished screen with controls that did nothing; it is now an editor of
  your client settings:
  * Visuals: block previews on/off, previews only while building, mini block preview, max block previews, appear
    and break animation length, preview scale.
  * Performance: preview distance, update throttling, max mini previews.

  Switches are ON/OFF buttons, numbers are sliders over the allowed range (whole-number settings move in whole
  steps; the two block limits use a curve so the common values of a few thousand are easy to hit). Every setting
  has a tooltip; changes apply at once, *Reset to Defaults* restores them, *Done* (or Escape) saves them to your
  client config file (`config/sophisticatedbuilding-client.json` on Fabric, `config/sophisticatedbuilding-client.toml`
  on NeoForge and Forge).
* **A way to open it:** a new button in the radial menu (above Modifier Settings) and a new key,
  *Open Player Settings* (unbound by default; set it in Controls, category Sophisticated Building).
* **Terrain Mound options have icons.** The Natural Variation and Terrain Shape buttons of the Terrain Mound mode
  were blank; they now have icons, and the active variation and shape are highlighted like every other option.
* **Randomizer bag windows show the bag's own name,** so a bag renamed in an anvil shows its new name.
* **Forge** is supported on every version (before 5.0.0 the mod was NeoForge and Fabric only), and so are all the
  Minecraft versions in the table above (before: 1.21.1 only).

### Fixes

* **Survival, undo of a merge:** undoing a merge (one more snow layer, a slab made double, one more candle, sea
  pickle, turtle egg or pink petal) now takes that one item off the block again and gives it back; redo charges it
  again. Before, undoing a snow-layer merge lost the layer, and the other merges could not be undone at all (with
  survival replace on they were mined instead, and turtle eggs mined that way dropped nothing).
* **Survival, failed placements:** a block the server does not place is no longer charged: a placement a
  protection mod refuses (a cancelled place event on NeoForge and Forge), water plants in the Nether, or a block
  that is already there. Water plants refused in the Nether are no longer dropped as items either.
* **Survival, multi-item blocks:** a build now charges every item of the block it places, like undo and redo
  already did (three candles cost three, a double slab two). Before, such a block cost one item, which could
  duplicate items when the block the preview merged with was gone by the time the build arrived.
* **Undo stack:** undoing a build whose blocks were already removed (e.g. mined by another player) no longer fails
  every time and blocks all older undos; blocks that are already back in their old state count as undone.
* **Disable mode + Quick Replace:** the block that a single click replaces now shows its preview and outline, as in
  the other modes (plain Disable mode still places like vanilla, without a preview).
* **Fabric:** holding exactly the blocks a build needs (e.g. one stone with the rest in a backpack) no longer loses
  the held block on the client when a build-mode click replaces the vanilla placement; the server now resends the
  slot, as on NeoForge and Forge.
* **Mini Block Preview toggle is saved:** the radial menu's toggle now changes the `showMiniBlockPreview` client
  setting (before, the toggle was forgotten on restart and a value changed in the config file was only read at
  start-up).
* **Radial menu at small window sizes:** the side buttons stay inside the window at every window size and GUI scale
  (on small windows the Terrain Shape and tile-entity protection buttons were partly off screen); the power level
  summary no longer pops up on top of a button's tooltip.
* **Randomizer bag titles** no longer run past the edge of the bag window: a long title is drawn smaller, and a very
  long one (e.g. a renamed bag) is cut with "..." and shown in full when you point at it.
* **Modifier settings:** very long numbers (e.g. coordinates far from spawn) are drawn smaller instead of past the
  edges of their field.
* **Omega Randomizer Bag screen** no longer allocates a new native buffer for every weight badge on every frame.
* **Sophisticated Backpacks scan:** when the installed Sophisticated Backpacks build cannot be linked, the warning
  now names the cause instead of only saying that the scan is disabled.

Found and fixed while bringing the mod to the other versions, before any of those jars was published:

* NeoForge 1.20.4: in singleplayer, multi-block builds placed nothing.
* Fabric 1.19.2 and 1.19.4: the client crashed at start without Sophisticated Backpacks installed
  (`IllegalAccessError`).
* Forge: the mod's data pack was listed as incompatible on several versions (its `pack.mcmeta` now declares the
  pack formats of each Minecraft version).
* Forge 26.1 and 26.1.1: `AbstractMethodError` at the first rendered frame; the 26.1.2 Forge jar now runs on all
  three.

### Compatibility notes

* **Sophisticated Backpacks versions:** each jar accepts the Sophisticated Backpacks/Core builds it was tested with
  and newer ones of the same Minecraft version (per-jar minimums below). An older build stops the game at loading
  with a message naming the version.
* **NeoForge 26.1 and 26.1.1:** NeoForge only ever published beta builds for these versions; the
  `neoforge-26.1` jar needs NeoForge 26.1.0.19-beta or newer (tested on 26.1.0.19-beta and 26.1.1.15-beta). With
  Sophisticated Backpacks use Backpacks 26.1-3.25.48 and **Core 26.1-1.4.26**: Backpacks 3.25.49 to 3.25.51 need a
  Core 1.4.28 that was never released, and Core 1.4.27 needs NeoForge 26.1.2 (it crashes the server on the beta
  builds when a backpack is opened, with or without this mod). The jar refuses Core 1.4.27 at loading with a clear
  message instead.
* **Minecraft 1.16.3 (Forge):** the Sophisticated Backpacks build for 1.16.3 (1.16.4-1.0.0.94) has no Tool Swapper,
  so mass breaking never takes tools from backpacks, and no upgrade slot checks (a backpack can hold more than one
  Building Upgrade; it counts once, with its highest enabled tier).
* **Minecraft 1.16.x to 1.18.x (Forge):** Sophisticated Backpacks has no upgrade count limits there; the Building
  Upgrade itself allows one per backpack (on 1.18.x the installed one has to be taken out to change the tier).
* **Minecraft 1.19.4 (Fabric):** the Sophisticated Backpacks Fabric port's 1.19.4 files are beta builds (tested with
  Backpacks 3.19.5 build 105 and Core 0.5.109); one Building Upgrade per backpack.
* **Minecraft 1.21 (Fabric):** the Sophisticated Backpacks Fabric port has no 1.21 build; the integration works on
  1.21.1.
* **Minecraft 1.20.1 on NeoForge:** use the Forge jar (NeoForge 1.20.1 loads Forge mods; tested on
  NeoForge 1.20.1-47.1.106).
* **Forge 1.16.3 and 1.16.4 servers:** Forge 34 and 35 do not start at all on Java 8u321 or newer (their
  modlauncher fails with `NoSuchMethodError: sun.security.util.ManifestEntryVerifier.<init>`, with or without this
  mod); a dedicated server needs an older Java 8 (the Minecraft launcher's own Java 8 is fine). Forge 1.16.5 is not
  affected.
* **Recipes on 1.16.x:** there is no deepslate or amethyst, so Compressed Cobbled Deepslate is made from blackstone
  and the Diamond and Omega Randomizer Bags use prismarine crystals.
* **Config screens:** Forge (every version) and NeoForge 1.20.4 have no in-game config screen for the mod's config
  files; the Player Settings screen covers the client settings everywhere.
* **Where previews are drawn:** on Forge 1.21.4 to 26.1.2 the block previews and outlines are drawn after the rest
  of the world (after particles and weather); on Fabric 1.21.10 to 26.1.2 at the end of the main world pass (before
  particles and weather); on 26.2 by the game itself together with the world's other translucent objects (tinted
  behind water or stained glass). They look the same otherwise.
* **Key category:** since Minecraft 1.21.9 the mod's key category is `key.category.sophisticatedbuilding.main`
  (was `key.sophisticatedbuilding.category`); custom language packs need the new key. Key bindings are kept.
* **Fabric 1.21.1:** Fabric API 0.108.0 or newer is required (older builds crashed the client at start).
* **Known issue (Forge):** loading an existing singleplayer world directly with `--quickPlaySingleplayer` can crash
  with "Can not retrieve LootModifierManager until resources have loaded once"; this is a Forge bug that also
  happens with Forge's example mod alone.
* **Known issue (Fabric):** the unofficial Sophisticated Backpacks Fabric port logs a harmless
  `No data fixer registered for` error at start-up.

### Technical details

**Jars.** File names now include the Minecraft version: `sophisticatedbuilding-<loader>-<minecraft>-5.0.0.jar`
(before: `sophisticatedbuilding-<loader>-<version>.jar`). A folder `<loader>-<mc>` on a branch builds a second jar of
one loader for another Minecraft version (`forge-1.21`, `forge-1.19`, `forge-1.18`, `forge-1.16.4`,
`neoforge-26.1`). Every declared minimum (loader, Fabric API, Sophisticated Backpacks/Core) is the version the jar
was run with, not a wildcard:

| Jar | Minecraft | Loader | Requires | Java | Sophisticated Backpacks |
|---|---|---|---|---|---|
| `sophisticatedbuilding-fabric-26.2-5.0.0.jar` | 26.2 | Fabric | Fabric Loader 0.19.5, Fabric API 0.161.0 | Java 25 | - |
| `sophisticatedbuilding-forge-26.2-5.0.0.jar` | 26.2 | Forge | Forge 65.1.3 | Java 25 | - |
| `sophisticatedbuilding-neoforge-26.2-5.0.0.jar` | 26.2 | NeoForge | NeoForge 26.2.0.88 | Java 25 | yes (Backpacks [3.26.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-neoforge-26.1.2-5.0.0.jar` | 26.1.2 | NeoForge | NeoForge 26.1.2.109 | Java 25 | yes (Backpacks [3.26.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-fabric-26.1.2-5.0.0.jar` | 26.1, 26.1.1, 26.1.2 | Fabric | Fabric Loader 0.19.5, Fabric API 0.155.3 | Java 25 | - |
| `sophisticatedbuilding-forge-26.1.2-5.0.0.jar` | 26.1, 26.1.1, 26.1.2 | Forge | Forge 62.0.9 | Java 25 | - |
| `sophisticatedbuilding-neoforge-26.1-5.0.0.jar` | 26.1, 26.1.1 | NeoForge | NeoForge 26.1.0.19-beta | Java 25 | yes (Backpacks [3.25.48,), Core [1.4.26,1.4.27)) |
| `sophisticatedbuilding-fabric-1.21.11-5.0.0.jar` | 1.21.11 | Fabric | Fabric Loader 0.19.5, Fabric API 0.141.6 | Java 21 | - |
| `sophisticatedbuilding-forge-1.21.11-5.0.0.jar` | 1.21.11 | Forge | Forge 61.2.1 | Java 21 | - |
| `sophisticatedbuilding-neoforge-1.21.11-5.0.0.jar` | 1.21.11 | NeoForge | NeoForge 21.11.45 | Java 21 | yes (Backpacks [3.26.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-fabric-1.21.10-5.0.0.jar` | 1.21.10 | Fabric | Fabric Loader 0.19.5, Fabric API 0.138.4 | Java 21 | - |
| `sophisticatedbuilding-forge-1.21.10-5.0.0.jar` | 1.21.10 | Forge | Forge 60.1.15 | Java 21 | - |
| `sophisticatedbuilding-neoforge-1.21.10-5.0.0.jar` | 1.21.10 | NeoForge | NeoForge 21.10.64 | Java 21 | yes (Backpacks [3.26.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-fabric-1.21.8-5.0.0.jar` | 1.21.8 | Fabric | Fabric Loader 0.19.5, Fabric API 0.136.1 | Java 21 | - |
| `sophisticatedbuilding-forge-1.21.8-5.0.0.jar` | 1.21.8 | Forge | Forge 58.1.22 | Java 21 | - |
| `sophisticatedbuilding-neoforge-1.21.8-5.0.0.jar` | 1.21.8 | NeoForge | NeoForge 21.8.54 | Java 21 | yes (Backpacks [3.26.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-fabric-1.21.5-5.0.0.jar` | 1.21.5 | Fabric | Fabric Loader 0.19.5, Fabric API 0.128.2 | Java 21 | - |
| `sophisticatedbuilding-forge-1.21.5-5.0.0.jar` | 1.21.5 | Forge | Forge 55.0.24 | Java 21 | - |
| `sophisticatedbuilding-neoforge-1.21.5-5.0.0.jar` | 1.21.5 | NeoForge | NeoForge 21.5.98 | Java 21 | yes (Backpacks [3.27.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-fabric-1.21.4-5.0.0.jar` | 1.21.4 | Fabric | Fabric Loader 0.19.5, Fabric API 0.119.4 | Java 21 | - |
| `sophisticatedbuilding-forge-1.21.4-5.0.0.jar` | 1.21.4 | Forge | Forge 54.1.5 | Java 21 | - |
| `sophisticatedbuilding-neoforge-1.21.4-5.0.0.jar` | 1.21.4 | NeoForge | NeoForge 21.4.157 | Java 21 | yes (Backpacks [3.27.2,), Core [1.5.0,)) |
| `sophisticatedbuilding-forge-1.21.1-5.0.0.jar` | 1.21.1 | Forge | Forge 52.1.2 | Java 21 | - |
| `sophisticatedbuilding-fabric-1.21.1-5.0.0.jar` | 1.21, 1.21.1 | Fabric | Fabric Loader 0.18.6, Fabric API 0.108.0 | Java 21 | yes (unofficial Fabric port) |
| `sophisticatedbuilding-forge-1.21-5.0.0.jar` | 1.21 | Forge | Forge 51.0.33 | Java 21 | - |
| `sophisticatedbuilding-neoforge-1.21.1-5.0.0.jar` | 1.21, 1.21.1 | NeoForge | NeoForge 21.0.167 | Java 21 | yes (Backpacks [3.20.26,), Core [0.7.13,)) |
| `sophisticatedbuilding-fabric-1.20.4-5.0.0.jar` | 1.20.4 | Fabric | Fabric Loader 0.19.5, Fabric API 0.97.3 | Java 17 | yes (unofficial Fabric port) |
| `sophisticatedbuilding-forge-1.20.4-5.0.0.jar` | 1.20.4 | Forge | Forge 49.2.9 | Java 17 | - |
| `sophisticatedbuilding-neoforge-1.20.4-5.0.0.jar` | 1.20.4 | NeoForge | NeoForge 20.4.251 | Java 17 | yes (Backpacks [3.20.6,), Core [0.6.21,)) |
| `sophisticatedbuilding-fabric-1.20.1-5.0.0.jar` | 1.20.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.92.12 | Java 17 | yes (unofficial Fabric port) |
| `sophisticatedbuilding-forge-1.20.1-5.0.0.jar` | 1.20.1 | Forge + NeoForge | Forge 47.1.3 | Java 17 | yes (Backpacks [3.26.3,), Core [1.5.1,)) |
| `sophisticatedbuilding-fabric-1.19.4-5.0.0.jar` | 1.19.4 | Fabric | Fabric Loader 0.19.5, Fabric API 0.87.2 | Java 17 | yes (unofficial Fabric port) |
| `sophisticatedbuilding-forge-1.19.4-5.0.0.jar` | 1.19.4 | Forge | Forge 45.4.5 | Java 17 | - |
| `sophisticatedbuilding-forge-1.19.2-5.0.0.jar` | 1.19.2 | Forge | Forge 43.5.2 | Java 17 | yes (Backpacks [1.19.2-3.20.2.1035,), Core [1.19.2-0.6.4.730,)) |
| `sophisticatedbuilding-fabric-1.19.2-5.0.0.jar` | 1.19, 1.19.1, 1.19.2 | Fabric | Fabric Loader 0.19.5, Fabric API 0.58.0 | Java 17 | yes (unofficial Fabric port) |
| `sophisticatedbuilding-forge-1.19-5.0.0.jar` | 1.19, 1.19.1 | Forge | Forge 41.1.0 | Java 17 | yes (Backpacks [1.19-3.18.9.661,1.19.2), Core [1.19-0.4.10.87,1.19.2)) |
| `sophisticatedbuilding-fabric-1.18.2-5.0.0.jar` | 1.18.2 | Fabric | Fabric Loader 0.19.5, Fabric API 0.77.0 | Java 17 | - |
| `sophisticatedbuilding-forge-1.18.2-5.0.0.jar` | 1.18.2 | Forge | Forge 40.3.12 | Java 17 | yes (Backpacks [1.18.2-3.20.3,), Core [1.18.2-0.6.4,)) |
| `sophisticatedbuilding-forge-1.18.1-5.0.0.jar` | 1.18.1 | Forge | Forge 39.1.2 | Java 17 | yes (Backpacks [1.18.1-3.15.15,), Core [1.18.1-3.15.15,)) |
| `sophisticatedbuilding-fabric-1.18.1-5.0.0.jar` | 1.18, 1.18.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.44.0 | Java 17 | - |
| `sophisticatedbuilding-forge-1.18-5.0.0.jar` | 1.18 | Forge | Forge 38.0.17 | Java 17 | yes (Backpacks [1.18-3.12.1,)) |
| `sophisticatedbuilding-fabric-1.17.1-5.0.0.jar` | 1.17.1 | Fabric | Fabric Loader 0.19.5, Fabric API 0.46.1 | Java 16 | - |
| `sophisticatedbuilding-forge-1.17.1-5.0.0.jar` | 1.17.1 | Forge | Forge 37.1.1 | Java 16 | yes (Backpacks [1.17.1-3.12.3,)) |
| `sophisticatedbuilding-forge-1.16.5-5.0.0.jar` | 1.16.5 | Forge | Forge 36.2.42 | Java 8 | yes (Backpacks [1.16.5-3.15.20,)) |
| `sophisticatedbuilding-fabric-1.16.5-5.0.0.jar` | 1.16.4, 1.16.5 | Fabric | Fabric Loader 0.19.5, Fabric API 0.42.0 | Java 8 | - |
| `sophisticatedbuilding-forge-1.16.4-5.0.0.jar` | 1.16.4 | Forge | Forge 35.1.37 | Java 8 | yes (Backpacks [1.16.4-3.0.0.289,1.16.5)) |
| `sophisticatedbuilding-fabric-1.16.3-5.0.0.jar` | 1.16.3 | Fabric | Fabric Loader 0.19.5, Fabric API 0.25.0 | Java 8 | - |
| `sophisticatedbuilding-forge-1.16.3-5.0.0.jar` | 1.16.3 | Forge | Forge 34.1.42 | Java 8 | yes (Backpacks [1.16.4-1.0.0.94,)) |

**Code structure.** Loader-neutral code and assets are compiled once from a shared `common/` source tree into every
loader's jar (jars are about 0.9 MB instead of about 3 MB). The mod no longer bundles Flywheel/Ponder: the rendering
helpers for ghost block previews and outlines are included directly (Catnip, MIT, attribution in the jar); there is
no dependency on Flywheel, Ponder, Catnip or Create any more. `main` is the hub (docs, this changelog, the
upstream-jar manifest, scripts, `templates/branch/`); game code lives on the `mc/<version>` branches, each with one
standalone Gradle build per loader folder (`docs/ARCHITECTURE.md`, `docs/PORTING.md`). Where a Minecraft version
lacks an API or content, the branch uses the closest equivalent and its patch notes say so (item data in NBT before
1.20.5, vanilla tool classes instead of tool item tags on 1.19, the 1.21.6 GUI render states, the 1.21.9 input
events, 26.1's unobfuscated render-state GUI, 26.2's submitted world geometry).

**Changes on Minecraft 1.21.1 compared with 4.2.1** (the only version 4.2.1 shipped for):

* Fabric: the four decompress recipes use the ids `decompress_compressed_*` (a recipe book unlock of the old ids is
  reset; the recipes craft the same).
* NeoForge: the Building Upgrade state and the backpack tool list are re-sent to the client every 10 ticks, not
  only while holding a building block or a Building Upgrade backpack. The Omega bag and randomizer bag menus close
  as soon as the bag leaves both hands, as on Fabric.
* Backpack count packets ignore unknown items instead of failing, on both loaders.
* Fabric: Minecraft 1.21 or 1.21.1 (4.2.1 declared 1.21.1 and every later 1.21.x, never tested there); Fabric API
  0.108.0 or newer; Fabric Loader 0.18.6 or newer.
* NeoForge: Minecraft 1.21 or 1.21.1, NeoForge 21.0.167 or newer; Sophisticated Backpacks 3.20.26 / Core 0.7.13 or
  newer (the last 1.21 builds) are accepted.

**Server rules (all versions).** `ServerBlockPlacer` charges the item count of every placed state
(`ReplaceRules.restoreCost`: a merge costs one item, three candles onto air three), and only for blocks really set
(`BlockHelper.placeSchematicBlock` reports it; the loader's place event can refuse it). Undo of a merge
(`ReplaceRules.Action.UNMERGE`) puts the old state back without mining and gives the merged item back; undo/redo of
a block already in the target state counts as done. Merge kinds exist where Minecraft has them (candles from 1.17,
pink petals from 1.20).

**Client config API.** Config values can be set and saved from code on every loader (`ConfigValue#set`,
`IConfigHelper#save`: Fabric's own JSON backend, NeoForge `ModConfigSpec`, Forge `ForgeConfigSpec`); the Player
Settings screen and the radial menu's Mini Block Preview toggle use it.

**Fixes found while porting (details).** NeoForge 20.2-20.4 passes payload objects through the in-memory
connection without encoding them, so the server received the client's live block set, which the client cleared on
its next tick; the mod now sends a decoded copy. The Fabric 1.19.2/1.19.4 crash without Sophisticated Backpacks
came from access wideners that only the Sophisticated Backpacks dev environment applied (`RenderType.create`,
`Screen#font`); the jars now open them themselves or use accessors. Forge 64 added a `PassDefinition#extracts`
overload that Forge 62/63 do not have; the mod overrides the two-argument method, which works on Forge 62 to 64.

**Build, CI and testing.**

* Every branch has the same CI workflow (GitHub Actions: build, unit tests, Fabric GameTests, headless smoke
  server), `build-all.ps1` (always `--no-daemon`) and `release.ps1`, synced from `templates/branch/`.
* Tests on every branch and loader: unit tests (Fabric 104, NeoForge/Forge 90), 24 Fabric GameTests (server rules:
  storage data, survival replace, merges, merge undo refunds, charging, undo stack, skipFirst, world border,
  adventure mode) and an in-game smoke harness (`runSmokeServer`, `runSmokeClient`): building, undo/redo, survival
  rules, the mod's screens (randomizer bags, Player Settings, modifier widgets, radial menu icons and layout) and,
  where Sophisticated Backpacks exists, `sb.*` checks with real backpacks (supply from the Building Upgrade, disabled
  upgrade, tier cap, HUD count sync, Tool Swapper, worn backpack, upgrade settings tab). Dev-only; never in the
  release jars. The smoke client never touches the OS mouse cursor or takes the focus.
* `scripts/test-all-versions.ps1` runs build, unit tests, GameTests, servers and the smoke harness for every branch
  and loader, several instances in parallel (per-version locks, one shared game window lock); `docs/TESTING.md`.
* Release tooling on `main`: `scripts/bump-version.ps1` (mod version on every branch, release jars rebuilt) and
  `scripts/curseforge-upload.ps1` (CurseForge upload plan from each jar's own metadata; `docs/RELEASING.md`).
* ModDevGradle Legacy builds (Forge 1.17.1-1.20.1) always recompile Minecraft, also on CI (otherwise unit tests
  failed on CI only with a signature error for Forge's signed classes).

## 4.2.1

* Undo/redo now costs and restores the real number of items a block is made of (double slabs,
  stacked candles/sea pickles/turtle eggs/snow layers/pink petals) instead of always charging one
  item, which previously let undo duplicate items.
* Failed undo/redo entries stay on the stack instead of being dropped, and are retried on the next
  undo/redo.
* Same-block merges (slab → double slab, stacking candles/etc.) are limited to blocks that are
  actually meant to stack that way; blocks with other state (crop growth, composter fill,
  respawn-anchor charge, ...) can no longer be advanced for free by building over them.
* Disable mode no longer double-processes a normal vanilla click; mirror/array copies and Quick
  Replace still go through the mod as before.
* Build-mode placing/breaking (including mirror/array copies) now respects spawn protection, the
  world border, and adventure-mode rules in every game mode.
* NeoForge: the Building Upgrade now caps supply per build at its tier's block count (matching
  Fabric) and keeps the last block in hand instead of consuming it while the backpack still has
  more; the tooltip explaining the cap is back.
* Fabric: a broken config file (out-of-range value, wrong type, unknown key) is now corrected
  automatically, with the original kept as a `.bak` file, instead of re-warning on every startup.
* Randomizer bag builds no longer strip the name/custom data from the remaining stack in your
  inventory.

## 4.2.0

* **Survival replace** (off by default, `SurvivalReplace.enabled`): the radial-menu replace modes
  and Quick Replace now work in Survival, using the same survival-breaking rules as mass breaking
  (best available tool, durability, drops, hunger, protected/unbreakable-block skipping, mining
  delay). Undo/redo of a survival replace never creates blocks or items for free.
* **Real, file-backed config on Fabric** (`config/sophisticatedbuilding-{common,server,client}.json`),
  matching the NeoForge config's option names/ranges; missing keys are backfilled, out-of-range
  values clamped, and a broken file is left untouched with defaults used instead.
* Storage blocks (shulker boxes, Supplementaries sacks, ...) placed via a build mode now keep their
  contents and custom name instead of being placed empty (fix for #4).
* Build-mode placement preview is hardened against other mods' `getStateForPlacement` throwing for
  reasons other than a null player: that one block is skipped in the preview instead of crashing.
* Building Upgrade clarified (only an *enabled Building Upgrade* feeds a build, not Refill) and
  follow-up fixes: a Fabric bug where the last block of a held item could get stuck un-placeable;
  a NeoForge bug double-counting backpacks worn in Curios slots; a misleading NeoForge tooltip
  promising a per-use cap it doesn't enforce.
* The survival server now refuses to silently overwrite a non-replaceable block without mining it
  first. Undoing a survival break no longer throws and correctly refunds the block's item.

## 4.1.1 (hotfix)

Fixed a NeoForge server crash on login (`NoSuchMethodError` in
`PlayerInventoryProvider.runOnBackpacks`) when running against Sophisticated Backpacks 3.26.0+,
which changed that method's return type from `void` to `boolean` — a binary-incompatible change.
The mod now calls it through a reflection/`MethodHandle` shim (`BackpackScanCompat`) that works
against both old and new signatures, and every Sophisticated Backpacks/Core call site now catches
`LinkageError` in addition to `Exception`, so a future upstream break degrades to "no Building
Upgrade found" instead of crashing the server. Also removed a noisy harmless NeoForge startup
warning about a missing mixin refmap, and stale loot-modifier data for items that don't exist in
this fork. See `docs/history/PATCH_NOTES_4.1.1.md` and `docs/history/ANALYSIS_AND_INSTRUCTIONS/10_UPSTREAM_API_BREAK_4.1.1.md`
for the full root-cause record.

## 4.1.0

* **Survival mass breaking** (Fabric + NeoForge): bulk breaking now works in Survival, not just
  Creative, using vanilla-matching rules — a suitable tool (main hand, hotbar, inventory, offhand,
  or an enabled Tool Swapper/Advanced Tool Swapper backpack upgrade), vanilla durability/drops/
  hunger cost, correct-tool-for-drops enforcement, tools never broken by the mod, unbreakable
  blocks always skipped, hardness-0 blocks broken free-handed, and a scaling mining delay with an
  on-screen countdown/HUD. Switch to Disable mode for plain vanilla mining/placing.
* Disable mode now re-syncs to the server on every world join, fixing it getting stuck blocking
  vanilla placing/breaking after a rejoin.
* **The Building Upgrade actually works now**: worn backpacks (Trinkets/Accessories/Curios) are
  found via `PlayerInventoryProvider.runOnBackpacks`; the server is now authoritative for upgrade
  state and backpack counts via a new `BuildingUpgradeStatePacket` instead of the client
  inspecting backpack inventories directly; `BuildingUpgradeWrapper` now extends Sophisticated
  Core's `UpgradeWrapperBase` so enabling/disabling the upgrade is reliable (migration note: a
  previously-disabled Building Upgrade comes back enabled after this update).
* Radial build menu: a rebound menu key now works (no more hard-coded Left Alt fallback), and
  opening the menu no longer discards an in-progress multi-click build.
* Line Thickness (1/3/5 blocks) for Line/Diagonal Line, and Diagonal Wall Hollow/Filled, are now
  implemented (previously advertised with UI/icons but no effect).
* Fabric now compiles against the correct CurseMaven coordinates for all three Sophisticated
  1.21.1 ports, fixing a dev-runtime `NoClassDefFoundError` for porting_lib classes.
* New server config keys under `ServerConfig.survivalBreaking` (`enabled`, `stopBeforeToolBreaks`,
  `maxDelayTicks`, `exhaustionPerBlock`); NeoForge only — Fabric config loading wasn't implemented
  yet at this version, so these were in-memory defaults there.

## 4.0.0

* Fixed a `BlockPlaceContext.getPlayer()` null crash during client-side placement preview (some
  mods, e.g. Create Simulated's redstone magnet, assume a non-null player in
  `getStateForPlacement`); `MyPlaceContext` now carries the real player through.
* Fixed the radial build menu (Alt key) treating the cursor as stuck at the screen center after
  opening; it now seeds from the first frame's mouse position and accumulates movement deltas.
