# R3-GUI port notes (reference mc/1.21.1: 030ef2d, 1a0ca7c on top of 5.0.0 d8ab383)

`git -C versions/1.21.1 show 030ef2d` / `show 1a0ca7c` list every file. Both are pure client/lang/asset changes; the
server/gameplay part of 5.0.1 is on the R3 gameplay branch (b6).

## 030ef2d - previous build mode
- New pure `common/.../buildmode/BuildModeHistory.java` + `common/src/test/.../BuildModeHistoryTest.java` (3 tests): copy
  unchanged (no Minecraft classes).
- `buildmode/BuildModes.java`: fields `previousBuildMode`/`beforeDisabledBuildMode` replaced by
  `BuildModeHistory<BuildModeEnum> history = new BuildModeHistory<>(DISABLED, DISABLED, SINGLE)`; `setBuildMode` calls
  `history.changeTo(buildMode)` right after assigning `this.buildMode`; `activatePreviousBuildMode()` =
  `setBuildMode(history.previous())`; `activateDisableBuildModeToggle()` = `setBuildMode(history.disableToggleTarget())`.
  Same on every version (BuildModes is identical code on all branches; check the field names).

## 1a0ca7c - translation keys, dead content, typos
- `en_us.json`: removed `key.sophisticatedbuilding.{replace,altplacement,cycle_replace_tool}.desc`,
  `key.sophisticatedbuilding.{upgrade_power_level,use_reach_upgrade,require_previous_reach_upgrades}`,
  `item.sophisticatedbuilding.{muscles,elastic_hand,building_techniques_book}[.desc]`, `sophisticatedbuilding.overlay.total`;
  `key.sophisticatedbuilding.next_power_level_how` reworded (Reach Upgrades 1, 2, 3; /powerlevel); ~55 new keys
  (`item.sophisticatedbuilding.reach_upgrade.tooltip[.previous]`, `item.sophisticatedbuilding.randomizer_bag.tooltip.*`,
  `sophisticatedbuilding.message.*`, `sophisticatedbuilding.hud.{placing,breaking}_hint`, `sophisticatedbuilding.gui.*`),
  inserted before the `player_settings` block. Copy the key set; colors are `§` codes inside the values.
  Only other lang files: none on 1.21.1 (check `lang/` on the branch; add the same keys where other languages exist).
- Deleted: `item/PowerLevelItem.java` (never registered), `models/item/{muscles,elastic_hand,building_techniques_book}.json`,
  `textures/item/{muscles,elastichand,buildingtechniquesbook}.png` (grep first: nothing else references them).
- Java, literal -> `Component.translatable(key[, args])` (same English): `ReachUpgrade{1,2,3}Item` (tooltip, 5 messages;
  `ChatFormatting` import dropped), `AbstractRandomizerBagItem` (5 tooltip lines, missing-blocks message),
  `OmegaRandomizerBagScreen` (Reset button, weight/chance/scroll tooltip; chance arg `String.format("%.1f", p)`),
  `gui/buildmodifier/{ArrayEntry,MirrorEntry,RadialMirrorEntry,BaseModifierEntry,ModifiersScreen}`, `LabeledScrollInput`
  (keyboard hint), `render/RenderHandler` (placing/breaking hint; the two ChatFormatting constants removed),
  `render/BlockPreviews` (action bar `sophisticatedbuilding.message.selection_size` with count, x, z, y), `gui/buildmode/RadialMenu`
  (power level "Creative"/"vanilla"/"%s blocks" via `I18n.get`), `ClientEvents` (two "disabled" messages).
  `SophisticatedBuilding.log(Player, Component)` / `log(Player, Component, boolean)` added (server-side translatable
  messages are translated on the client).
- Version APIs: `Component.translatable/literal` 1.19+ (1.16-1.18: `new TranslatableComponent(...)`); the item tooltip
  method signature differs per version (`appendHoverText(ItemStack, TooltipContext, List, TooltipFlag)` 1.20.5+, `Level`
  before; 1.21.5+ `TooltipDisplay` + `Consumer<Component>` - keep the branch's signature, change only the lines);
  `displayClientMessage` is `sendMessage`-style on 1.16 (`player.displayClientMessage(Component, boolean)` exists since 1.16).
- Typos: `CommonConfig` mirror-radius comment "Consume Power Level upgrades upgrades" -> "Consume Reach Upgrades";
  `OmegaRandomizerBagItem` javadoc weight "1-10" -> "1-90".
- New unit test `common/src/test/.../LangKeysTest.java`: scans `../common/src/main/java` and the loader's `src/main/java`
  for literal keys (`sophisticatedbuilding.*`, `key.sophisticatedbuilding.*`, `item.sophisticatedbuilding.*`,
  `Lang.translateDirect("x")`) and fails if one is missing in en_us.json. Common tests 90 -> 94 (Fabric 108).
- Harness: `GuiScenarios` finds the Omega "Reset" button by its (translated) text - unchanged.
- Not converted here (server code, R3 gameplay branch): `ServerBlockPlacer` / `UndoRedo` chat messages ("You are not
  allowed to build.", "Too many blocks to place. Max: ...", ...).

Verified on 1.21.1: build fabric 108 / neoforge 94 / forge 94 / forge-1.21 94; runSmokeServer 11 (1 skip) / 11 / 5 / 5.
No client runs (not allowed at the time).
