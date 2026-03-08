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
