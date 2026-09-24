# 02 – Upstream Sophisticated ports (Fabric, MC 1.21.1)

Source: Salandora's unofficial Fabric ports (branch `1.21.x-fabric` of
https://github.com/Salandora/SophisticatedCore, …/SophisticatedBackpacks, …/SophisticatedStorage).
The GitHub repos publish **no releases**; builds are only on CurseForge.

## Latest Fabric 1.21.1 files (verified via the CurseForge site API on 2026-09-14)

| Mod | CF project id | CF file id | File | Published | sha1 |
|-----|---------------|-----------|------|-----------|------|
| Sophisticated Core (Unofficial Fabric port) | 979317 | 7344653 | `sophisticatedcore-1.21.1-1.2.9.21.168.jar` | 2025-12-17 | `857b3862af8a55b635c2f3b03ad92b2e3f3dfdeb` |
| Sophisticated Backpacks (Unofficial Fabric port) | 979322 | 6844426 | `sophisticatedbackpacks-1.21.1-3.23.4.3.106.jar` | 2025-08-03 | `fc6d87d9b902225790bdfbc45ba412e8572e593f` |
| Sophisticated Storage (Unofficial Fabric port) | 979326 | 7344673 | `sophisticatedstorage-1.21.1-1.3.7.9.139.jar` | 2025-12-17 | `bfdf71e189715206733692d7c1f60af589e79d23` |

These are exactly the versions in the user's screenshot and are the newest 1.21.1 files
(newer uploads on those projects are 1.20.1 builds). Direct download URL pattern:
`https://www.curseforge.com/api/v1/mods/<projectId>/files/<fileId>/download`.

Where they are now:
- `Fabric-0.18.6-1.21.1/other_mods/` (production, intermediary-mapped, manifest `Fabric-Mapping-Namespace: intermediary`).
- `DevInstance_Fabric/run/mods/` next to `sophisticatedbuilding-fabric-4.0.0.jar` (so the exported-jar dev instance now actually loads SB/SC/SS; Fabric Loader remaps intermediary jars in dev automatically).

Dependency declarations of the jars (from their `fabric.mod.json`):
- core: `fabricloader >=0.16.9`, `fabric-api >=0.114.0+1.21.1`, `forgeconfigapiport >=21.1.3`, `team_reborn_energy >=4.1.0`; bundles porting_lib modules (`core`, `transfer`, `fluids`, `loot`, `model_loader`, `conditions`, `lazy_registration`, `render_types`, `level_events`, `item_abilities` 3.1.0-beta.47) and `energy-4.1.0` as jar-in-jar.
- backpacks / storage: `sophisticatedcore >=1.21.1-1.2.9.15 <1.22`.

Optional integrations shipped inside the Backpacks port: Trinkets (`compat/trinkets/TrinketsCompat`), Curios, Chipped, Litematica, EMI/JEI/REI.

## API facts that the fixes depend on (decompiled with Vineflower 1.11.1 from the Loom-remapped jars)

### SophisticatedCore `net.p3pp3rf1y.sophisticatedcore`

`upgrades.IUpgradeWrapper`
```
boolean isEnabled(); void setEnabled(boolean); default boolean canBeDisabled();
ItemStack getUpgradeStack(); default boolean hideSettingsTab();
default void onBeforeRemoved(); default void onAdded();
```

`upgrades.UpgradeWrapperBase<W extends IUpgradeWrapper, T extends UpgradeItemBase<W>> implements IUpgradeWrapper`
```
protected UpgradeWrapperBase(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler)
protected final IStorageWrapper storageWrapper; protected final ItemStack upgrade; protected final T upgradeItem; protected final Consumer<ItemStack> upgradeSaveHandler;
public ItemStack getUpgradeStack(); protected void save();   // save() == upgradeSaveHandler.accept(upgrade)
public boolean isEnabled()  { return upgrade.getOrDefault(ModCoreDataComponents.ENABLED, true); }   // (via the sophisticatedCore_getOrDefault duck interface)
public void setEnabled(boolean enabled) { upgrade.set(ModCoreDataComponents.ENABLED, enabled); save(); storageWrapper.getUpgradeHandler().refreshWrappersThatImplementAndTypeWrappers(); }
```
`init.ModCoreDataComponents.ENABLED` is a `Supplier<DataComponentType<Boolean>>`.

`upgrades.UpgradeHandler` (extends porting_lib `ItemStackHandler`)
- `Map<Integer, IUpgradeWrapper> getSlotWrappers()` – all installed wrappers, enabled or not (lazy, cached).
- `<T> List<T> getTypeWrappers(UpgradeType<T>)` – **only wrappers whose `isEnabled()` was true when the type cache was built**. The cache is rebuilt only by `refreshWrappersThatImplementAndTypeWrappers()` (called by `UpgradeWrapperBase.setEnabled`) or `refreshUpgradeWrappers()` (slot content change). A wrapper that manages `enabled` itself without calling the refresh leaves a stale cache.
- `isItemValid(slot, stack)` = `stack.isEmpty() || stack.getItem() instanceof IUpgradeItem`; the backpack subclass additionally requires `stack.is(ModItems.BACKPACK_UPGRADE_TAG)` (`sophisticatedbackpacks:upgrade`).
- Wrapper creation: `((IUpgradeItem) item).getType().create(storageWrapper, upgradeStack, saveHandler)`; the save handler does `setStackInSlot(slot, stack)` with `justSavingNbtChange = true` (no wrapper refresh).
- Constructor deserializes `contentsNbt.getCompound("upgradeInventory")` via `RegistryHelper.getRegistryAccess()` (client registry on the client thread, server registry otherwise).

