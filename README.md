![Banner](images/banner.png)

# Installation

1. Download the jar.
2. Place the jar in the `plugins` folder.
3. Reload the server.

All jump pad and player toggle features are available to everyone. The only permission is
`thjumppads.reload`, required for `/jumppads reload` (operators by default).

# Dependencies

#### Soft dependencies:
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/): Used for placeholders in messages and commands. Also to register placeholders for the plugin.

# Commands and permissions
| Command | Description | Permission |
|---|---|---|
| `/thjumppads toggle` | Enable or disable the jump pads use | `-` |
| `/thjumppads enable` | Enable your jump pads | `-` |
| `/thjumppads disable` | Disable your jump pads | `-` |
| `/thjumppads status` | Show your status | `-` |
| `/thjumppads reload` | Reload config and messages | `thjumppads.reload`* |

*Enabled for OPs by default

# Plugin files

<details>
<summary>config.yml</summary>

```yaml
# ==========================================
# ThJumpPads - Configuration File
# ==========================================

# The block that players must stand on to activate the jump pad.
jump-block: "IRON_PLATE"

# If true, there must be a block below the jump pad block for it to work.
requires-block-below: false

# The block that must be below the jump pad block for it to work. Only used if
# requires-block-below is set to true.
block-below: "IRON_BLOCK"

# The vertical distance the player will be launched, in blocks.
jump-height: 5

# The horizontal distance the player will be launched, in blocks.
jump-width: 5

# Worlds where the jump pads are enabled.
# (Leave empty to enable in all worlds)
enabled-worlds: []

# Database for storing player toggles.
# Options on "type" must be on of the following options: sqlite, mysql, mongodb
storage:
  type: sqlite # sqlite, mysql o mongodb
  sqlite:
    file: data.db
  mysql:
    host: localhost
    port: 3306
    database: televeling
    username: root
    password: ""
    use-ssl: false
    verify-server-certificate: true
    allow-public-key-retrieval: false
    connection-timeout: 10000
  mongodb:
    uri: "mongodb://localhost:27017"
    database: jumppads
    collection: player_toggles

# Sound settings when a player uses a jump pad.
sound:
  enabled: true
  name: "LEVEL_UP"
  volume: 1.0
  pitch: 1.2
```

</details>

<details>
<summary>messages.yml</summary>

</details>



# Plugin Metrics

[![bStats](images/bstats-charts.svg)](https://bstats.org/plugin/bukkit/ThJumpPads/35094)

