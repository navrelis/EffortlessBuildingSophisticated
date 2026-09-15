# 10 – Upstream API break: `PlayerInventoryProvider.runOnBackpacks` (hotfix 4.1.1)

Analysis by the orchestrator (Fable) on 2026-09-15 from a real crash report of a NeoForge 1.21.1
modpack (433 mods, NeoForge 21.1.250, Java 21). This file is both the root-cause record and the
task contract for the implementing agent. Read `01`, `05` (conventions) and `09` (conventions
recap) first; they still apply.

## 1. What happened

- Crash: `Exception in server tick loop` right after the player logged in.
- Stack (top frames, from `crash-2026-09-15_10.34.36-server.txt`):
  ```
  java.lang.NoSuchMethodError: 'void net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider.runOnBackpacks(net.minecraft.world.entity.player.Player, net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider$BackpackInventorySlotConsumer)'
      at sophisticated.building.item.upgrade.BuildingUpgradeHelper.findBestBuildingUpgrade(BuildingUpgradeHelper.java:43)
      at sophisticated.building.item.upgrade.BuildingUpgradeHelper.getBuildingUpgradeTier(BuildingUpgradeHelper.java:309)
      at sophisticated.building.CommonEvents.sendBuildingUpgradeState(CommonEvents.java:65)
      at sophisticated.building.CommonEvents.onPlayerLoggedIn(CommonEvents.java:209)
  ```
- Only one mod changed since the last successful world join: `sophisticatedbuilding 4.0.0 -> 4.1.0`.
- Installed Sophisticated mods (official P3pp3rf1y NeoForge builds):
  `sophisticatedcore-1.21.1-1.5.1.2341`, `sophisticatedbackpacks-1.21.1-3.26.3.2158`,
  `sophisticatedstorage-1.21.1-1.5.91.2127`.
- Integrity: the sha1 of the user's `sophisticatedbuilding-neoforge-4.1.0.jar`
  (`13a81d9dbd8d0b2d2984314e66895709192d7380`) is identical to the jar in `ExportedJars/` here.
  Nothing was tampered with and no foreign code is involved; this is a plain binary-compatibility
  bug in our jar.

## 2. Root cause (verified with `javap` on the real jars)

| Sophisticated Backpacks (NeoForge 1.21.1) | `runOnBackpacks(Player, BackpackInventorySlotConsumer)` returns |
|---|---|
| 3.25.44.1736 (what `gradle.properties` compiles against) | `void` |
| 3.25.77.2086, 3.25.78.2107 (2026-08-19) | `void` |
| **3.26.0.2116 (2026-09-05)** | **`boolean`** |
| 3.26.3.2158 (installed by the user, current latest) | `boolean` (+ new `findBackpack(...)`) |

The JVM method descriptor includes the return type. Our NeoForge jar was compiled against
3.25.44 and therefore contains `runOnBackpacks:(...)V`. Backpacks >= 3.26.0 only has
`runOnBackpacks:(...)Z`, so linking fails with `NoSuchMethodError`. Source-compatible,
binary-incompatible upstream change.

Why it crashed the server instead of degrading: every guard around Backpacks code is
`catch (Exception | NoClassDefFoundError)`. `NoSuchMethodError` is a `LinkageError`
(`IncompatibleClassChangeError`), not an `Exception` and not `NoClassDefFoundError`, so it escaped
`BuildingUpgradeHelper`, `CommonEvents.sendBuildingUpgradeState` (which catches only
`NoClassDefFoundError`) and the login event, and the server tick loop died.

Why 4.0.0 worked with the same Backpacks build: the shipped 4.0.0 jar was built from commit
`46926b4`, before `cf51b35` introduced `PlayerInventoryProvider.runOnBackpacks`. 4.0.0 never called
this API; 4.1.0 calls it on every login/respawn and periodically per tick.

