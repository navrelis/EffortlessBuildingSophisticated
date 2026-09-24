# R4: backward ports research (2026-09-25)

Proof builds (all green, standalone, one class referencing an SB/Core API): `local/toolchains/<mc>/<loader>/` (git-ignored).

| MC | Forge toolchain | Fabric toolchain | SB |
|---|---|---|---|
| 1.16.3 | FG6 + Gradle 8.4, Gradle JVM 17, Java 8, Forge 34.1.42 | Loom 1.17.21, Gradle 9.5.0, Java 8, FAPI 0.25.0+build.415-1.16 | Forge only |
| 1.16.5 | same, Forge 36.2.42 (runServer Done, SB 3.15.20.755 loaded) | FAPI 0.42.0+1.16 | Forge only |
| 1.17.1 | FG6 + Gradle 8.8, Gradle JVM 17, Java 16, Forge 37.1.1 | Java 16, FAPI 0.46.1+1.17 (foojay-resolver 1.0.0 for JDK 16 download) | Forge only |
| 1.18.1 | same family, Forge 39.1.2 | Java 17, FAPI 0.46.6+1.18 | Forge only |
| 1.18.2 | same family, Forge 40.3.12 | FAPI 0.77.0+1.18.2 | Forge only |
| 1.19.2 | same family, Forge 43.5.2 (runServer Done, SB 3.20.2.1035 + Core 0.6.4.730) | FAPI 0.77.0+1.19.2, SB Fabric port curse 979322:5803830 + core 979317:5803819 | Forge + Fabric |

## Facts
- No Fabric SB port below 1.19.2: Fabric 1.18.2 ... 1.16.3 ship without IBackpackIntegration (NONE).
- runOnBackpacks returns void on every jar 1.16.3-1.19.2 (BackpackScanCompat handles it).
- Upgrade framework (UpgradeContainerBase/Type/Registry, UpgradeItemBase) exists since SB's first 1.16.3 release.
- Package: `net.p3pp3rf1y.sophisticatedbackpacks.*` for core classes on 1.16.3-1.18.1; `sophisticatedcore.*` from 1.18.2 (own artifact).
- UpgradeItemBase ctor: no-arg (<=1.17.1) -> (CreativeModeTab) (1.18.1) -> (CreativeModeTab, IUpgradeCountLimitConfig) (1.18.2-1.19.2) -> (IUpgradeCountLimitConfig) (1.21.1). IUpgradeCountLimitConfig missing before 1.18.2: override getUpgradesPerStorage / getUpgradesInGroupPerStorage.
- Wrapper: <=1.17.1 only IBackpackWrapper, via capability `CapabilityBackpackWrapper.BACKPACK_WRAPPER_CAPABILITY` + `stack.getCapability(...)`; 1.18.1+ `new BackpackWrapper(stack)`, IBackpackWrapper extends IStorageWrapper.
- Tool Swapper upgrade absent on 1.16.3 (sb.tool_swapper_tools must be skipped there); present from 1.16.5.
- SLF4J absent before 1.18 (log4j). JDK 8 javac: no `options.release`. Fabric GameTest API present at 1.18.2/1.19.2, absent at 1.16.5; 1.17.1/1.18.1 unchecked.
- Forge SimpleChannel/NetworkRegistry, ClientRegistry.registerKeyBinding, ForgeConfigSpec unchanged 1.16.3-1.19.2.
- Java features in 1.21.1 common (210 files): records 17 files, instanceof patterns 37, switch expressions 9 files, var 133, List/Map/Set.of 12. Java 16 supports all; only 1.16.x (Java 8) needs a language downgrade.
- Merge verdicts unchanged: 1.19-1.19.2 viable, 1.18+1.18.1 viable, 1.16.3 separate.
