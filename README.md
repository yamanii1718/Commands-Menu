# Command Menu — Fabric 1.21.11 Client Mod

A **client-side** Fabric mod that opens a searchable, paginated GUI listing every command available on the server (or in single-player).  Works anywhere Sodium or Iris works — no server-side component needed.

---

## Features

| Feature | Details |
|---|---|
| **Customizable keybind** | Default **K** — change it in *Options → Controls → Command Menu* |
| **Searches commands** | Type in the search box to filter instantly |
| **Paginated list** | 8 commands or options per page with navigation buttons |
| **Command tree browser** | Click a command to explore its subcommands and options |
| **Smart click** | Simple commands execute immediately; argument commands open chat pre-filled |
| **Open in chat** | Place the current command path in chat so you can complete its arguments |
| **Client-only** | Works on any server — no plugin/mod required server-side |

---

## How to Build

```bash
bash build.sh
```

The compiled JAR ends up at `build/libs/command-menu-1.0.0.jar`.

Copy that file into your `.minecraft/mods/` folder (alongside Fabric Loader and Fabric API).

---

## Manual build (if you prefer)

```bash
export JAVA_HOME=/nix/store/k95pqfzyvrna93hc9a4cg5csl7l4fh0d-openjdk-21.0.7+6
export PATH=$JAVA_HOME/bin:$PATH
cd minecraft-command-menu
gradle build --no-daemon
```

---

## Requirements

- **Minecraft** 1.21.11
- **Fabric Loader** ≥ 0.19.2
- **Fabric API** 0.141.4+1.21.11 or newer compatible version
- **Java** 21+

---

## Usage

1. Join a world or server.
2. Press **K** (or your configured key).
3. The Command Menu opens, listing every command the server exposes.
4. Type in the search box to filter commands or options.
5. Click a command to browse its available subcommands.
6. Use **Back** to return to the previous level.
7. Use **Open Chat** to place the current command path in the chat bar.
8. Simple leaf commands run immediately when clicked.
9. Press **Escape** to close without running anything.