Other API surface checked (public/protected members of every Core/Backpacks class we import,
compile-time vs. installed version): nothing else we use was removed or changed. Core
1.4.38 -> 1.5.1 only removed `UpgradeGuiManager.registerInventoryPart/getInventoryPart` (we use
`registerTab`, unchanged) and added members. `BackpackWrapper.fromStack`,
`ToolSwapperUpgradeWrapper`, `ToolSwapMode`, `UpgradeHandler`, `IStorageWrapper`,
`UpgradeSettingsTab`, `StorageScreenBase` signatures we call are unchanged.

Fabric: the Salandora port is frozen at 3.23.4.3.106 (`void`); no newer 1.21.1 file exists on
CurseForge. Fabric is not affected today but must get the same shim for parity and future-proofing.

Secondary observations from the same log (not crash-related):
- `Reference map 'sophisticatedbuilding.refmap.json' for sophisticatedbuilding.mixins.json could
  not be read`: our NeoForge mixin config declares a refmap that is never generated and has an
  empty mixin list. Harmless WARN; NeoForge 1.21 runs on Mojang names and needs no refmap. Fix by
  removing the `"refmap"` key from the **NeoForge** config only (Fabric/Loom generates one).
- `Attempted to select two dependency jars from JarJar which have the same identification`: our
  jar nests `flywheel-neoforge-1.0.6.jar` directly and again inside `ponder-neoforge`. JarJar picks
  one; harmless. Not part of this hotfix, noted for later.
- The rest of the log (JEI tooltip errors, "Cannot get config value before config is loaded",
  Puzzles Lib config warning, WaterMedia VLC missing) comes from other mods and is unrelated.

## 3. Fix design

Goal: one jar that links against **both** the `void` (<= 3.25.x, all Fabric ports) and the
`boolean` (>= 3.26.0) variants, plus defense in depth so that the *next* upstream break degrades to
"no Building Upgrade found" with one WARN line instead of killing the server.

1. **Return-type-agnostic lookup.** Reflection (`Class.getMethod(name, paramTypes)`) matches on
   name + parameter types only and ignores the return type. Wrap the resulting `Method` in a
   `MethodHandle` (`MethodHandles.publicLookup().unreflect(m)`) and call it with `invoke` as an
   expression *statement*: per JLS 15.12.3 the call-site return type is then `void` and `asType`
   drops the `boolean` result. Cache the handle in a lazy holder.
2. **Compile against the current upstream** so future breaks show up at compile time:
   NeoForge `sophisticatedcore_version=1.21.1-1.5.1.2341`,
   `sophisticatedbackpacks_version=1.21.1-3.26.3.2158` (both on the Modrinth maven already used;
   Backpacks 3.26.3 requires Core `[1.5.1,)`, hence bumping both). The shim makes the compiled
   descriptor irrelevant, so older Backpacks keep working; `neoforge.mods.toml` ranges stay
   `[3.22.0,)` / `[1.0.0,)`.
3. **Catch `LinkageError`, not `NoClassDefFoundError`,** at every boundary that touches
   `net.p3pp3rf1y.*` code.
4. **Warn once.** Today the scan failure is logged at DEBUG and vanished from the user's log. The
   first failure per JVM must be a WARN naming the installed Backpacks version; later ones DEBUG.

## 4. Tasks (contract). Version becomes **4.1.1** on both loaders.

Do the Fabric project first (it has the JUnit setup), then NeoForge. One commit per task, trailer
`Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, `git push origin main` after each
commit, no red build. Do **not** touch `graphify-out/` (orchestrator owns it) and do not run any
`graphify` command. Do not stage `graphify-out/` in your commits.

### T-U1 – MC-free invoker + unit test (Fabric first, then copy byte-identical to NeoForge)

New `sophisticated/building/utilities/ReturnTypeAgnosticInvoker.java` with **no Minecraft or
Sophisticated imports**, so it is testable like `ToolSelectorTest`:

```java
public final class ReturnTypeAgnosticInvoker {
    private ReturnTypeAgnosticInvoker() {}

