# AnnouncementGUI

AnnouncementGUI is a Paper plugin for managing scheduled announcements through an in-game inventory GUI.

It supports:

- create, edit, and delete announcements in GUI
- scheduled automatic broadcasts
- panel-style announcement layout with title, description, and message body
- mixed color formatting in one line
- `LOCAL`, `SERVER`, `SERVERS`, `GROUP`, and `GLOBAL` targets
- multi-server event sync through Redis

## Features

- Main GUI for announcement management
- Create/Edit/Delete announcement workflow
- Chat-based input prompts for announcement fields
- Structured announcement format:
  - internal name
  - title
  - description lines
  - message body lines
  - interval
  - target scope
  - enabled/disabled state
- Panel renderer for announcement output
- Support for legacy color codes like `&a`, `&b`, `&6`
- Support for hex color codes like `&#55FFFF`
- Optional `<center>...</center>` support for body lines
- Redis Pub/Sub sync for cross-server create/update/delete/broadcast events

## Current Storage Model

The current implementation uses:

- local YAML file storage for announcement data
- Redis only for cross-server event sync

That means:

- the source of truth is still `announcements.yml`
- Redis is not used as the main database
- if a server is offline during a sync event, it may miss updates

For production-grade multi-server persistence, the next recommended step is MySQL or MariaDB.

## Requirements

- Java 21
- Paper 1.21.x
- Maven 3.9+
- Redis if you want cross-server sync

## Build

```bash
mvn clean package
```

Or on Windows:

```bat
build.bat
```

Output jar:

```text
target/announcementgui-1.0.jar
```

## Installation

1. Build the plugin jar.
2. Put the jar into your Paper server `plugins` folder.
3. Start the server once.
4. Edit the generated config.
5. Restart the server or run `/announcement reload`.

## Configuration

Main config file:

```text
plugins/AnnouncementGUI/config.yml
```

### Example

```yml
server:
  id: "lobby-1"
  groups:
    - "lobby"
    - "network"

storage:
  file: "announcements.yml"

sync:
  enabled: true
  type: "REDIS"
  redis:
    uri: "redis://127.0.0.1:6379/0"
    channel: "announcementgui:sync"

scheduler:
  check-interval-ticks: 20

formats:
  panel:
    top-border: "&6&m------------------------------------------------"
    body-divider: "&6&m------------------------------------------------"
    bottom-border: "&6&m------------------------------------------------"
    body-separator-mode: "DIVIDER"
```

### `server.id`

`server.id` is the unique ID for one Paper server instance.

It is **not** a Minecraft world name.

Examples:

- `lobby-1`
- `survival-1`
- `survival-2`
- `minigame-1`

### `server.groups`

`groups` are server categories.

They are **not** world names.

Examples:

- `lobby`
- `survival`
- `minigame`
- `economy`

## Multi-Server and Cross-Server Setup

To use cross-server sync:

1. Give every server a different `server.id`
2. Point all servers to the same Redis instance
3. Use the same Redis `channel`
4. Set `sync.enabled: true`

### Example

Server A:

```yml
server:
  id: "lobby-1"
  groups:
    - "lobby"
```

Server B:

```yml
server:
  id: "survival-1"
  groups:
    - "survival"
```

Shared Redis config on both:

```yml
sync:
  enabled: true
  type: "REDIS"
  redis:
    uri: "redis://:yourpassword@redis.example.com:6379/0"
    channel: "announcementgui:sync"
```

## Announcement Format

Announcements are rendered like a panel:

```text
------------------------------------------------
centered title
centered description
------------------------------------------------
message body
------------------------------------------------
```

Alternative style:

```text
------------------------------------------------
centered title
centered description

Website:
Discord:
Store:
------------------------------------------------
```

To switch spacing between header and body:

- `DIVIDER` = line separator
- `BLANK` = empty line
- `NONE` = no separator

## Color Support

Supported formats:

- legacy colors: `&a`, `&b`, `&6`, `&c`
- styles: `&l`, `&o`, `&n`, `&m`
- hex colors: `&#55FFFF`

Examples:

```text
&bWelcome! &6PlayerName
&cin example network
Website: &#55FFFFwww.example.com
Discord: &ediscord.example.com
Store: &astore.example.com
```

Body lines can also be centered manually:

```text
<center>&bCentered text</center>
```

## Commands

- `/announcementgui`
- `/agui`
- `/announcement open`
- `/announcement reload`
- `/announcement list`
- `/announcement broadcast <id>`

## Permissions

- `announcementgui.open`
- `announcementgui.create`
- `announcementgui.edit`
- `announcementgui.delete`
- `announcementgui.reload`
- `announcementgui.broadcast`
- `announcementgui.admin`

## Testing Checklist

- Plugin loads without errors
- `/agui` opens the main menu
- Create announcement from GUI
- Edit announcement title/description/message
- Delete announcement through confirm screen
- Announcement persists after restart
- Scheduled broadcast runs at the expected interval
- `SERVER` and `GROUP` targeting works as expected
- Redis sync propagates create/update/delete across servers

## Limitations

- Main persistence is still YAML, not SQL
- Redis sync is event-based, not shared-database-based
- If a server misses sync events while offline, its local file can become stale
- Chat input relies on the Bukkit conversation system for prompt capture

## Project Status

This project currently covers the first working implementation of AnnouncementGUI:

- GUI CRUD
- local persistence
- scheduler
- panel formatting
- Redis cross-server sync

The next major improvement is shared SQL storage for stronger multi-server consistency.

## License

Apache License 2.0. See [LICENSE](D:/coding/AnnouncementGUI/github/LICENSE).
