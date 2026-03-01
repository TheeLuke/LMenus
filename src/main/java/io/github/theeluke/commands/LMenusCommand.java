package io.github.theeluke.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.theeluke.LMenus;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

@CommandAlias("lm|lmenus")
public class LMenusCommand extends BaseCommand {

    private final MenuManager menuManager;
    private final SessionManager sessionManager;
    private final StorageManager storageManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public LMenusCommand(LMenus plugin) {
        this.menuManager = plugin.getMenuManager();
        this.sessionManager = plugin.getSessionManager();
        this.storageManager = plugin.getStorageManager();
    }

    // ADMIN COMMANDS

    @Subcommand("create")
    @CommandPermission("lmenus.admin.create")
    @Syntax("<name> <size> [title]")
    public void onCreate(Player player, String name, int size, @Optional String title) {
        if (menuManager.menuExists(name)) {
            player.sendMessage(ChatColor.RED + "A menu with that name already exists!");
            return;
        }

        if (size % 9 != 0 || size < 9 || size > 54) {
            player.sendMessage(ChatColor.RED + "Size must be a multiple of 9, between 9 and 54.");
            return;
        }

        // Create the new menu object and cache it
        Menu menu = new Menu(name, size, title, player.getUniqueId(), System.currentTimeMillis());
        menuManager.addMenu(menu);

        // Open the empty inventory for them to fill
        Inventory inv = menu.buildInventory();
        player.openInventory(inv);

        // Start the creation session
        sessionManager.startSession(player, SessionManager.SessionType.CREATING, menu.getName());
        player.sendMessage(ChatColor.GREEN + "Creating menu '" + name + "'. Place items and close the inventory to save!");
    }

    @Subcommand("edit")
    @CommandPermission("lmenus.admin.edit")
    @Syntax("<name>")
    @CommandCompletion("@menus")
    public void onEdit(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "Menu not found.");
            return;
        }

        // Open the pre-populated inventory
        player.openInventory(menu.buildInventory());
        sessionManager.startSession(player, SessionManager.SessionType.EDITING, menu.getName());
        player.sendMessage(ChatColor.YELLOW + "Editing menu '" + menu.getName() + "'. Close the inventory to save changes.");
    }

    @Subcommand("remove")
    @CommandPermission("lmenus.admin.remove")
    @Syntax("<name>")
    @CommandCompletion("@menus")
    public void onRemove(Player player, String name) {
        if (!menuManager.menuExists(name)) {
            player.sendMessage(ChatColor.RED + "Menu not found.");
            return;
        }

        menuManager.removeMenu(name);
        storageManager.deleteMenuFile(name);
        player.sendMessage(ChatColor.GREEN + "Menu '" + name + "' has been permanently deleted.");
    }

    @Subcommand("retitle")
    @CommandPermission("lmenus.admin.retitle")
    @CommandCompletion("@menus @nothing")
    @Syntax("<name> <new_title>")
    public void onRetitle(Player player, String name, String newTitle) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "Menu not found.");
            return;
        }

        menu.setTitle(newTitle);
        storageManager.saveMenu(menu);
        player.sendMessage(ChatColor.GREEN + "Menu '" + name + "' retitled to: " + ChatColor.translateAlternateColorCodes('&', newTitle));
    }

    @Subcommand("reload")
    @CommandPermission("lmenus.admin.reload")
    @Description("Reloads all menus from the YAML files.")
    public void onReload(org.bukkit.command.CommandSender sender) {
        // Closes any active menu sessions
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (sessionManager.hasSession(p)) {
                p.closeInventory();
                p.sendMessage(ChatColor.RED + "An admin reloaded LMenus. Your session was safely closed.");
            }
        }

        menuManager.clear();
        storageManager.loadAll(menuManager);

        sender.sendMessage(ChatColor.GREEN + "[LMenus] Successfully reloaded " + menuManager.getLoadedMenus().size() + " menus from disk!");
    }

    // PLAYER COMMANDS

    @Subcommand("open") // Explicitly handles /lm open <name>
    @CommandPermission("lmenus.use.open")
    @CommandCompletion("@menus")
    @Syntax("<name>")
    public void onOpen(Player player, String name) {
        openMenu(player, name);
    }

    @Subcommand("info")
    @CommandPermission("lmenus.use.info")
    @CommandCompletion("@menus")
    @Syntax("<name>")
    public void onInfo(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "Menu not found.");
            return;
        }

        // Translate the UUID to a username
        String creatorName = org.bukkit.Bukkit.getOfflinePlayer(menu.getCreator()).getName();
        if (creatorName == null) {
            // Fallback
            creatorName = "Unknown (" + menu.getCreator().toString() + ")";
        }

        player.sendMessage(ChatColor.GRAY + "=== Menu Info: " + ChatColor.AQUA + menu.getName() + ChatColor.GRAY + " ===");
        player.sendMessage(ChatColor.GRAY + "Title: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', menu.getTitle()));
        player.sendMessage(ChatColor.GRAY + "Size: " + ChatColor.WHITE + menu.getSize() + " slots");
        player.sendMessage(ChatColor.GRAY + "Items configured: " + ChatColor.WHITE + menu.getItems().size());
        player.sendMessage(ChatColor.GRAY + "Created by: " + ChatColor.WHITE + creatorName);
        player.sendMessage(ChatColor.GRAY + "Created on: " + ChatColor.WHITE + dateFormat.format(new Date(menu.getCreationDate())));
    }

    @Subcommand("list")
    @CommandPermission("lmenus.use.list")
    public void onList(Player player) {
        if (menuManager.getLoadedMenus().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are currently no menus created.");
            return;
        }

        // Extract the names of all loaded menus
        String menuNames = menuManager.getLoadedMenus().stream()
                .map(Menu::getName)
                .collect(java.util.stream.Collectors.joining(ChatColor.GRAY + ", " + ChatColor.AQUA));

        player.sendMessage(ChatColor.GRAY + "=== " + ChatColor.AQUA + "Loaded Menus " + ChatColor.GRAY + "===");
        player.sendMessage(ChatColor.AQUA + menuNames);
    }

    @HelpCommand
    @CommandPermission("lmenus.use.help")
    public void onHelp(Player player, co.aikar.commands.CommandHelp help) {
        help.showHelp();
    }

    // Helper
    private void openMenu(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "Menu not found.");
            return;
        }

        player.openInventory(menu.buildInventory());
        sessionManager.startSession(player, SessionManager.SessionType.VIEWING, menu.getName());
    }
}
