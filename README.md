# Chairs

**[日本語版 README](README_ja.md)**

A Spigot/Paper plugin that allows players to sit on stairs and configured blocks with click or command actions.

## Overview
Chairs adds lightweight, version-tolerant sitting behavior for Minecraft servers.
It supports both right-click sitting and command-driven sitting while preserving the existing user experience.
Compatibility and operational stability are prioritized across old and modern Paper versions.

### Target Users
- Server operators running survival, lobby, or RPG-style servers.
- Teams that want configurable chair behavior with minimal overhead.

## Features
- Right-click sitting and command-based sitting.
- `/chair` to seat yourself, `/chair <player>` to force-seat another player (permission required).
- Runtime block control via `/chairs block disable|enable <MATERIAL|PATTERN>`.
- Pattern support such as `*_STAIRS`.
- Per-world sit disable settings.
- Sit-time restrictions and addons (command restrict/healing/item pickup).
- Enhanced error logging with optional stack traces.

## Requirements
- Java 8 or later (plugin runtime target)
- Spigot or Paper 1.15.2 or later
- Gradle 8.14.3 (build)

**Tested versions (real server startup):**

| Minecraft | Server | JDK |
|---|---|---|
| 1.15.2 | Paper 1.15.2 (build 393) | JDK 11.0.31 |
| 1.16.5 | Paper 1.16.5 (build 794) | JDK 11.0.31 |
| 1.21.11 | Paper 1.21.11 (build 69) | JDK 21.0.11 |
| 26.1.2 | Paper 26.1.2 (build 72) | JDK 25.0.3 |

Code-level compatibility target: 1.15.2 – 26.1.2

## Status
- Last README update: 2026-06-30
- Stability policy: keep plugin operation alive, log root causes in detail
- Distribution note: this release is published as the Cyrne1_7208 version

## Installation
1. Build the plugin jar.
2. Copy the jar into your server `plugins/` directory.
3. Restart the server.

```bash
./gradlew.bat clean build
```

Output: `target/Chairs.jar`

## Quick Start
1. Place `target/Chairs.jar` in `plugins/`.
2. Start or restart the server.
3. Confirm `[Chairs] Enabling Chairs v1.2.0` in logs.
4. Right-click stairs or run `/chair` in-game.

Expected output:

```text
Chairs is enabled in the server log and players can sit normally.
```

## Usage

### Commands
| Command | Permission | Description |
|---|---|---|
| `/chairs reload` | `chairs.reload` | Reload plugin configuration |
| `/chairs on` | `chairs.sit` | Enable your sit feature |
| `/chairs off` | `chairs.sit` | Disable your sit feature |
| `/chairs block list` | `chairs.block.edit` | Show disabled block IDs/patterns |
| `/chairs block disable <MATERIAL\|PATTERN>` | `chairs.block.edit` | Disable block ID/pattern |
| `/chairs block enable <MATERIAL\|PATTERN>` | `chairs.block.edit` | Re-enable block ID/pattern |
| `/chair` | `chairs.sit` | Seat yourself |
| `/chair <player>` | `chairs.sit.force` | Force-seat a player |

Block pattern examples:

```text
/chairs block disable WARPED_STAIRS
/chairs block disable *_STAIRS
/chairs block enable *_STAIRS
```

`MATERIAL|PATTERN` is case-insensitive (`*_stairs` also works).

## Configuration
Main `config.yml` areas:

| Section | Key | Purpose |
|---|---|---|
| `sit-config` | `disabled-worlds` | Worlds where sitting is disabled |
| `sit-config` | `max-distance` | Max sit interaction distance |
| `sit-config.stairs` | `enabled` | Stair sitting on/off |
| `sit-config.additional-blocks` | `MATERIAL` or `*_PATTERN : number` | Additional sit blocks and offsets |
| `sit-config.disabled-blocks` | `MATERIAL` / `*_PATTERN` | Excluded sit blocks |
| `sit-effects.healing` | `enabled` etc. | Sit-time healing settings |
| `sit-effects.itempickup` | `enabled` | Sit-time item pickup |
| `sit-restrictions.commands` | `all`, `list` | Command restriction while sitting |

## Stability Notes
This version strengthens production safety through:
- Exception capture for command execution paths.
- Exception logging for sit/re-sit/unsit flows.
- Per-player exception isolation in effect tasks.
- Validation for invalid material IDs and patterns at config load.

Representative log tags:

```text
[CMD]
[SIT-START]
[SIT-RESIT]
[SIT-UNSIT]
[CFG-SAVE]
[EFFECT-HEAL]
[EFFECT-PICKUP]
```

## Testing
Manual real-server tests were executed with Paper 1.15.2 / 1.16.5 / 1.21.11 / 26.1.2.
For each target, plugin load (`Enabling Chairs`), command execution, and clean startup were verified.

## Troubleshooting
- `InvalidDescriptionException: commands are of wrong type`
  - Check indentation under `commands` in `plugin.yml`.
- `Minecraft 1.19 requires Java 17 or above`
  - Use Java 17+ for modern server lines (including 1.21.x).
- `Invalid or corrupt jarfile paper.jar`
  - Re-download server jar from official Paper source.

## Acknowledgements
- Original contributors: spoothie, cnaude, _Shevchik_, Pugabyte

## AI Usage
- AI used: GPT-5.3-Codex, GLM-4.6, MiMo-V2.5
- Usage scope: Code review, bug fixing
- Human review: commands/config/runtime behavior checked against source and startup logs

## Contributing
Issues and pull requests are welcome.
For behavior changes, please update README and configuration explanations together.

## License
GPL-3.0. See `LICENSE`.

