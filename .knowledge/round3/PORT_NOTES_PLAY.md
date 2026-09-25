# R3 gameplay port notes (5.0.1 reference: wip/r3play-1.21.1, based on mc/1.21.1 d8ab383 = 5.0.0)

Worktree `local/wt/r3play-1.21.1`. Commits (`git -C local/wt/r3play-1.21.1 show <hash>`):

| Commit | Item | What |
|---|---|---|
| 0416339 | 1 | Fabric per-player data saved with the player |
| dab018d | 3 | Fabric: Fabric API break events + Common Protection API for the mod's breaks/placements |
| 5f94ae4 | 2, 4, lang | server checks of build requests, common config sync, array limit, modifier caps, translatable server messages |
| 665b354 | 5, 6 | offhand-bag filter, material cost list full count |
| 48261e8 | - | merge of mc/1.21.1 (h5b: client lang keys, previous build mode); no conflicts |

Counts on 1.21.1 after all of it (incl. the mc/1.21.1 merge): unit tests Fabric 117, NeoForge/Forge/Forge 1.21 103
(5.0.0: 104 / 90; +9 from R3 (BuildLimitsTest 6, ModifierLimitsTest 2, SyncedValuesTest 1), +4 from h5b incl. LangKeysTest); Fabric GameTests 38 (24 + 14); runSmokeServer +1 check
`server.request_limits` on every loader (Fabric 12 with 1 skip, NeoForge 12, Forge/Forge 1.21 6), also with -PsmokeNoSb=true.

## 1. Fabric per-player data (Fabric only)

