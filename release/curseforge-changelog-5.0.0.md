# Sophisticated Building 5.0.0

**Every Minecraft version from 1.16.3 to 26.2, on Fabric, NeoForge and Forge.** Pick the file for your Minecraft
version and loader; the file name says both (`sophisticatedbuilding-<loader>-<minecraft>-5.0.0.jar`). Sophisticated
Backpacks stays optional.

## Versions and loaders

- **26.2, 1.21.11, 1.21.10, 1.21.8, 1.21.5, 1.21.4:** Fabric, NeoForge, Forge
- **26.1, 26.1.1, 26.1.2:** Fabric and Forge (one jar each for all three), NeoForge 26.1.2, and a separate NeoForge jar for 26.1/26.1.1 (NeoForge beta builds)
- **1.21, 1.21.1:** Fabric and NeoForge (one jar each for both), Forge (one jar each)
- **1.20.4:** Fabric, NeoForge, Forge
- **1.20.1:** Fabric, Forge (the Forge jar also runs on NeoForge 1.20.1)
- **1.19.4, 1.18.2, 1.17.1, 1.16.3:** Fabric, Forge
- **1.19, 1.19.1, 1.19.2 / 1.18, 1.18.1 / 1.16.4, 1.16.5:** Fabric (one jar for each group), Forge (1.19.2, 1.19-1.19.1, 1.18.1, 1.18, 1.16.5, 1.16.4)

**Sophisticated Backpacks integration** (Building Upgrade feeds your builds from your backpacks): NeoForge on every
version it exists for (1.20.4 to 26.2), Forge 1.16.3 to 1.20.1 (not 1.19.4), Fabric 1.19.2, 1.19.4, 1.20.1, 1.20.4
and 1.21.1 (with the unofficial Fabric port). Worn backpacks in Curios or Trinkets slots count too. Elsewhere the
Building Upgrade items are placeholders and everything else works as usual.

## New

- **Player Settings screen:** a real editor of your client settings (block previews, previews only while building,
  mini previews, preview limits, animation lengths, preview scale, preview distance, update throttling). Changes apply
  at once; Done or Escape saves them; Reset to Defaults restores them.
- Open it with the **new radial menu button** (above Modifier Settings) or the **new key "Open Player Settings"**
  (unbound by default; Controls, category Sophisticated Building).
- **Terrain Mound** options now have icons, and the active ones are highlighted.
- Randomizer bag windows show the **bag's own name** (a renamed bag shows its new name).

## Fixes

- **Survival:** undoing a merge (snow layer, double slab, candle, sea pickle, turtle egg, pink petal) gives that item
  back instead of losing it or failing; redo charges it again.
- **Survival:** blocks the server does not place (refused by a protection mod, water plants in the Nether, block already
  there) are no longer charged; refused water plants are no longer dropped.
- **Survival:** a build charges every item of a multi-item block (three candles cost three), which closes an item
  duplication.
- **Undo** no longer gets stuck on a build whose blocks were already removed.
- **Disable mode + Quick Replace** shows the preview and outline of the block it replaces.
- **Fabric:** holding exactly the blocks a build needs no longer loses the held block on the client.
- The radial menu's **Mini Block Preview** toggle is now saved.
- The **radial menu** stays inside small windows; the power level summary no longer covers button tooltips.
- **Bag titles** fit the bag window (smaller, or cut with "..." and shown in full on hover).
- Very long numbers in the **modifier settings** fit their fields.
- The **Omega Randomizer Bag** screen no longer allocates memory for every weight badge on every frame.
- The Sophisticated Backpacks **scan warning** names the cause when the installed build cannot be linked.

## Good to know

- **NeoForge 26.1/26.1.1:** needs NeoForge 26.1.0.19-beta or newer. With Sophisticated Backpacks use Backpacks
  26.1-3.25.48 and **Core 26.1-1.4.26** (Core 1.4.27 needs NeoForge 26.1.2 and is refused at loading).
- **Forge 1.16.3:** that Sophisticated Backpacks version has no Tool Swapper, so mass breaking never takes tools from
  backpacks.
- **Forge 1.16.3/1.16.4 dedicated servers** need a Java 8 older than 8u321 (a Forge 34/35 limitation; the Minecraft
  launcher's Java 8 is fine).
- **Fabric 1.19.4:** the Sophisticated Backpacks Fabric port's 1.19.4 files are beta builds.
- **1.16.x recipes** use blackstone and prismarine crystals instead of deepslate and amethyst.
- **Key category** since 1.21.9: `key.category.sophisticatedbuilding.main` (language packs); your bindings are kept.
- Jar names now include the Minecraft version. Each jar declares the loader, Fabric API and Sophisticated
  Backpacks versions it was tested with as its minimum.

Full changelog: https://github.com/navrelis/EffortlessBuildingSophisticated/blob/main/CHANGELOG.md
