# Sophisticated Building Update – 4.2.1

4.2.1 contains everything from 4.2.0 (see PATCH_NOTES_4.2.0.md) plus the following fixes.

## Artifacts

* `sophisticatedbuilding-neoforge-4.2.1.jar` — NeoForge
* `sophisticatedbuilding-fabric-4.2.1.jar` — Fabric

## Fixes

### Undo/redo no longer duplicates items

Undo and redo of a survival build now costs the real number of items a block is actually made of:
a double slab counts as 2 slabs, and candles, sea pickles, turtle eggs, snow layers and pink
petals count as however many are stacked on the block. Restoring a merge on top of the same block
without mining it only costs the difference — redoing a slab merge (single slab → double slab)
costs one slab. Undoing back through a merge does mine the block: undoing a double slab back to a
single slab mines the double slab (2 slabs to your inventory) and places a single slab (1 slab).
Previously undo/redo always charged exactly one item regardless of how many were actually in the
block, which let undo duplicate items for anything stacked higher than one.

### Failed undo/redo entries are no longer lost

If an undo or redo step can't be applied right now — you're missing the item to place back, or the
block can't be mined — that entry now stays on the undo/redo stack instead of being dropped. The
next Ctrl+Z / Ctrl+Y retries it, and you get a message when nothing in the batch could be undone or
redone.

### Same-block merges limited to real stackable blocks

Building over the same block only merges into a single placement for blocks that are actually
meant to stack that way: slab → double slab, or one more candle, sea pickle, turtle egg, snow
layer or pink petal on top of what's already there. Blocks with other kinds of state — crop growth
stages, composter fill level, a respawn anchor's charge, and similar — can no longer be advanced or
changed for free by building over them.

### Disable mode no longer double-processes vanilla clicks

In Disable mode, a normal placement or break now goes through vanilla only — the mod no longer
processes that same block a second time. Mirrored and arrayed copies from mirror/array settings
are still placed or broken by the mod as before. With Quick Replace enabled, the mod still handles
the replacement itself as intended. A single-block click in Disable mode no longer sends anything
to the server at all.

### Build-mode placing/breaking respects protections in every game mode

Placing and breaking blocks through any build mode, including mirror and array copies, now
respects spawn protection, the world border, and adventure-mode block-interaction rules exactly
like vanilla placing and mining do, in every game mode — not just in the cases that were already
covered.

### NeoForge: Building Upgrade supply capped per build, tooltip restored

On NeoForge, the Building Upgrade now supplies at most its tier's block count for a single build —
"Place up to N blocks at once" — matching how it already worked on Fabric. While a Building Upgrade
backpack contains the block you're holding, the last block in your hand is no longer used up, so
you keep holding it and can keep building from the backpack. The tooltip that explains the
per-build cap is back.

### Fabric: broken config files are corrected instead of warning forever

If a Fabric config file (`sophisticatedbuilding-common.json`, `-server.json`, `-client.json`) has
an out-of-range value, a value of the wrong type, or an unknown key, it's now corrected
automatically and the original is kept as `<name>.json.bak` (or `-1`, `-2`, ... if a backup already
exists) instead of the game re-warning about it on every single startup.

### Randomizer bag keeps custom names on the remaining stack

When a randomizer bag build takes blocks from your inventory, a named or otherwise customised
stack now keeps its name/data on the items that remain in your inventory afterwards — previously
the rest of the stack lost it.

## Known issues

* An `ERROR` line reading "No data fixer registered for" at Fabric startup comes from the
  unofficial Sophisticated Backpacks Fabric port, not from this mod. It's harmless and can be
  ignored.

## Compatibility

Unchanged from 4.2.0:

* **Fabric** compiles and runs against the unofficial 1.21.1 ports (Sophisticated Core
  1.2.9.21.168, Sophisticated Backpacks 3.23.4.3.106).
* **NeoForge** compiles against Sophisticated Core 1.5.1.2341 and Sophisticated Backpacks
  3.26.3.2158, but its compatibility declarations are unchanged, so both older and newer
  Backpacks/Core installs are still accepted at runtime.