Files: `fabric/.../fabric/FabricPlayerData.java` (holder interface + save/load/copy), `fabric/.../fabric/mixin/PlayerDataMixin.java`,
`fabric/src/main/resources/sophisticatedbuilding.fabric.mixins.json` (listed in fabric.mod.json "mixins"),
`FabricPlatformHelper` (get/set/has power level, getPersistentData read the holder; the UUID maps are gone),
`FabricCommonEvents` (`ServerPlayerEvents.COPY_FROM` -> `FabricPlayerData.copy`; AFTER_RESPAWN only calls onPlayerRespawned).
Player NBT: compound `sophisticatedbuilding` { `power_level`: PowerLevel#serializeNBT, `data`: the persistent data }.
Test: `fabric/src/gametest/.../PlayerDataGameTest.java` (restart = `saveWithoutId` + `load` into a new player object without
the UUID; same-UUID fresh object sees nothing; `restoreFrom` copies).

How to persist per version on Fabric:
- 1.16.x-1.21.5: mixin into `net.minecraft.world.entity.player.Player` `addAdditionalSaveData(CompoundTag)` /
  `readAdditionalSaveData(CompoundTag)` at TAIL (Mojang names; Loom remaps them - on Loom 1.17 statically, no refmap).
  1.16.3 (Fabric API 0.25): no fabric-entity-events-v1, so no `ServerPlayerEvents.COPY_FROM` (checked: its POM has no
  such module); use a mixin into `ServerPlayer#restoreFrom(ServerPlayer, boolean)` at TAIL instead. Check 1.16.5's API
  jar the same way (javap `net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents`).
- 1.21.6+: the save methods take `ValueOutput` / `ValueInput` (`addAdditionalSaveData(ValueOutput)`,
  `readAdditionalSaveData(ValueInput)`): write the compound with `output.store("sophisticatedbuilding", CompoundTag.CODEC, tag)`
  and read it with `input.read("sophisticatedbuilding", CompoundTag.CODEC)` (check the names with javap), or use the Fabric
  Data Attachment API (`AttachmentRegistry.create(id, builder -> builder.persistent(codec).copyOnDeath())`, Fabric API
  0.95+ for 1.20.4+; on 1.21.1 it is marked experimental), which also handles the respawn copy.
- `PowerLevel#serializeNBT(HolderLookup.Provider)` needs `player.registryAccess()` (1.20.5+); older branches have the
  provider-less variant.
- NeoForge/Forge: unchanged (attachment / capability + Clone event already persisted the power level; persistent data is
  the loader's `getPersistentData()`).

## 2. Server checks of build requests + common config sync (all loaders, common code)

Files: `utilities/BuildLimits.java` (pure), `utilities/ModifierLimits.java` (NBT only),
`systems/ServerBlockPlacer.java` (`validateRequest` in `placeBlocksDelayed` and both branches of `breakBlocks`; NOT in
`applyBlockSet`/undo/redo), `network/message/ModifierSettingsPacket.java` (`DATA_KEY` public; ServerHandler caps with
`ModifierLimits.cap`), `CommonConfig.java` (`SYNCED` list, `SERVER_VALUES`, `syncedValues()`, `value(player, configValue)`),
`attachment/PowerLevel.java` (every limit through `CommonConfig.value(player, ...)`), `utilities/SyncedValues.java`,
`network/message/CommonConfigSyncPacket.java` (+ `PacketHandler` CLIENTBOUND entry, failure key
`sophisticatedbuilding.networking.common_config_sync.failed`), `CommonEvents.onPlayerLoggedIn` (sends it),
`ClientEvents.onLoggingOut` (clears it).
Rules: start (`BlockSet#firstPos`) within `max(placementReach, ceil(interactionRange) + 1) + 3` (squared block distance
like the client); `|lastPos - firstPos| + 1 <= maxBlocksPerAxis` per axis; every entry within Chebyshev distance
`max(buildModeReach, startReach) + maxBlocksPerAxis + modifierReach + 3` of the player's block position (modifierReach =
sum over the player's stored enabled modifiers: array extent, 2 x mirror/radial mirror radius, capped); more than
`maxBlocksPlacedAtOnce` unskipped entries are cut to it (the start stays). Rejections send an action-bar message.
Tests: `ServerLimitsGameTest` (through `placeBlocksDelayed`, looks at `getDelayedEntries()`), unit tests
`BuildLimitsTest`, `ModifierLimitsTest`, `SyncedValuesTest`, smoke `server.request_limits` (`ServerScenarios`,
registered in the three `*SmokeServerTests` with batch `smoke_limits`; 1.16.3/1.16.5 register it in `SmokeServer.scenarios()`).
IMPORTANT for every branch: the GameTest/smoke players must stand at their structure now (`GameTestSupport.spawnPlayer`
moves the player to `absoluteVec(3.5, 1, 3.5)`, `ServerScenarios.player` too), or every existing test is out of reach.

Version APIs:
- `Player#blockInteractionRange()`: 1.20.5+. Older: 4.5 survival / 5.0 creative (`ServerGamePacketListenerImpl` checks
  6.0 blocks), use `player.isCreative() ? 5.0 : 4.5`.
- `ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64))` and payload records: 1.20.5+. Older branches: their packet style
  (FriendlyByteBuf `writeVarIntArray`/`readVarIntArray`).
- `Entity#moveTo(Vec3)` (harness): 1.21.x; older `moveTo(x, y, z)` / `setPos`.
- `ModifierLimits` uses `switch` arrows (Java 14): Java 8 branches (1.16.x) use classic `switch`/if.
- The mirror classes (`Mirror`, `RadialMirror`) load `Minecraft` in their constructor: the server works on the NBT only.

## 3. Claim/protection mods on Fabric (Fabric only)

Files: `fabric/.../platform/FabricBlockEventHelper.java` (`fireBlockBreakEvent` fires `PlayerBlockBreakEvents.BEFORE`
(+ `CANCELED` on refusal) and asks `FabricProtection.canBreak`; `placeBlock` asks `FabricProtection.canPlace` before
placing; new `afterBlockBroken` fires `AFTER`), `fabric/.../platform/FabricProtection.java` (guarded by
`FabricLoader.isModLoaded("common-protection-api")`, catches `Exception | LinkageError`),
`common/.../platform/services/IBlockEventHelper.java` (`default afterBlockBroken`, no-op on NeoForge/Forge),
`common/.../create/foundation/utility/BlockHelper.java` (`destroyBlockAs` calls it after the block is gone, also on the
ice-to-water path), `fabric/build.gradle` (Nucleoid maven `https://maven.nucleoid.xyz` exclusive for `eu.pb4`,
`modCompileOnly` + `modGametestImplementation` of `eu.pb4:common-protection-api`, both `transitive = false` - its POM
drags an old Fabric Loader that breaks the dev launch; `loom.createRemapConfigurations(sourceSets.gametest)`),
`fabric/gradle.properties` (`common_protection_api_version=1.0.0`).
Coverage: claim mods that listen to Fabric API's player break events, and those that register a Common Protection API
provider (e.g. Patbox's GOML Reserved). Not verified against specific claim mods in this round.
Test: `ProtectionEventsGameTest` (BEFORE listener cancels one of two breaks, AFTER follows the other; a CPA provider refuses
one placement - not placed, not charged).
Versions: `PlayerBlockBreakEvents` (BEFORE/AFTER/CANCELED) exists in every Fabric API of the branches (checked in
1.16.3's fabric-events-interaction-v0 0.4.1). Common Protection API: 1.0.0
(`GameProfile` parameter, Java 17: Minecraft 1.18+/1.19+ Fabric branches up to 1.21.8), 2.0.0 (`NameAndId`, 1.21.9+ incl.
26.x). 1.16.x/1.17.1 (Java 8/16): no CPA; only the break events.

## 4. Array per-axis limit (common)

`buildmodifier/Array.java`: copies = `BuildLimits.arrayCount(count, largestOffset, maxBlocksPerAxis(player))`; server cap in
`ModifierLimits.cap` (see 2). Test `ArrayLimitGameTest` (findCoordinates with a survival player; ServerHandler caps a
stored array to axis/offset and a mirror to the radius).

## 5. Offhand bag filter (common)

`compatibility/CompatHelper.containsBlock`: a randomizer bag accepts a block only if one of its stacks is that block's
`BlockItem`; never air/null. Test `OffhandBagFilterGameTest`. Older branches: the bag inventory API differs
(`getBagInventory` over NBT before 1.20.5) - the fix is the predicate only.

## 6. Material cost list (common)

`utilities/MaterialCost.java` (new; the overlay's counting moved here), `BlockUtilities.placementCost(existing, target)`
(= `ReplaceRules.restoreCost(itemCount(target), same block ? itemCount(existing) : 0)`), `client/gui/MaterialCostOverlay`
uses `MaterialCost.tally`, `BuilderChain` marks missing items with the same count. Test `MaterialCostGameTest`. On
1.16.x use the merge kinds of the branch (no candles).

## Server messages (lang)

`SophisticatedBuilding.message(player, actionBar, key, args...)` (red `Component.translatable`); keys
`sophisticatedbuilding.message.{survival_breaking_disabled, not_allowed_to_build, not_allowed_to_use_mod, no_blocks,
too_many_blocks, mixed_place_break, not_allowed_to_undo, request_out_of_reach, request_over_axis_limit, request_capped}` in
en_us.json. `Component.translatable` is 1.19+; older `new TranslatableComponent(key, args)`.
