# LMenus

![Spigot/Paper](https://img.shields.io/badge/Spigot%2FPaper-1.21-blue?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=java)
![Version](https://img.shields.io/badge/Version-1.0.0-success?style=for-the-badge)

**LMenus** is a highly dynamic, lightweight Spigot/Paper plugin that lets server admins create and manage custom GUI inventory menus entirely in-game. Ditch the YAML configuration files and build your shops, kits, and server selectors through a visual interface.

## Features

* **100% In-Game Editor:** Create, resize, and edit menus using a visual drag-and-drop inventory interface.
* **Multi-Command Chaining:** Bind player commands, console commands, or menu-linking actions to any button.
* **Advanced Flag System:** Customize menus and buttons with interactive flags (Vault costs, cooldowns, permission nodes, auto-refreshing).
* **Live Placeholders:** Full PlaceholderAPI support. Open menus re-render every tick to keep your placeholders perfectly live.
* **Safe Config Syncing:** Missing config keys are automatically injected on reload without overwriting your custom message translations.
* **Anti-Dupe:** Strict inventory listeners block shift-clicking, double-clicking, and offhand swapping to protect your server's economy.

## Soft Dependencies
While LMenus functions perfectly on its own, it hooks into the following plugins to expand its feature set:
* **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)** - Parses placeholders in item names, lore, and menu titles.
* **[Vault](https://www.spigotmc.org/resources/vault.34315/)** - Enables the `cost` flag for building economy-based shop menus.

---

## Architecture Overview

For developers looking to contribute or understand the codebase, LMenus is built on a structured, manager-based architecture:

* **`StorageManager`**: Persists menus as individual YAML files under `plugins/LMenus/menus/`, utilizing Bukkit's native `ItemStack` serialization.
* **`MenuManager`**: An in-memory cache (`Map<String, Menu>`) providing lightning-fast, case-insensitive menu lookups.
* **`SessionManager`**: Tracks active GUI states for players (`CREATING`, `EDITING`, `VIEWING`, `ADDING_BUTTON`, etc.) to safely route inventory clicks.
* **`MenuRefreshTask`**: A lightweight repeating task that re-renders open inventories for viewers if the `auto_refresh` flag is enabled.

---

## Commands & Permissions

LMenus uses Aikar's Command Framework (ACF) to provide structured subcommands and auto-tab completion.

| Command | Description | Permission |
| --- | --- | --- |
| `/lm create <name> <size> [title]` | Creates a new menu (1-6 rows, or 9-54 slots). | `lmenus.admin.create` |
| `/lm edit <name>` | Opens a menu in Edit Mode. | `lmenus.admin.edit` |
| `/lm resize <name> <size>` | Resizes an existing menu safely. | `lmenus.admin.resize` |
| `/lm retitle <name> <title>` | Renames the inventory GUI title. | `lmenus.admin.retitle` |
| `/lm remove <name>` | Deletes a menu from memory and disk. | `lmenus.admin.remove` |
| `/lm reload` | Reloads `config.yml` and closes active sessions. | `lmenus.admin.reload` |
| `/lm button command <menu> <isPlayer> <cmd>` | Binds a command to a clicked slot. | `lmenus.admin.button` |
| `/lm button menu <menu> <target>` | Binds a menu-open action to a slot. | `lmenus.admin.button` |
| `/lm button remove <menu>` | Clears all buttons from a clicked slot. | `lmenus.admin.button` |
| `/lm flag menu <menu> <flag> <value>` | Sets a menu-level flag. | `lmenus.admin.flags` |
| `/lm flag button <menu> <flag> <value>` | Sets a button-level flag on a clicked slot. | `lmenus.admin.flags` |
| `/lm flag list` | Displays all available flags in-game. | `lmenus.admin.flags` |
| `/lm open <name>` | Opens a menu for viewing. | `lmenus.use.open` |
| `/lm info <name>` | Displays internal metadata for a menu. | `lmenus.use.info` |
| `/lm list` | Lists all loaded menus. | `lmenus.use.list` |
| `/lm help` | Shows the help menu. | `lmenus.use.help` |

---

## The Flag System

Flags dictate how menus and buttons behave. You apply them in-game by running the flag command and clicking the target slot.

### Menu Flags

* `auto_refresh` (true/false) - Re-renders the menu continuously for live PAPI updates.
* `refresh_ticks` (number) - Determines the exact tick interval for auto-refreshing.
* `filler_item` (Material/none) - Fills all empty slots with the specified item.
* `permission` (node/none) - Restricts who can open the entire menu.

### Button Flags

* `cooldown` (seconds) - Per-player, per-slot cooldown before the button can be clicked again.
* `close_on_click` (true/false) - Dictates whether the GUI closes after a successful click.
* `permission` (node/none) - Restricts who can execute this specific button.
* `cost` (number) - Requires a Vault economy balance to click (charges automatically).
* `visible_no_permission` (true/false) - Dynamically hides the physical item from players who lack the button's permission node.

---

## Build Instructions

LMenus is a Maven project targeting Java 17 (compiled for a Java 21 runtime environment). 

To build the shaded jar locally:

```bash
# Clone the repository
git clone [https://github.com/theeluke/LMenus.git](https://github.com/theeluke/LMenus.git)
cd LMenus

# Build the shaded jar (Output: target/LMenus-<version>-shaded.jar)
mvn package

# Skip tests (if necessary)
mvn package -DskipTests
```

> **Note:** The build process automatically relocates ACF (`co.aikar.commands` → `io.github.theeluke.lmenus.acf`) to prevent class-loading conflicts with other plugins on the server.