    /**
     * Finds the public instance method {@code name(paramTypes...)} on {@code owner} regardless of
     * its declared return type and returns a handle for it, or empty if it does not exist or is
     * not accessible. Never throws.
     */
    public static Optional<MethodHandle> findVirtualIgnoringReturnType(Class<?> owner, String name, Class<?>... paramTypes)
}
```
Implementation: `owner.getMethod(name, paramTypes)` then `MethodHandles.publicLookup().unreflect(m)`;
catch `NoSuchMethodException | IllegalAccessException | SecurityException | LinkageError` and
return `Optional.empty()`.

Test `src/test/java/sophisticated/building/ReturnTypeAgnosticInvokerTest.java` with two nested
public static dummy classes, one with `public void runOnBackpacks(Object player, Runnable c)` and
one with `public boolean runOnBackpacks(Object player, Runnable c)` (returning `true`), both
recording that they were called. Assert: (a) both resolve; (b) invoking each handle as a statement
`handle.invoke(instance, player, consumer);` executes the body without exception (this proves the
`boolean -> void` adaptation); (c) a missing method name yields `Optional.empty()`; (d) wrong
parameter types yield `Optional.empty()`. Fabric `.\gradlew.bat build --no-daemon` must show the
new tests passing (there are 17 today).

### T-U2 – `BackpackScanCompat` and use it at all three call sites (Fabric, then NeoForge)

New `sophisticated/building/integration/BackpackScanCompat.java` (package rule from `09`: it
imports `net.p3pp3rf1y.*`):

```java
public final class BackpackScanCompat {
    /**
     * Runs consumer over every backpack SophisticatedBackpacks knows for the player (inventory,
     * offhand, armour slot, Trinkets/Accessories/Curios handlers). Works with Backpacks builds where
     * runOnBackpacks returns void (<= 3.25.x, all Fabric ports) and boolean (>= 3.26.0).
     *
     * @return true if the scan ran, false if the Backpacks API could not be linked (already logged)
     */
    public static boolean forEachBackpack(Player player, PlayerInventoryProvider.BackpackInventorySlotConsumer consumer)
}
```
- Lazy holder for the `MethodHandle` obtained via
  `ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(PlayerInventoryProvider.class,
  "runOnBackpacks", Player.class, PlayerInventoryProvider.BackpackInventorySlotConsumer.class)`.
- Call: `HANDLE.invoke(PlayerInventoryProvider.get(), player, consumer);` as a statement. Catch
  `Throwable` around the invoke: rethrow `RuntimeException` and non-`LinkageError` `Error`s
  unchanged (callers handle them as before); for `LinkageError` and for the "handle unavailable"
  case log once at WARN and return false. Checked exceptions cannot come out of this API; wrap any
  in `RuntimeException` rather than declaring `Throwable`.
- Warn-once: `private static final AtomicBoolean REPORTED`. The message must contain the Backpacks
  version (NeoForge: `ModList.get().getModContainerById("sophisticatedbackpacks")` and
  `getModInfo().getVersion()`; Fabric: `FabricLoader.getInstance().getModContainer("sophisticatedbackpacks")`
  and `getMetadata().getVersion()`), and say that the Building Upgrade / Tool Swapper backpack scan
  is disabled until the mod is rebuilt against that Backpacks build. Subsequent failures DEBUG.
- Replace the three direct `PlayerInventoryProvider.get().runOnBackpacks(player, ...)` calls:
  `BuildingUpgradeHelper.findBestBuildingUpgrade`, `BuildingUpgradeHelper.findAllBuildingUpgrades`,
  `ToolSwapperIntegration.collectBackpackTools`. Lambda bodies unchanged. After this task,
  `grep -rn "runOnBackpacks" src/main/java` must only hit `BackpackScanCompat` (code and javadoc).
- Keep the Fabric and NeoForge copies of `BackpackScanCompat` identical except for the
  version-lookup line (mark it with a `// loader-specific` comment).

