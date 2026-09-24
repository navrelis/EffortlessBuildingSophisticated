# Sophisticated Building Update – 4.1.1 (hotfix)

## What this fixes

**NeoForge crash on login with Sophisticated Backpacks 3.26.0 or newer.** If you ran
sophisticatedbuilding-neoforge 4.1.0 together with `sophisticatedbackpacks-1.21.1` 3.26.0, 3.26.1,
3.26.2 or 3.26.3, the server crashed shortly after a player logged in with:

```
NoSuchMethodError: PlayerInventoryProvider.runOnBackpacks(Player, BackpackInventorySlotConsumer)
```

Root cause: Sophisticated Backpacks 3.26.0 changed `runOnBackpacks` to return `boolean` instead of
`void`. That is a source-compatible but binary-incompatible change — our 4.1.0 jar was compiled
against the old `void` signature, so it could no longer link against the new one and the whole
server tick loop died instead of just disabling the Building Upgrade feature.

## The fix

* The mod now calls `runOnBackpacks` through a small reflection/`MethodHandle` shim
  (`BackpackScanCompat`) that ignores the declared return type. **Both older Backpacks builds
  (<= 3.25.x, including every Fabric port) and newer ones (>= 3.26.0) now work with the same jar** —
  no more recompiling every time this one method's return type changes upstream.
* Every place that touches Sophisticated Backpacks/Core code now catches `LinkageError` in addition
  to `Exception`/`NoClassDefFoundError`. `NoSuchMethodError` and friends are `LinkageError`s, not
  plain exceptions, so the old guards let this kind of upstream break straight through to the
  server tick loop. If the *next* upstream release breaks something else we call, the mod now
  degrades to "no Building Upgrade found" instead of crashing the server.
* **One-time warning instead of silence.** The first time the Backpacks scan can't be linked at
  all, you get a single WARN line in the log naming the installed Backpacks version and saying the
  Building Upgrade / Tool Swapper backpack scan is disabled until the mod is rebuilt against it.
  Later occurrences in the same session are logged quietly at DEBUG instead of repeating the WARN.
* NeoForge now compiles against the current upstream (Sophisticated Core 1.5.1.2341, Sophisticated
  Backpacks 3.26.3.2158) so a future binary break in this API would show up as a compile error
  during development, not a runtime crash for players. The mod's own compatibility declarations
  (`neoforge.mods.toml` version ranges) are unchanged, so both older and newer Backpacks/Core
  installs are still accepted at runtime.
* Removed a harmless but noisy startup WARN on NeoForge: `Reference map
  'sophisticatedbuilding.refmap.json' for sophisticatedbuilding.mixins.json could not be read`. Our
  NeoForge mixin config never generates a refmap (NeoForge 1.21 runs on Mojang names and doesn't
  need one), so the stale reference is simply removed. Fabric is unaffected — Loom still generates
  its own refmap there.
* Removed stale loot-modifier data for items that no longer exist in this fork (`muscles`,
  `elastic_hand`, `building_techniques_book`, `building_techniques_book_library` — leftovers from
  the original Effortless Building mod). These produced four "Could not decode
  GlobalLootModifier" warnings on every world load on NeoForge.

## Who is affected

* **NeoForge** users running sophisticatedbuilding 4.1.0 together with Sophisticated Backpacks
  3.26.0 or later. If that was you, this is the crash-fix you need.
* **Fabric** was not affected (the unofficial Fabric port is frozen at an older, `void`-returning
  build), but gets the same shim for parity and to protect against a future port update.

## Update recommendation

Update to 4.1.1 on NeoForge if you use Sophisticated Backpacks 3.26.0+. Fabric users can update at
their convenience; there is no behavior change for the currently supported Fabric port.
