# Alternative plan: per-life player models via CPM (Customizable Player Models)

Alternative to the implemented "vanilla model swap" approach (bbmodel → `LayerDefinition` at runtime).
Kept around to potentially test later. Researched July 2026 against CPM 0.6.27a / MC 26.2.

## Why CPM

[Customizable Player Models](https://github.com/tom5454/CustomPlayerModels) (tom5454, MIT) is the
established "custom player model" mod: renderer replacement, vanilla animation parity, first-person
arms, armor interplay, multiplayer sync and persistence are all done and maintained across MC
versions (1.6.4 → 26.2, fabric + neoforge + forge). It exposes a mod API designed exactly for
server-assigned models, so the per-life model feature reduces to glue code.

Tradeoffs vs the implemented approach:

- (+) No rendering/sync code to maintain at all; survives MC renderer refactors (e.g. the 26.x
  Avatar rewrite) without work on our side.
- (+) Supports non-humanoid rigs, free-form/per-face UV, scaling, custom keyframed animations and
  gestures (`playAnimation`) — headroom the vanilla-swap approach hard-caps.
- (−) Heavyweight *required* dependency on server and every client (editor UI, social features).
- (−) Models cannot be consumed as raw `.bbmodel`: each model must be exported once through CPM's
  Blockbench plugin or its in-game editor to CPM's format (Base64 export). The existing
  `cydonia.bbmodel` would be rebuilt once on a CPM player template.

## Dependency wiring

Maven repo (add to `buildSrc/src/main/kotlin/multiloader-convention.gradle.kts` repositories,
filtered to group `com.tom5454.cpm`):

```
https://raw.githubusercontent.com/tom5454/maven/main
```

Artifacts (versions current as of July 2026):

| Module   | Dependency | Artifact |
|----------|------------|----------|
| base     | `compileOnly` | `com.tom5454.cpm:CustomPlayerModels-API:0.6.27` (version-independent, loader-agnostic) |
| fabric   | `modRuntimeOnly` (dev) / user-installed | `com.tom5454.cpm:CustomPlayerModels-Fabric-26.2:0.6.27a` |
| neoforge | `runtimeOnly` (dev) / user-installed | `com.tom5454.cpm:CustomPlayerModels-26.2:0.6.27a` |

The API jar is safe to compile against in `base` (no MC classes, uses `Player.class` generics).
Runtime jar should NOT be jar-in-jar'd (it is a full mod users install themselves); declare it in
`fabric.mod.json`/`neoforge.mods.toml` as required (or optional if using the compat-checker route,
see below).

## Integration

1. **Plugin class** implementing `com.tom.cpm.api.ICPMPlugin`:
   - `getOwnerModId()` → `"exphardcore"`
   - `initCommon(ICommonAPI api)` → store the api instance (this is the server-relevant one)
   - `initClient(IClientAPI api)` → store if client features are ever needed
2. **Registration per loader**:
   - Fabric: entrypoint in `fabric.mod.json`: `"entrypoints": { "cpmapi": ["...CpmCompat"] }`
   - NeoForge (0.6.26+): annotate the plugin class with `@CPMPlugin` (no IMC needed)
3. **Applying a model per life** (server side, e.g. in `LifeHandler.newLife` /
   `refreshExpeditionData`):

   ```java
   api.setPlayerModel(ServerPlayer.class, player, base64Model, /*forced*/ true, /*persistent*/ true);
   // and on life end / feature disable:
   api.resetPlayerModel(ServerPlayer.class, player);
   ```

   - `forced=true` prevents players from overriding via the CPM UI.
   - `persistent=true` keeps the model across relogs.
   - `base64Model` strings come from CPM's "Export as Base64" (documented as intended for
     programmatic/server use). Store them per model in mod data (they are plain strings, so they
     also travel trivially over Apibalego datasync).
4. **Optional-dependency variant**: gate everything behind the existing `ModCompatChecker` pattern
   (like Apibalego) so the mod still runs without CPM — plain vanilla skins as fallback. The plugin
   class must then only be classloaded when CPM is present (fabric entrypoint handles this
   naturally; on neoforge the `@CPMPlugin` annotation is only scanned by CPM itself, also fine).

## Model pipeline

1. Install the CPM Blockbench plugin (in the CPM repo, `Blockbench/` folder) or use the in-game
   editor (`/cpm` → editor).
2. Import/rebuild each character model on the player template (standard rig; extra parts like the
   cydonia bun/hat attach to the head bone).
3. Export → "Base64" → paste the string into the mod's model registry (assets/config/datasync).
4. Optional: author custom animations/gestures in the editor; trigger from code via
   `ICommonAPI.playAnimation(Player.class, player, name, value)`.

## Docs / references

- API doc: CPM wiki `API-documentation.md` (github.com/tom5454/CustomPlayerModels/wiki)
- API sources: `CustomPlayerModels/src/shared/java/com/tom/cpm/api/` (`ICommonAPI`, `IClientAPI`,
  `ICPMPlugin`)
- Modrinth: https://modrinth.com/mod/custom-player-models (26.2 builds: `26.2v0.6.27a-{fabric,neoforge}`)