### T-U3 – widen every guard to `LinkageError` (both loaders)

Change `catch (Exception | NoClassDefFoundError e)` to `catch (Exception | LinkageError e)` in:
`BuildingUpgradeHelper` (3x), `ToolSwapperIntegration`, `BreakToolHelper`, `CommonEvents`
(NeoForge: also the `catch (NoClassDefFoundError ignored)` in `sendBuildingUpgradeState`),
Fabric `FabricCommonEvents`, and anywhere else `grep -rn "NoClassDefFoundError" src/main/java`
still hits (update the javadoc in `ToolSwapperIntegration` and the rule text it quotes). The
`SophisticatedBuilding.java` `catch (Throwable)` blocks on Fabric stay as they are.
In `BuildingUpgradeHelper` the three catch blocks currently log at DEBUG; keep DEBUG there (the
WARN-once now lives in `BackpackScanCompat`).

### T-U4 – NeoForge compile deps, mixin refmap, version, patch notes, export

1. `Neoforge-21.1.217-1.21.1/gradle.properties`: `sophisticatedcore_version=1.21.1-1.5.1.2341`
   and `sophisticatedbackpacks_version=1.21.1-3.26.3.2158`. Build must stay green; if the newer
   Core breaks compilation of our GUI classes, stop and report; do not patch around it silently.
2. Remove the `"refmap"` line from
   `Neoforge-21.1.217-1.21.1/src/main/resources/sophisticatedbuilding.mixins.json` only. Leave the
   Fabric config alone.
3. `mod_version=4.1.1` in both `gradle.properties`.
4. New root `PATCH_NOTES_4.1.1.md` (short, user-facing, same style as 4.1.0): the crash symptom,
   the affected combination (4.1.0 NeoForge + Sophisticated Backpacks >= 3.26.0), the fix, the
   note that both older and newer Backpacks now work, the one-WARN-line behaviour on future
   breaks, and the refmap warning removal.
5. Run `.\rebuild_all_and_export_jar.ps1` from the repo root (PowerShell). Then verify and paste
   the output into your notes:
   ```
   javap -c -p <BuildingUpgradeHelper.class extracted from ExportedJars/sophisticatedbuilding-neoforge-4.1.1.jar> | grep runOnBackpacks   -> no hits
   javap -c -p <ToolSwapperIntegration.class ...> | grep runOnBackpacks                                                          -> no hits
   javap -c -p <BackpackScanCompat.class ...> | grep -E "invoke|runOnBackpacks"                                                  -> MethodHandle.invoke + the string constant only
   ```
   Do the same on the Fabric jar (class names are the same).
6. Session log: append an "Implementer notes – 4.1.1 hotfix" section to
   `ANALYSIS_AND_INSTRUCTIONS/07_SESSION_LOG.md` with commit hashes, build/test output summary,
   javap evidence and any deviation. Add `10_UPSTREAM_API_BREAK_4.1.1.md` to the numbered list in
   `ANALYSIS_AND_INSTRUCTIONS/README.md`.

### Acceptance (the orchestrator will check all of these)

- Both builds green; Fabric test count = 17 + the new invoker tests.
- No direct `invokevirtual PlayerInventoryProvider.runOnBackpacks` left in either jar.
- `grep -rn "NoClassDefFoundError" */src/main/java` only hits comments that explain the
  `LinkageError` rule (or nothing).
- `BackpackScanCompat` warns once, returns false, never throws `LinkageError` to callers.
- NeoForge compiles against Core 1.5.1.2341 / Backpacks 3.26.3.2158; `neoforge.mods.toml` ranges
  unchanged.
- Version 4.1.1 on both loaders, jars in `ExportedJars/`, `PATCH_NOTES_4.1.1.md` at root.
