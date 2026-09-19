# ServerSwitch

ServerSwitch is a server-side Fabric mod that turns one Minecraft server into a configurable virtual server network with a central HUB, isolated server/world groups, player-state separation, server switching, server locking/hiding, direct join, recording mode, and administrator management.

> Development target: Minecraft 1.20.1, Fabric, Java 17.

## Project status

Early development. The repository starts with the architecture and implementation blueprint before feature code is added.

## Goals

- Provide a central HUB.
- Allow players to switch virtual servers with `/server <name>`.
- Provide `/hub`.
- Allow administrators to create, configure, rename, enable/disable, lock/unlock, hide/unhide, and order virtual servers.
- Allow each virtual server to have its own Overworld, Nether, End, spawn, rules, player inventory/state, tags, effects, permissions, and other scoped data.
- Provide a GUI server selector with protected clickable items.
- Support optional NPC integration without making NPCs a core dependency.
- Support direct-join and recording modes.
- Keep global data separate from server-scoped data.
- Prefer persistent, crash-safe configuration and player-state storage.

## Player commands

- `/server` — show servers visible and available to the player.
- `/server <server>` — switch to a virtual server.
- `/hub` — return to the HUB.
- `/server help` — show player-facing command help.
- `/server version` — show the mod version; restricted by permission when configured.

Players must never gain access to administrative commands merely because a server exists.

## Admin / operator commands

The administrative command tree is rooted at `/serverswitch`.

Core groups:

- `/serverswitch server create <id>`
- `/serverswitch server delete <id> confirm`
- `/serverswitch server rename <id> <display-name>`
- `/serverswitch server enable <id>`
- `/serverswitch server disable <id>`
- `/serverswitch server lock <id>`
- `/serverswitch server unlock <id>`
- `/serverswitch server hide <id>`
- `/serverswitch server unhide <id>`
- `/serverswitch server order <id> <number>`
- `/serverswitch server info <id>`
- `/serverswitch server list`
- `/serverswitch server icon <id>`
- `/serverswitch world set <id> overworld <world>`
- `/serverswitch world set <id> nether <world>`
- `/serverswitch world set <id> end <world>`
- `/serverswitch spawn set <id>`
- `/serverswitch spawn info <id>`
- `/serverswitch hub set`
- `/serverswitch hub border <blocks>`
- `/serverswitch hub protect`
- `/serverswitch directjoin <id>`
- `/serverswitch directjoin off`
- `/serverswitch directjoin status`
- `/serverswitch recording start <id>`
- `/serverswitch recording stop`
- `/serverswitch recording status`
- `/serverswitch gui`
- `/serverswitch status`
- `/serverswitch reload`
- `/serverswitch setup`

The exact aliases may evolve during implementation, but public player commands and administrative commands remain deliberately separated.

## Virtual server model

A virtual server is not a second JVM or independent Minecraft process. It is a managed server profile inside one Fabric server process.

Example:

```text
ServerSwitch
├── HUB
│   └── hub
├── Kids
│   ├── kids_overworld
│   ├── kids_nether
│   └── kids_end
└── Girls
    ├── girls_overworld
    ├── girls_nether
    └── girls_end
```

Virtual servers must be isolated at the application/data layer even though they share the same Minecraft process.

## Data isolation

### Global data

- UUID
- network-level bans/mutes where implemented
- global preferences
- other deliberately global metadata

### Server-scoped data

- inventory
- armor/offhand
- XP and levels
- health/hunger state where appropriate
- active effects
- position and rotation
- gamemode where configured
- server-scoped tags
- server-scoped permissions
- server-scoped statistics/settings

A switch must save the current scoped state, change the world/realm, clear or replace scoped state safely, and load the target scoped state.

## HUB

The HUB is a protected world/profile.

Default HUB rules:

