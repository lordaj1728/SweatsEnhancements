# SweatsEnhancements

Independent client-side quality-of-life mod for **Minecraft 1.8.9 Forge**. Java 8 is the target runtime. This project is not an official server mod.


## Download and install

Download **[SweatsEnhancements-0.0.1.jar](https://github.com/lordaj1728/SweatsEnhancements/releases/download/v0.0.1/SweatsEnhancements-0.0.1.jar)** from the [latest release](https://github.com/lordaj1728/SweatsEnhancements/releases/latest). Place it in your Minecraft 1.8.9 Forge instance's `mods` folder, then restart the game. **Remove the previous JAR** before installing; do not run both versions together.

Open the menu with **P** or `/se menu` (also `/sweatsenhancements menu`). The HUD editor defaults to **H**. Configure feature shortcuts in the mod's config menu.

## What's included

- Configurable HUD, scoreboard and tab-list presentation, including a received dungeon-map HUD.
- Dungeon and Slayer visual and information features, plus independent quality-of-life options.
- Optional Item Safeguard: UUID-bearing items are protected by UUID; items without a UUID use item type, metadata and NBT, so identical copies share a protection rule. Set its key in the mod's menu or use `/se safeguard` while holding an item.
- Features can be configured independently. See the in-game menu for available switches and their descriptions.

The custom map/room detection and puzzle features from earlier private development are **not included** in this release. Neither are externally sourced pet portraits. This is a manual-assistance mod; it does not ship automated puzzle solving or click queues.

## Updating from the previous mod

Back up your Minecraft `config` folder first. On first launch, SweatsEnhancements copies the old settings file, protected-item rules, and chat filters from `config/fpe/` into `config/sweatsenhancements/` **only when the new destination files are absent**. The old files are not deleted. If you already have new files, the migration does not overwrite them.

## Build from source

The [matching 0.0.1 source ZIP](https://github.com/lordaj1728/SweatsEnhancements/releases/download/v0.0.1/SweatsEnhancements-0.0.1-source.zip) contains the build scripts and required source. On **Linux x64**, from the source directory:

```bash
bash build.sh build
```

The script downloads its Java 8 and Gradle 6.9.4 toolchain on first run; dependencies require internet access. The installable JAR is written to `build/libs/`. On Windows, use a Linux x64 environment such as WSL2; the supplied build script does not download a native Windows JDK. See `SOURCE-REBUILD-NOTICE.md` for scope and provenance notes.

## Issues and credits

Found a problem? [Open an issue](https://github.com/lordaj1728/SweatsEnhancements/issues) with your Minecraft/Forge/Java versions, relevant settings, steps to reproduce and any non-sensitive logs. **Remove account details, chat content and server addresses from logs before posting them publicly.**

The mod includes adapted third-party components with their own attribution and licensing terms. Read `THIRD-PARTY-SKY-VELOR-NOTICE.txt`, `src/main/resources/THIRD-PARTY-NEF-NOTICE.txt`, `src/main/resources/THIRD-PARTY-NEU-NOTICE.txt`, the accompanying license files, and `SOURCE-REBUILD-NOTICE.md`. No blanket license claim is made here for the entire repository.
