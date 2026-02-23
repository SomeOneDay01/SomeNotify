# Changelog

All notable changes to this project are documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [Unreleased]
### Added
- Added `message-template-mode` config option to explicitly choose template source:
  - `format`, `lines`, `auto`.

### Changed
- Updated docs and default config to describe explicit template mode selection.

## [1.1.0] - 2026-02-23
### Added
- Added `/notify reload` command to reload `config.yml` without proxy restart.
- Added separate reload permission: `somenotify.reload` (`reload-permission` in config).
- Added language selection in config: `ru` and `en`.
- Added localized messages for both languages, including reload result messages.
- Added flexible message templates:
  - `message-format` (single/multiline string)
  - `message-lines` (list of lines, prioritized when non-empty)
- Added `{player}` placeholder alias (same value as `{sender}`).
- Added project `.gitignore`.
- Added `README.md` with build/install/config documentation.

### Changed
- Improved config loading with safe fallback to built-in defaults on error.
- Updated default `config.yml` with new options and examples.

## [1.0.0] - 2026-02-23
### Added
- Initial Velocity plugin setup (`SomeNotify`).
- Added `/notify <message>` broadcast command for all online players behind Velocity.
- Added configurable sender display mode (`player`, `custom`, `console`).
- Added configurable permission and message texts in `config.yml`.
