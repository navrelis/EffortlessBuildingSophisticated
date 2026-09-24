# Sophisticated Building Update – 4.0.0

## Supported versions

* Minecraft **1.21.1**
* **NeoForge** (see `gradle.properties` `neo_version` in the NeoForge project)
* **Fabric Loader** + Fabric API (see Fabric project `gradle.properties`)

## Artifacts

* `sophisticatedbuilding-neoforge-<version>.jar` — NeoForge
* `sophisticatedbuilding-fabric-<version>.jar` — Fabric

## Major changes

### Placement preview crash fix (compatibility)

* **Fixed** `BlockPlaceContext.getPlayer()` being **`null`** during client-side placement preview (`MyPlaceContext` previously passed `null` into `BlockPlaceContext`).
* Blocks whose `getStateForPlacement` assumes a non-null player (for example **Create Simulated** redstone magnet) could crash with `NullPointerException` when selecting those blocks in large builds.
* **Change:** `MyPlaceContext` now receives the real **`Player`**, and `BlockEntry` / `BuilderChain` pass it through for preview state resolution.

### Radial build menu (Alt key)

* **Fixed** the radial menu treating the cursor as stuck at the **screen center** after opening (common when the OS/window warps the cursor when a `Screen` opens).
* **Change:** seed from the first frame’s GUI mouse position, then accumulate **movement deltas** in `mouseMoved()` so wedge highlighting tracks the pointer reliably.

## Update recommendation

* Use **4.0.0** NeoForge and Fabric builds together if you maintain parallel loaders for the same Minecraft version.
* Re-test large-placement previews alongside mods that customize `getStateForPlacement` behavior.
