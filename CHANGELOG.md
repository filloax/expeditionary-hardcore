# Changelog

## 1.0.0

*First release*

- New loadouts from the series
- Adapt quirks from the series
- Final character skins, replacing the placeholder ones
- Hardcore hearts in the HUD
- `enableCustomHeartsIcon` config to use a custom hearts icon from a resource pack (off by default)
- `resetEnderChest` config, clears the player's ender chest on death (on by default)
- New respawn center option: death position
- Add mod icon
- Update default settings for respawn
- Fix player model crash with stuck arrows
- Fix crash when Apibalego is not installed
- Fix quirk description vanishing in the effect gfx
- Fix loadout items going into the boots slot
- Update bundled Apibalego and FilloaxLib (fixes a NeoForge warning), update KFF requirement
- \[Cydonia mode] API entry to send events to Apibalego

## 0.4.2

*Beta*

- Avoid repeating loadouts / skins (quirks already sort of did that, but consolidate logic)
- \[Cydonia mode] Shield has fixed color + lang string tweak

## 0.4.1

*Beta*

- Update localization
- Record death point (currently only for history cmd and cydonia mode)
- \[Cydonia mode] Fix info sending

## 0.4.0

*Beta*

- Add proper sprites instead of placeholders
- Use placeholder skins taken from online for random model feature (to be removed in final published version)
- \[Cydonia mode] auto add Growsseth pattern to loadout shields, with random colors

## 0.3.4

*Beta*

- Improve DH compatibility again, should work with shaders. No-shader version may not work
  properly until some DH-side fixes are released

## 0.3.3

*Beta*

- Improve DH compatibility (restart worldgen on respawn, fix stutter with shaders), no longer
  requires nightly build

## 0.3.2

*Beta*

- Add Distant Horizons support: worked fine even before, but adds a setting
  `respawnDistantHorizonsDisableSeconds` that turns off DH for that time
  after respawning, to avoid LoD islands and assorted wonkiness from teleporting
  (set to 0 to disable)

## 0.3.0

*Beta*

- Update to 26.3 (neoforge broken until kff exists)

## 0.2.1

*Beta*

- Update bundled Apibalego

## 0.2.0

*Beta*

- Fix Gamemode button in wrong tabs
- Builtin datapacks in Cydonia mode

## 0.1.0

*Beta*

Initial Beta release.