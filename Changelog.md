# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project follows the [Ragnarök Versioning Convention](https://github.com/Red-Studio-Ragnarok/Commons/blob/main/Ragnar%C3%B6k%20Versioning%20Convention.md).

## [UNRELEASED] Alfheim Version 1.6 Changelog

### Added

- Alfheim will now attempt to hijack the mixins of Phosphor/Hesperus if it is installed and log a warning

### Changed

- Now requires MixinBooter 10.5 and up

### Internal

- Updated to [MixinBooter](https://github.com/CleanroomMC/MixinBooter) 10.5
- Removed the `comformVisibility` overwrite in `mixins.alfheim.json` as it's no longer require with MixinBooter 9.4 and above
- Only generate sources jar for releases
- Switched from Javadoc to Markdowndoc
- Updated to [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) 8.12.1
- Updated to [org.jetbrains.gradle.plugin.idea-ext](https://github.com/JetBrains/gradle-idea-ext-plugin) 1.1.10
- Updated to [Gradle](https://gradle.org) 8.13-rc-2
- Updated to [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 1.4.3

## Alfheim Version 1.5 Changelog - 2025-02-11

### Changed

- `DeduplicatedLongQueue` now creates a new deduplication set instead of clearing it
- Improved client light processing performance by using a `DeduplicatedLongQueue` instead of a `Set` of `BlockPos` objects
- Updated dependencies:
  - Requires Red Core 0.6 and up

### Fixed

- Fixed client lighting not updating past render chunk boundaries [#40](https://github.com/Red-Studio-Ragnarok/Alfheim/issues/40)

### Internal
  
- Updated to [org.jetbrains.gradle.plugin.idea-ext](https://github.com/JetBrains/gradle-idea-ext-plugin) 1.1.9
- Updated to [MixinBooter](https://github.com/CleanroomMC/MixinBooter) 10.2
- Updated to [gradle-buildconfig-plugin](https://github.com/gmazzo/gradle-buildconfig-plugin) 5.5.1
- Updated to [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) 8.11
- Updated to [foojay-resolver](https://github.com/gradle/foojay-toolchains) 0.9.0
- Updated to [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 1.4.2
- Reworked buildscript
- Updated [Red Core](https://www.curseforge.com/minecraft/mc-mods/red-core) dependency to 0.6

## Alfheim Version 1.4 Changelog - 2024-08-21

### Changed

- Overwrites now do not enforce the scope of methods to prevent crashes, using `conformVisibility` option instead
- Updated dependencies:
  - Requires MixinBooter 8.8 and up
- Simplified Cubic Chunks detection logic

### Fixed

- Fixed version checking
- Fixed Alfheim not loading on dedicated servers

### Removed

- Removed line asking to report a threading issue which is caused by other mods

### Internal

- Updated to [gradle-buildconfig-plugin](https://github.com/gmazzo/gradle-buildconfig-plugin) 5.4.0
- Updated to [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) 8.7.1
- Remade the build script
- Switched to the new standard `gradle.properties`
- Updated to [Gradle](https://gradle.org) 8.8
- Updated to [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 1.4.1
- Updated to [MixinBooter](https://github.com/CleanroomMC/MixinBooter) 9.0
- Set a minimum Gradle Daemon JVM version requirement
- General cleanup

## Alfheim Version 1.3 Changelog - 2024-03-28

### Changed

- Queues are now deduplicated, meaning that the lighting engine won't update a position multiple times in a row, improving performance

### Fixed

- Fixed compatibility with Vintagium (Thanks to [Asek3](https://github.com/Asek3) in [#41](https://github.com/Red-Studio-Ragnarok/Alfheim/pull/41))
- Fixed `ArrayIndexOutOfBoundsException` crashes

### Internal

- Cleaned up the buildscript
- Now uses the Red Studio maven for Red Core
- Updated [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) to version 1.3.34
- Updated [foojay-resolver](https://github.com/gradle/foojay-toolchains) to version 0.8.0
- Updated [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) to version 8.6
- Updated [org.jetbrains.gradle.plugin.idea-ext](https://plugins.gradle.org/plugin/org.jetbrains.gradle.plugin.idea-ext) to version 1.1.8

## Alfheim Version 1.2 Changelog - 2023-12-11

### Changed

- Stopped using `PooledLongQueue` using `LongArrayFIFOQueue` instead, should be more optimized (faster & lighter lighting updates)
- Skip spreading light neighbor checks early if the current light is lower than the neighbor light
- Made minor changes to clamping (Shouldn't cause a difference)

### Internal

- Updated RFG
- Updated Gradle
- Fixed the names of the arrays of queues
- Cleaned up `LightingEngine`

## Alfheim Version 1.1.1 Changelog - 2023-11-19

### Fixed

- Fixed log spam

## Alfheim Version 1.1 Changelog - 2023-11-18

#### Now depends on Red Core 0.5
#### Now depends on MixinBooter 8.6

### Changed

- Lighting engine will now schedule updates no matter if that chunk is loaded, which is different from vanilla but potentially fix areas lighting being weird when going far away
- Made the logic for capping the client updates per frame dumber which should fix performance drops on lower end hardware (Fixes [#20](https://github.com/Red-Studio-Ragnarok/Alfheim/issues/20))
- Made all head-cancels overwrite instead
- All overwrites now make the scope of the overwritten methods `public` to prevent access level conflicts at runtime
- Updated the in game description to the new short description
- Updated to Red Core 0.5

### Fixed

- Fixed a crash at start on Mohist (Probably any Bukkit implementation)
- Fixed skins not working with Alfheim paired with some other mod (we don't know which)
- Fixed unescaped unicode characters in the description

### Internal

- Switched to [CurseUpdate](https://forge.curseupdate.com/) for update checking
- Switched to [gradle-buildconfig-plugin](https://github.com/gmazzo/gradle-buildconfig-plugin) entirely for project constants
- Switched to Gradle Kotlin DSL
- Switched to Adoptium
- Moved logo to the root of the resources module
- General cleanup
- General typo fixing
- Added missing since tags
- Deprecated `PooledLongQueue`

### Removed

- Removed access transformed as overwrites can already do the job on their own

## Alfheim Version 1.0.2 Changelog - 2023-09-12

### Fixed

- Fixed crash on world load with Nothirium
- Fixed crashes on dedicated servers

### Internal

- Reorganized `ChunkMixin`
- Cleaned up `ChunkMixin`

## Alfheim Version 1.0.1 Changelog - 2023-09-08

### Fixed

- Fixed crash on launch with certain mods (Fluidlogged API, CodeChickenLib, FunkyLocomotion and more)

## Alfheim Version 1.0 Changelog - 2023-09-06

Initial Release
