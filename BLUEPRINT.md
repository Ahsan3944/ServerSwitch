# ServerSwitch — A-to-Z Implementation Blueprint

## 1. Product definition

ServerSwitch is a server-side Fabric mod that creates a virtual multi-server experience inside one Minecraft server process.

Players use simple public commands such as `/server` and `/hub`. Operators use a separate administrative command namespace to create and configure virtual servers without editing source code.

## 2. Fixed concepts

### HUB
The central landing area.

### Virtual Server
A named logical server profile containing its world group, spawn, visibility state, access state, GUI entry, and scoped player state.

### World Group
Three optional world references per virtual server:

- Overworld
- Nether
- End

The first implementation must validate these references and keep dimension transitions inside the same virtual server.

### Server Scope
Everything that belongs to one virtual server and must not leak into another.

## 3. Public experience

### Join
Normal default:
`player joins -> HUB`

When direct join is enabled:
`player joins -> configured target server`

### Commands

```text
/server
/server <id>
/server help
/server version
/hub
```

Unknown, hidden, disabled, or locked targets must return a clear message without exposing unnecessary administrative state.

### Selector GUI

The HUB selector contains one protected item per visible server.

Each item should display:

- display name
- status
- player count if available
- concise description
- click-to-join instruction

Items are not transferable inventory.

## 4. Administrator experience

All administrative operations are rooted at `/serverswitch`.

### Server creation

```text
/serverswitch server create kids
```

Creates registry metadata and a predictable server directory/config entry. World data should only be created/copied when explicitly implemented and safe to do so; creating a profile must not silently overwrite an existing Minecraft world.

### Server editing

Admin can change:

- internal display name
- enabled state
- locked state
- hidden state
- GUI order
- icon/selector item
- description
- world mapping
- spawn
- selected per-server rules

### Server deletion

Deletion requires confirmation.

The implementation must clearly distinguish:

1. deleting the ServerSwitch profile;
2. deleting world data.

World deletion is destructive and must never happen implicitly.

### World assignment

```text
/serverswitch world set kids overworld kids_world
/serverswitch world set kids nether kids_nether
/serverswitch world set kids end kids_end
```

The implementation validates that the referenced world exists and is compatible with the intended dimension.

### Spawn

```text
/serverswitch spawn set kids
```

Stores world, x, y, z, yaw and pitch.

```text
/serverswitch hub set
```

Stores the HUB world and spawn.

## 5. HUB protection design

HUB restrictions are implemented at event/interaction boundaries rather than relying only on one external protection mod.

At minimum:

- block break denied
- block place denied
- PvP denied
- entity damage denied where appropriate
- item drop denied
- item pickup denied
- container access denied unless explicitly allowlisted
- inventory movement restrictions for selector items
- mob spawning suppressed
- time locked to day
- weather controlled
- explosions/fire restricted
- hunger preserved
- configurable border

The protection layer must avoid interfering with administrative actions.

## 6. Server state machine

Each server has independent flags:

```text
enabled
locked
hidden
maintenance
display order
direct-join eligibility
```

State evaluation order should be deterministic.

Suggested join eligibility:

1. server exists;
2. server enabled;
3. server is not hidden unless the player has bypass permission;
4. server is not locked unless the player has bypass permission;
5. all required world references are valid;
6. destination is available;
7. perform switch.

## 7. Player data isolation

The key invariant is:

```text
global_player_data
!=
realm_player_data
```

For each player and virtual server, persist only the fields required for safe restoration.

Minimum scoped data:

- main inventory
- armor
- offhand
- XP/level
- health
- hunger/saturation where required
- active potion effects
- fire/freeze/fall state where relevant
- gamemode if server-scoped
- position/rotation
- server-scoped tags
- server-scoped permissions
- optional server-specific statistics

Never serialize volatile references such as live entity objects.

## 8. Session switching algorithm

```text
request switch
  -> validate target
  -> prevent concurrent switch for same player
  -> save current scope
  -> mark transition state
  -> remove/replace scope-specific effects
  -> transition to target world
  -> load target scope
  -> apply target inventory/effects/permissions/tags
  -> teleport to target spawn or saved position
  -> clear transition state
  -> persist completion
```

Failure path:

```text
failure during switch
  -> do not apply incomplete target state
  -> restore last valid source/known-safe state
  -> log detailed diagnostic
  -> inform player with safe message
```

The implementation should use an atomic or transactional approach for persisted state as far as practical.

## 9. Nether and End isolation

Each virtual server must have explicit dimension mappings.

Example:

```text
kids:
  overworld = kids_world
  nether    = kids_nether
  end       = kids_end
```

Dimension travel must never resolve a Kids portal destination to Girls or another server.

The implementation must test:

- Overworld -> Nether
- Nether -> Overworld
- Overworld -> End
- End -> Overworld
- logout/login in each dimension
- switch server while in Nether
- switch server while in End

## 10. Global vs scoped permissions

The permission architecture must support both:

### Global
Examples: network administration and network bans.

### Scoped
Examples:

```text
server=kids permission=moderator
server=girls permission=none
```

A scoped permission must not automatically become a global permission.

## 11. Recording mode

```text
/serverswitch recording start kids
```