`inventory.InventoryHandler` (extends porting_lib `ItemStackHandler`): `int getSlotCount()`, `ItemStack getStackInSlot(int)`, `ItemStack extractItem(int slot, int amount, boolean simulate)`, `ItemStack insertItem(int, ItemStack, boolean)`, `int getSlotLimit(int)`. (`getSlots()` returns the transfer-API slot list, not a count.)

`api.IStorageWrapper`: `getInventoryHandler()`, `getUpgradeHandler()`, `Optional<UUID> getContentsUuid()`, `getStorageType()`, `getDisplayName()`, …

`inventory.StorageWrapperRepository.getStorageWrapper(stack, Class, factory)` – Guava cache keyed by **ItemStack identity** (no equals/hashCode override), expires 10 min after access. A new stack instance (any slot re-sync from the server) yields a brand-new wrapper.

`common.gui.UpgradeContainerBase<W, C>`: ctor `(Player, int upgradeContainerId, W upgradeWrapper, UpgradeContainerType<W,C>)`; `protected W upgradeWrapper`; `abstract void handlePacket(CompoundTag)`; `sendBooleanToServer(String, boolean)` / `sendDataToServer(Supplier<CompoundTag>)` only act on the client and route to `StorageContainerMenuBase.handlePacket` → `upgradeContainers.get(containerId).handlePacket(data)` on the server.

`common.gui.StorageContainerMenuBase`: `setUpgradeEnabled(int slot, boolean)` (client sends `{upgradeEnabled, upgradeSlot}`; both sides call `slotWrappers.get(slot).setEnabled(enabled)`), `getUpgradeEnabled(slot)`, `canDisableUpgrade(slot)` (= wrapper `canBeDisabled()`). `client.gui.StorageScreenBase` renders one `ButtonDefinitions.UPGRADE_SWITCH` toggle per upgrade slot that `canDisableUpgrade` – i.e. **Core already provides the on/off switch for our upgrade**. Backpacks also has a keybind path `network.UpgradeTogglePayload` → `wrapper.setEnabled(!wrapper.isEnabled())`.

`client.gui.UpgradeSettingsTab<C>`: ctor `(C container, Position, StorageScreenBase<?>, Component tabLabel, Component closedTooltip)`, abstract `moveSlotsToTab()`, inherits `addHideableChild(...)` from `SettingsTabBase`. `client.gui.UpgradeGuiManager.registerTab(UpgradeContainerType, IUpgradeSettingsFactory)`.

`common.gui.UpgradeContainerRegistry.register(ResourceLocation itemId, UpgradeContainerType)`; `instantiateContainer` requires `item instanceof IUpgradeItem && !wrapper.hideSettingsTab()`.

`upgrades.UpgradeItemBase<T>`: ctor `(IUpgradeCountLimitConfig)`; `IUpgradeItem`: `getType()`, `getUpgradeConflicts()`, `getUpgradesPerStorage(String)`, `getUpgradesInGroupPerStorage(String)`, `getUpgradeGroup()`, `getName()`, `canAddUpgradeTo(IStorageWrapper, ItemStack, boolean firstLevelStorage, boolean isClientSide)`, `canRemoveUpgradeFrom(IStorageWrapper, boolean)` (+ overload with `Player`), `canSwapUpgradeFor(ItemStack, int, IStorageWrapper, boolean)`.

### SophisticatedBackpacks `net.p3pp3rf1y.sophisticatedbackpacks`

`backpack.wrapper.BackpackWrapper implements IBackpackWrapper (extends IStorageWrapper)`
- `public BackpackWrapper(ItemStack)`; `public static IBackpackWrapper fromStack(ItemStack)` (through `StorageWrapperRepository`); `public static Optional<IBackpackWrapper> fromExistingData(ItemStack)`.
- `getUpgradeHandler()`: real handler only if `getContentsUuid().isPresent()` (component `ModCoreDataComponents.STORAGE_UUID`), else `IBackpackWrapper.Noop.INSTANCE.getUpgradeHandler()`. Contents NBT comes from `BackpackStorage.get().getOrCreateBackpackContents(uuid)`.
- `backpack.BackpackStorage.get()`: on the **server thread** the world `SavedData` (`sophisticatedbackpacks`); on the **client** a static `clientStorageCopy` that only receives data through `network.BackpackContentsPayload`: `settings` when a backpack GUI opens (`BackpackContainer.sendStorageSettingsToClient`), and `inventory` + `upgradeInventory` only when `client.render.ClientBackpackContentsTooltip` requests them (`RequestBackpackInventoryContentsPayload`, i.e. hovering the backpack item). **There is no sync of the upgrade inventory to the client outside of an open backpack menu.**
- `backpack.BackpackItem` (extends Core `ItemBase`, implements `Equipable`); `init.ModItems.BACKPACK_UPGRADE_TAG`.

`util.PlayerInventoryProvider` – the supported way to enumerate a player's backpacks:
```
PlayerInventoryProvider.get().runOnBackpacks(Player player, BackpackInventorySlotConsumer consumer)
interface BackpackInventorySlotConsumer { boolean accept(ItemStack backpack, String inventoryName, String identifier, int slot); } // return true = stop
```
Registered handlers: `"main"` (player.getInventory().items), `"offhand"`, `"armor"` (chest slot), and `"trinkets"` when Trinkets is loaded (`compat.trinkets.TrinketsCompat.init()` via Core's `CompatRegistry`, plus a Curios handler on Curios). Backpacks worn through Trinkets/Accessories are **not** part of `player.getInventory()`.

`common.gui.BackpackContext.PlayerInventory.getBackpackWrapper()` resolves the menu's wrapper through the same `PlayerInventoryProvider` handler + `BackpackWrapper.fromStack(stack)`, so on the server the menu wrapper and any wrapper we obtain for the same stack instance are the **same object** (shared `UpgradeHandler`, shared type-wrapper cache).