- no block breaking
- no block placing
- no PvP
- no normal entity damage
- no item dropping
- no item pickup
- no unwanted inventory manipulation
- no hunger loss
- no hostile or passive mob spawning
- permanent daytime
- clear weather
- no fire/explosion abuse
- configurable interaction allowlist for selector items/NPCs
- configurable world border / protected area

The selector inventory is navigation UI, not transferable player inventory. Selector items cannot be dropped, moved into containers, traded, crafted, or otherwise converted into normal items.

## Server lifecycle

Each virtual server can be:

- ONLINE — visible and joinable according to permissions.
- LOCKED — optionally visible but unavailable to normal players.
- HIDDEN — absent from normal selectors.
- DISABLED — administratively disabled.
- MAINTENANCE — reserved state for future maintenance handling.

Administrative bypass must be permission-based.

## Direct Join and Recording Mode

Direct join sends players to a selected server instead of the HUB.

Recording mode is a convenience layer that can:

1. select the recording target;
2. lock other public servers;
3. optionally hide other servers;
4. enable direct join to the target;
5. restore previous server states when recording ends.

State restoration must be persisted safely so a restart does not silently corrupt the intended network configuration.

## Configuration

Configuration must be externalized and editable without changing source code.

Planned layout:

```text
config/serverswitch/
├── config.json
├── servers.json
├── gui.json
└── permissions.json
```

Virtual-server world folders belong to Minecraft's world storage layout rather than being treated as arbitrary configuration text files.

Configuration validation is required. Missing worlds, invalid IDs, duplicate IDs, malformed values, or unsafe references must produce an actionable error instead of crashing the server.

## Persistence and crash safety

Player-state switches are high-risk operations. The implementation must:

- save scoped state before switching;
- avoid partial writes where practical;
- recover deterministically after a crash;
- prevent item duplication/loss;
- avoid applying one server's effects/tags/permissions to another;
- preserve last known valid configuration.

## Permissions

Permissions must distinguish public actions, administrative actions, and optional bypass actions.

Planned nodes include:

- `serverswitch.admin`
- `serverswitch.server.create`
- `serverswitch.server.delete`
- `serverswitch.server.edit`
- `serverswitch.server.lock`
- `serverswitch.server.hide`
- `serverswitch.server.world`
- `serverswitch.server.spawn`
- `serverswitch.hub.manage`
- `serverswitch.recording`
- `serverswitch.directjoin`
- `serverswitch.bypass.lock`
- `serverswitch.version`

## Architecture

Planned modules:

```text
ServerSwitch
├── Core
├── ServerManager
├── RealmManager
├── PlayerDataManager
├── RealmSessionManager
├── TeleportManager
├── PermissionManager
├── CommandManager
├── GuiManager
├── NPC integration
├── HubManager
├── ProtectionManager
├── RecordingManager
├── ConfigurationManager
├── StorageManager
└── EventManager
```

The command, GUI and NPC layers must all call the same server-management backend.

## Non-goals

- Creating separate Minecraft server processes.
- Replacing Velocity/Bungee-style proxy networking.
- Making NPCs mandatory.
- Hard-coding a fixed list of servers.
- Storing important state only in RAM.
- Making server creation require source-code changes.

## Implementation order

1. Fabric project/build metadata.
2. Core configuration and persistent registry.
3. Public and admin command registration.
4. HUB registration/spawn/protection.
5. Virtual-server creation/editing.
6. World assignment and safe switching.
7. Server-scoped player data.
8. GUI selector.
9. Lock/hide/enable/disable.
10. Direct join.
11. Recording mode.
12. Optional NPC integration.
13. Validation, crash recovery, tests, and documentation.

## Compatibility policy

The first stable target is Minecraft 1.20.1 on Fabric with Java 17. Multi-version support must not be promised until each target version has been tested. API/mapping changes should be isolated where possible so later ports do not rewrite the whole architecture.

## Author

Created by Ahsan3944.

Project repository: https://github.com/Ahsan3944/ServerSwitch
