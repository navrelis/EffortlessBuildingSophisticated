# upstream/

Reference copies of the **Sophisticated Backpacks** mod and its matching **Sophisticated Core**
library dependency, for every (loader, Minecraft version) combination that has ever had a
CurseForge release. Pulled from CurseForge for reference / decompilation / API-diffing when
working on this project's compatibility layer. Sources:

- Official (Forge + NeoForge) Sophisticated Backpacks — CurseForge project [422301](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks), by P3pp3rF1y.
- Official (Forge + NeoForge) Sophisticated Core — CurseForge project [618298](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core), by P3pp3rF1y.
- Unofficial Fabric port of Sophisticated Backpacks — CurseForge project [979322](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks-unofficial-fabric-port), by salandora.
- Unofficial Fabric port of Sophisticated Core — CurseForge project [979317](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core-unofficial-fabric-port), by salandora.

## Layout

```
upstream/
  manifest.json          committed - the reproducible source of truth (file ids, hashes, curseMaven coords)
  fetch-upstream.ps1      committed - downloads/verifies the jars described by manifest.json
  README.md               committed - this file
  forge/<mc>/*.jar         NOT committed - downloaded on demand
  neoforge/<mc>/*.jar      NOT committed - downloaded on demand
  fabric/<mc>/*.jar        NOT committed - downloaded on demand
```

Jars are **not** committed to the repository: they are third-party binaries and this is a
public repo. `*.jar` is already covered by the root `.gitignore`. Only `manifest.json`,
`fetch-upstream.ps1` and this `README.md` are tracked.

## Refreshing the jars

From the repo root, with PowerShell 7 (`pwsh`):

```powershell
pwsh upstream/fetch-upstream.ps1
```

This reads `manifest.json` and downloads anything that's missing or whose size/SHA1 doesn't
match, leaving everything else untouched (safe to re-run any time, e.g. after a fresh clone).

Useful flags:

- `-Loader <forge|neoforge|fabric>` and `-Mc <version>` restrict the run to one pair.
- `-Force` re-downloads even files that already verify correctly.

Example: force a clean re-download of just the Fabric 1.21.1 pair:

```powershell
pwsh upstream/fetch-upstream.ps1 -Force -Loader fabric -Mc 1.21.1
```

The script talks to CurseForge's public website API (no API key needed) and downloads
sequentially, one file at a time, to be polite to the server.

To regenerate `manifest.json` itself from scratch (e.g. to pick up new CurseForge releases),
re-run the discovery process described in this folder's git history / the analysis session
that produced it: page through `https://www.curseforge.com/api/v1/mods/<project>/files` for
each of the four project ids above, pick the newest RELEASE file per (loader, mc) pair, read
each Backpacks jar's `META-INF/mods.toml` / `META-INF/neoforge.mods.toml` / `fabric.mod.json`
for its `sophisticatedcore` dependency range, and pick the newest RELEASE Core file that
satisfies it (see `warnings` in the manifest for the handful of pairs where none does).

## Loader x Minecraft version -> file matrix