Captures the current relevant network state before applying the recording policy.

Expected behavior:

- target server remains accessible;
- other normal servers may be locked;
- other normal servers may be hidden;
- direct join points to target;
- selector shows only the allowed target for normal players.

```text
/serverswitch recording stop
```

Restores the captured state.

If the server restarts during recording, persisted recording state must be enough to recover safely.

## 12. Direct join

```text
/serverswitch directjoin kids
```

Affects new joins and defines one global default target.

Optional future extension: permission-based or player/group-based direct join.

## 13. GUI architecture

The GUI should be generated from the ServerManager registry.

Never duplicate server definitions inside GUI code.

```text
ServerRegistry
     ↓
ServerSelectorBuilder
     ↓
Inventory GUI
```

Clickable actions must revalidate the server state at click time so a server locked after the GUI opened cannot still be entered.

## 14. NPC architecture

NPCs are optional adapters.

```text
ServerManager
   ↑
NPC adapter
```

NPC click should call the same switch operation as the command and GUI.

No core ServerSwitch feature may depend on a specific NPC mod.

## 15. Configuration model

Recommended top-level configuration:

```text
config/serverswitch/
├── config.json
├── servers.json
├── gui.json
└── permissions.json
```

Server records should contain:

- id
- displayName
- description
- enabled
- locked
- hidden
- order
- selector item
- world mappings
- spawn
- per-server rules
- optional metadata

Do not store Minecraft world folders inside JSON.

## 16. External management

The requirement “admin can configure servers without entering Minecraft” means the configuration files must be readable and editable from the server filesystem.

The mod must:

- discover server records at startup;
- validate them;
- create missing default files/directories;
- preserve unknown fields where practical;
- write changes safely;
- expose `reload` for non-destructive config updates;
- refuse unsafe reloads when active operations make the change invalid.

A later web/API control panel can be added without changing the ServerManager contract.

## 17. Storage choice

Use persistent server-side storage for authoritative state.

Recommended separation:

- JSON/YAML configuration for human-editable network configuration;
- persistent binary/NBT-like or structured storage for runtime/player-scoped data where that provides safer Minecraft integration.

Implementation choice must be based on the exact 1.20.1 Fabric API and mapping APIs available at coding time.

## 18. Logging and diagnostics

Log categories:

- CONFIG
- SERVER
- SWITCH
- PLAYER_DATA
- HUB
- RECORDING
- ERROR

Every failed switch should include enough context to reproduce the issue without leaking unnecessary player data.

## 19. Safety rules

Never:

- silently delete worlds;
- silently overwrite world assignments;
- copy inventories between servers;
- trust GUI state without revalidation;
- rely only on OP level when a permission check is intended;
- perform large blocking disk work on the main server tick when avoidable;
- assume every world is loaded;
- assume another mod will protect the HUB.

## 20. Testing matrix

### Commands
- create
- rename
- enable/disable
- lock/unlock
- hide/unhide
- list/info
- spawn/world/hub set
- direct join
- recording
- reload
- version/help

### Players
- normal player
- operator
- admin with bypass
- multiple simultaneous players

### Switching
- Hub -> Kids
- Kids -> Girls
- Girls -> Hub
- Kids Nether -> Girls
- Kids End -> Girls
- reconnect after each state

### Data
- inventory
- armor
- offhand
- XP
- effects
- tags
- permissions
- position

### Failure
- invalid world
- deleted world
- malformed config
- duplicate ID
- switch during another switch request
- server restart during recording
- disconnect during switch

## 21. Performance

The mod should not scan every world or every player every tick unnecessarily.

Prefer:

- event-driven hooks;
- cached server registry;
- per-player transition guards;
- batched/safe persistence;
- lazy GUI generation;
- bounded logging.

## 22. Versioning

Mod version follows semantic-style releases where practical:

```text
0.x = active development
1.0.0 = first stable implementation
```

Minecraft compatibility is explicitly listed per release.

## 23. Development phases

### Phase 0
Repository and build bootstrap.

### Phase 1
Core config, registry, commands, permissions.

### Phase 2
HUB setup and protection.

### Phase 3
Virtual server creation/editing and world mapping.

### Phase 4
Safe realm switching.

### Phase 5
Scoped player data.

### Phase 6
GUI selector.

### Phase 7
Lock/hide/enable/disable.

### Phase 8
Direct join and recording.

### Phase 9
Optional NPC adapter.

### Phase 10
Recovery, tests, documentation, release packaging.

## 24. Acceptance criteria

The implementation is considered functionally complete only when:

- a new virtual server can be created without source changes;
- its name/icon/spawn/worlds can be configured;
- public players can see and join permitted servers;
- HUB rules are enforced;
- server-scoped player state does not leak;
- Nether/End remain scoped;
- administrators can lock/hide/direct players;
- recording mode can be started/stopped safely;
- configuration survives restart;
- malformed configuration fails safely;
- the build produces a usable Fabric server-side mod JAR for the target version.

## 25. Out-of-scope until the core is stable

- proxy-style multi-process networking
- web dashboard
- cross-version support
- cross-server economy sync
- cross-server chat
- external database
- mandatory NPC framework
- automatic destructive world provisioning

These can be added later without weakening the core abstraction.
