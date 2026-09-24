# Sophisticated Building Update – 4.2.0

## Artifacts

* `sophisticatedbuilding-neoforge-4.2.0.jar` — NeoForge
* `sophisticatedbuilding-fabric-4.2.0.jar` — Fabric

## Major changes

### Survival replace (Fabric + NeoForge)

New, off by default. Turn it on with the `SurvivalReplace.enabled` config option (see below) to
get the radial-menu replace modes and Quick Replace in Survival, not just Creative:

* The block being replaced is mined first, using the same survival-breaking rules as mass
  breaking: the best available tool is used (including one supplied by a backpack's enabled Tool
  Swapper upgrade), tool durability is spent, drops go to your inventory, and hunger is charged
  exactly like vanilla mining.
* Protected and unbreakable blocks are skipped instead of replaced.
* A short mining delay applies before the swap happens, with the familiar on-screen "Replacing N
  blocks in X s" countdown.
* Undo and redo of a survival replace never creates blocks or items for free: undoing a replace
  mines the new block (its drops go to your inventory), and the old block is only put back if you
  still have its item, which is used up in the process.
* Placing the same block you're replacing still merges instead of wasting a placement — a slab
  becomes a double slab, or you get one more candle, sea pickle, or turtle egg on the stack — for
  the cost of a single item, same as before.

### Real config files on Fabric

Fabric now has real, file-backed config files instead of hard-coded defaults:

* `config/sophisticatedbuilding-common.json`
* `config/sophisticatedbuilding-server.json`
* `config/sophisticatedbuilding-client.json`

They use the same option names and value ranges as the NeoForge config, with `_comment_*` keys
explaining each option. Missing keys are added back in, out-of-range values are clamped, and a
config file with broken JSON is left untouched with defaults used instead — nothing gets deleted
or overwritten. Server-side option values are synced to clients when they join, the same way
NeoForge does it (the build-mode whitelist is intentionally never sent to clients).

On NeoForge, the equivalent per-world config lives at
`serverconfig/sophisticatedbuilding-server.toml`.

### Storage blocks placed with build modes keep their contents and name (fix for #4)

Placing a shulker box, a Supplementaries sack, or another storage block with a build mode (Line,
Wall, Floor, ...) used to delete the block's contents and custom name — it was placed empty, with
nothing dropped. Placement now applies the block's saved data the same way vanilla placing does,
so contents and names survive. Survival correctly consumes that exact item from your inventory;
Creative copies it as-is.

### Build-mode preview: hardened against other mods' placement bugs

The original crash here — computing the placement preview could call another mod's
`getStateForPlacement` with a null player and crash — was already fixed in 4.0.0 by passing the
real player through. 4.2.0 adds a safety net on top of that: if another mod's
`getStateForPlacement` still throws for some other reason while the preview is being computed
(seen with the Create Aeronautics mod's Create Simulated redstone magnet), that one block is
simply skipped in the preview instead of crashing the game.

### Building Upgrade: clarified and fixed (follow-up to the 4.1.0 fix, closes #3)

The Building Upgrade only supplies blocks for a build from a backpack when that backpack has an
**enabled Building Upgrade** — the Refill upgrade does not feed builds. With that clarified, a few
bugs found while auditing the feature are fixed:

* **Fabric:** the last block of a held item could get stuck un-placeable once an upgrade was
  active, even if none of the backpacks actually held that item.
* **NeoForge:** backpacks worn in Curios slots were counted twice, doubling both the reported
  amount and what got taken from them.
* **NeoForge:** the Building Upgrade tooltip no longer promises a per-use supply cap — NeoForge
  doesn't enforce one, unlike Fabric.
* Backpack-sync hardening: a wider range of upstream errors while scanning backpacks is now
  caught, instead of only a narrower subset.

### Other fixes

* The survival server now refuses to silently overwrite a non-replaceable block without actually
  mining it first, closing a hole a modified client could otherwise use.
* Undoing a survival break no longer throws an error, and correctly costs you the block's item
  back like any other undo.

## Compatibility

Same upstream Sophisticated versions as 4.1.1:

* **Fabric** compiles and runs against the unofficial 1.21.1 ports (Sophisticated Core
  1.2.9.21.168, Sophisticated Backpacks 3.23.4.3.106).
* **NeoForge** compiles against Sophisticated Core 1.5.1.2341 and Sophisticated Backpacks
  3.26.3.2158, but its compatibility declarations are unchanged, so both older and newer
  Backpacks/Core installs are still accepted at runtime.

## Update recommendation

* Survival replace is off by default — enable `SurvivalReplace.enabled` in your server config if
  you want it.
* If you rely on worn backpacks in Curios slots on NeoForge: the HUD count and the amount taken
  from those backpacks were doubled before this update; they now correctly match the backpacks'
  real contents.
