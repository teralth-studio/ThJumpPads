# Minecraft and Java compatibility

The release workflow builds separate Spigot and Paper JARs. Each directory is named for the first Minecraft version in its compatibility range:

```text
distribution/
  1.8/
    paper/
    spigot/
  1.12/
    paper/
  1.16.5/
    paper/
  1.17/
    paper/
    spigot/
  1.18/
    spigot/
  1.20/
    paper/
  1.20.5/
    spigot/
  26.1/
    paper/
    spigot/
```

Each JAR is compiled to the Java class-file level for its server range and against the earliest API version in that range. A server running a newer Java runtime can run an older-bytecode JAR; the reverse is not supported. The Paper Java levels below follow Paper's published recommendations, while the Spigot levels follow its published BuildTools guidance.

JAR filenames include the plugin release, platform, Minecraft range, and Java bytecode target, for example `ThJumpPads-1.0.2-paper-MC1.12-1.16.4-Java11.jar`.

## Spigot

| Directory | Minecraft Java Edition | Server Java |
|---|---|---:|
| `1.8/spigot` | 1.8.8–1.16.5 | 8 |
| `1.17/spigot` | 1.17.x | 16 |
| `1.18/spigot` | 1.18.x–1.20.4 | 17 |
| `1.20.5/spigot` | 1.20.5–1.21.11 | 21 |
| `26.1/spigot` | 26.1–26.3 | 25 |

Spigot's BuildTools page documents Java 8 below 1.17, Java 16 for 1.17, Java 17 after 1.17.1, and Java 21 after 1.20.5. Its Java guidance does not yet list 26.x; Java 25 for 26.1–26.3 follows Paper's current guidance and the Minecraft server runtime requirement.

## Paper

| Directory | Minecraft Java Edition | Server Java |
|---|---|---:|
| `1.8/paper` | 1.8.8–1.11.x | 8 |
| `1.12/paper` | 1.12.x–1.16.4 | 11 |
| `1.16.5/paper` | 1.16.5 | 16 |
| `1.17/paper` | 1.17.x–1.19.x | 17 |
| `1.20/paper` | 1.20.x–1.21.11 | 21 |
| `26.1/paper` | 26.1–26.3 | 25 |

Paper builds use Spigot's Bukkit API where Paper does not publish an API artifact for the older range, and for 26.x where the available Paper API artifact is still pre-release. Paper builds for 1.17–1.21 use Paper API.

On pushes to `main` and manual workflow runs, GitHub Actions publishes one categorized `ThJumpPads-compatible-jars` artifact containing all 11 JARs. Tagged releases attach the individual JARs and a ZIP preserving this directory structure.

Storage drivers are not treated as server-provided APIs. At runtime, ThJumpPads downloads only the configured storage backend's libraries into `plugins/ThJumpPads/lib`, verifies their SHA-256 digests, and loads them in an isolated class loader. This keeps the plugin independent of incidental libraries bundled by a particular server build.

## Sources

- [Paper: Getting started and recommended Java versions](https://docs.papermc.io/paper/getting-started/)
- [Spigot: BuildTools prerequisites](https://www.spigotmc.org/wiki/buildtools/#prerequisites) (the page notes that some information can be outdated)
- [Spigot API snapshot metadata](https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/maven-metadata.xml)
- [Minecraft Java Edition version history](https://minecraft.wiki/w/Java_Edition_version_history)