| Loader | MC | Backpacks file | Core file | Core range required |
|---|---|---|---|---|
| fabric | 1.19.2 | `sophisticatedbackpacks-1.19.2-3.20.2.22.jar` | `sophisticatedcore-1.19.2-0.6.4.30.jar` | `>=1.19.2-0.6.4 <1.20` |
| fabric | 1.20.1 | `sophisticatedbackpacks-1.20.1-3.23.4.5.110.jar` | `sophisticatedcore-1.20.1-1.2.7.15.166.jar` | `>=1.20.1-1.2.7.7 <1.20.4` |
| fabric | 1.20.4 | `sophisticatedbackpacks-1.20.4-3.20.7.101.jar` | `sophisticatedcore-1.20.4-0.6.27.138.jar` | `>=1.20.4-0.6.26 <1.21` |
| fabric | 1.21.1 | `sophisticatedbackpacks-1.21.1-3.23.4.3.106.jar` | `sophisticatedcore-1.21.1-1.2.9.21.168.jar` | `>=1.21.1-1.2.9.15 <1.22` |
| forge | 1.16.3 | `sophisticatedbackpacks-1.16.4-1.0.0.94.jar` | *(none - Core bundled in the SB jar, predates the Core split)* | - |
| forge | 1.16.4 | `sophisticatedbackpacks-1.16.4-3.0.0.289.jar` | *(none - Core bundled in the SB jar, predates the Core split)* | - |
| forge | 1.16.5 | `sophisticatedbackpacks-1.16.5-3.15.20.755.jar` | *(none - Core bundled in the SB jar, predates the Core split)* | - |
| forge | 1.17.1 | `sophisticatedbackpacks-1.17.1-3.12.3.496.jar` | *(none - Core bundled in the SB jar, predates the Core split)* | - |
| forge | 1.18 | `sophisticatedbackpacks-1.18-3.12.1.433.jar` | *(none - Core bundled in the SB jar, predates the Core split)* | - |
| forge | 1.18.1 | `sophisticatedbackpacks-1.18.1-3.15.15.550.jar` | *(none - Core bundled in the SB jar, predates the Core split)* | - |
| forge | 1.18.2 | `sophisticatedbackpacks-1.18.2-3.20.3.1063.jar` | `sophisticatedcore-1.18.2-0.6.4.604.jar` | `[1.18.2-0.6.0,)` |
| forge | 1.19 | `sophisticatedbackpacks-1.19-3.18.9.661.jar` | `sophisticatedcore-1.19-0.4.10.87.jar` | `[1.19-0.4.8,)` |
| forge | 1.19.1 | `sophisticatedbackpacks-1.19-3.18.9.661.jar` | `sophisticatedcore-1.19-0.4.10.87.jar` | `[1.19-0.4.8,)` |
| forge | 1.19.2 | `sophisticatedbackpacks-1.19.2-3.20.2.1035.jar` | `sophisticatedcore-1.19.2-0.6.4.730.jar` | `[1.19.2-0.6.0,)` |
| forge | 1.20.1 | `sophisticatedbackpacks-1.20.1-3.26.3.2157.jar` | `sophisticatedcore-1.20.1-1.5.1.2335.jar` | `[1.5.1.+,)` |
| neoforge | 1.20.1 | `sophisticatedbackpacks-1.20.1-3.26.3.2157.jar` | `sophisticatedcore-1.20.1-1.5.1.2335.jar` | `[1.5.1.+,)` |
| neoforge | 1.20.4 | `sophisticatedbackpacks-1.20.4-3.20.6.1051.jar` | `sophisticatedcore-1.20.4-0.6.21.608.jar` | `[0.6.17.+,)` |
| neoforge | 1.21 | `sophisticatedbackpacks-1.21-3.20.26.1151.jar` | `sophisticatedcore-1.21-0.7.13.797.jar` | `[0.7.10,)` |
| neoforge | 1.21.1 | `sophisticatedbackpacks-1.21.1-3.26.3.2158.jar` | `sophisticatedcore-1.21.1-1.5.1.2341.jar` | `[1.5.1,)` |
| neoforge | 1.21.4 | `sophisticatedbackpacks-1.21.4-3.27.2.2153.jar` | `sophisticatedcore-1.21.4-1.5.0.2336.jar` | `[1.5.0,)` |
| neoforge | 1.21.5 | `sophisticatedbackpacks-1.21.5-3.27.2.2152.jar` | `sophisticatedcore-1.21.5-1.5.0.2338.jar` | `[1.5.0,)` |
| neoforge | 1.21.8 | `sophisticatedbackpacks-1.21.8-3.26.2.2159.jar` | `sophisticatedcore-1.21.8-1.5.0.2342.jar` | `[1.5.0,)` |
| neoforge | 1.21.10 | `sophisticatedbackpacks-1.21.10-3.26.2.2151.jar` | `sophisticatedcore-1.21.10-1.5.0.2339.jar` | `[1.5.0,)` |
| neoforge | 1.21.11 | `sophisticatedbackpacks-1.21.11-3.26.2.2155.jar` | `sophisticatedcore-1.21.11-1.5.0.2340.jar` | `[1.5.0,)` |
| neoforge | 26.1 | `sophisticatedbackpacks-26.1-3.25.51.1692.jar` | `sophisticatedcore-26.1-1.4.27.1690.jar` (*newest available - does not meet the `[1.4.28,)` the jar itself asks for*) | `[1.4.28,)` |
| neoforge | 26.1.1 | `sophisticatedbackpacks-26.1-3.25.51.1692.jar` | `sophisticatedcore-26.1-1.4.26.1688.jar` (*newest available - does not meet the `[1.4.28,)` the jar itself asks for*) | `[1.4.28,)` |
| neoforge | 26.1.2 | `sophisticatedbackpacks-26.1.2-3.26.2.2156.jar` | `sophisticatedcore-26.1.2-1.5.0.2334.jar` | `[1.5.0,)` |
| neoforge | 26.2 | `sophisticatedbackpacks-26.2-3.26.2.2154.jar` | `sophisticatedcore-26.2-1.5.0.2337.jar` | `[1.5.0,)` |

Notes:

- `26.1`, `26.1.1`, `26.1.2`, `26.2` are Minecraft's post-1.21.11 (year.week-style) version tags;
  they're separate manifest entries even where several of them share the exact same Backpacks
  jar (CurseForge tags a single file with multiple MC versions).
- `1.19`/`1.19.1` share the same Backpacks jar and the same Core jar for the same reason.
- `1.16.3` picked up the newest CurseForge file tagged for that version, which happens to be
  named `sophisticatedbackpacks-1.16.4-1.0.0.94.jar` (an early build whose filename predates
  CurseForge's per-version tagging cleanup) — its `gameVersions` tag list does include `1.16.3`.
- Fabric builds are an **unofficial third-party port**, not maintained by the original author.
- Forge `1.20.1` and NeoForge `1.20.1` resolve to the *same* CurseForge file (it's tagged for
  both loaders at that version, back when Forge and NeoForge were still cross-compatible there).
- See `manifest.json`'s `skipped` and `warnings` arrays for pairs that were intentionally left
  out (beta/alpha-only) and for the two version-gap cases noted above (NeoForge `26.1` /
  `26.1.1`, where the Core project stopped publishing releases tagged for exactly that MC
  version before reaching the version the Backpacks jar itself asks for — the newest available
  Core for that MC tag was used instead).
