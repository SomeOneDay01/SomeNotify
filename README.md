# SomeNotify

`SomeNotify` is a Velocity plugin for sending message to all players connected through the proxy.

## Features
- Command `/notify <message>`
- Command `/notify reload` to reload `config.yml`
- Broadcast to all online players on all backend servers
- Configurable message format with MiniMessage
- Explicit template mode selector: `format`, `lines`, `auto`
- Configurable sender display mode: `player`, `custom`, `console`
- Built-in language support: `ru`, `en`

## Build
```bash
mvn clean package
```

Built jar:
`target/SomeNotify-1.0.0.jar`

## Installation
1. Build the jar.
2. Put `SomeNotify-1.0.0.jar` into your Velocity `plugins` folder.
3. Start Velocity once to generate config.
4. Edit `plugins/SomeNotify/config.yml`.
5. Restart Velocity.

## Permission
- `somenotify.notify` allows using `/notify`.
- `somenotify.reload` allows using `/notify reload`.

## Config (`config.yml`)
```yml
language: "ru"
permission: "somenotify.notify"
reload-permission: "somenotify.reload"

message-template-mode: "auto"

message-format: |
  <gray>[<gold>Notify</gold>]</gray>
  <yellow>{sender}</yellow><gray>:</gray>
  <white>{message}</white>

message-lines:
  - "<gold><bold>ОПОВЕЩЕНИЕ</bold></gold>"
  - "<gray>|</gray>"
  - "<gray>|</gray> <white>{message}</white>"
  - "<gray>|</gray>"
  - "<gray>|</gray> <yellow>отправил {player}</yellow>"

sender-mode: "player"
custom-sender-name: "Administration"
console-name: "Console"

messages:
  ru:
    usage: "<red>Use /notify <message> or /notify reload</red>"
    no-permission: "<red>No permission.</red>"
    empty-message: "<red>Message cannot be empty.</red>"
    sent: "<green>Message sent ({online} players).</green>"
    reload-success: "<green>Config reloaded successfully.</green>"
    reload-failed: "<red>Config reload failed. Check console.</red>"

  en:
    usage: "<red>Usage: /notify <message> or /notify reload</red>"
    no-permission: "<red>You do not have permission to use this command.</red>"
    empty-message: "<red>Message cannot be empty.</red>"
    sent: "<green>Message sent ({online} players).</green>"
    reload-success: "<green>Config reloaded successfully.</green>"
    reload-failed: "<red>Config reload failed. Check console.</red>"
```

You can replace texts in `messages.ru` with any Russian strings you want.

## Placeholders
- `{sender}` resolved sender name based on `sender-mode`
- `{player}` alias of `{sender}`
- `{message}` message from command arguments
- `{online}` number of players who received the message

## Notes
- `message-format` supports MiniMessage tags.
- `message-template-mode` controls which template is used:
  - `format`: always `message-format`
  - `lines`: always `message-lines` (fallback to `message-format` if empty)
  - `auto`: `message-lines` when non-empty, otherwise `message-format`
- `language` accepts `ru` or `en`. Any other value falls back to `ru`.
