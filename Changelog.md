# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project follows to [Ragnarök Versioning Convention](https://shor.cz/ragnarok_versioning_convention).

## [UNRELEASED] Alfheim Version 1.2 Changelog

### Changed

- Stopped using `PooledLongQueue` using `LongArrayFIFOQueue` instead, should be more optimized (Lower memory usage, faster & lighter lighting updates)
- Skip spreading light neighbor checks early if the current light is lower than the neighbor light
- Made minor changes to clamping (Shouldn't cause a difference)

### Internal

- Updated RFG
- Updated Gradle

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
