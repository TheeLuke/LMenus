package io.github.theeluke.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.github.theeluke.LMenus;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import io.github.theeluke.utils.MessageUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("unused")
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
    @Description("Creates a menu.")
    public void onCreate(Player player, String name, int size, @Optional String title) {
        if (menuManager.menuExists(name)) {
            MessageUtil.send(player, "menu_already_exists");
            return;
        }

        if (size % 9 != 0 || size < 9 || size > 54) {
            MessageUtil.send(player, "invalid_size");
            return;
        }

        // Create the new menu object and cache it
        Menu menu = new Menu(name, size, title, player.getUniqueId(), System.currentTimeMillis());
        menuManager.addMenu(menu);

        player.openInventory(menu.buildInventory(player, true));

        // Start the creation session
        sessionManager.startSession(player, SessionManager.SessionType.CREATING, menu.getName());
        MessageUtil.send(player, "menu_created", "{name}", name);
    }

    @Subcommand("edit")
    @CommandPermission("lmenus.admin.edit")
    @Syntax("<name>")
    @CommandCompletion("@menus")
    @Description("Opens GUI to edit the specified menu.")
    public void onEdit(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        // Open the pre-populated inventory
        player.openInventory(menu.buildInventory(player, true));
        sessionManager.startSession(player, SessionManager.SessionType.EDITING, menu.getName());
        MessageUtil.send(player, "menu_editing", "{name}", name);
    }

    @Subcommand("remove")
    @CommandPermission("lmenus.admin.remove")
    @Syntax("<name>")
    @CommandCompletion("@menus")
    @Description("Removes specified menu")
    public void onRemove(Player player, String name) {
        if (!menuManager.menuExists(name)) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        menuManager.removeMenu(name);
        storageManager.deleteMenuFile(name);
        MessageUtil.send(player, "menu_deleted", "{name}", name);
    }

    @Subcommand("retitle")
    @CommandPermission("lmenus.admin.retitle")
    @CommandCompletion("@menus @nothing")
    @Syntax("<name> <new_title>")
    @Description("Retitles the specified menu with new value.")
    public void onRetitle(Player player, String name, String newTitle) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        menu.setTitle(newTitle);
        storageManager.saveMenu(menu);
        MessageUtil.send(player, "menu_retitled", "{name}", name);
    }

    @Subcommand("reload")
    @CommandPermission("lmenus.admin.reload")
    @Description("Reloads all menus and configs.")
    public void onReload(org.bukkit.command.CommandSender sender) {
        // Closes any active menu sessions
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (sessionManager.hasSession(player)) {
                player.closeInventory();
                MessageUtil.send(player, "session_closed");
            }
        }

        menuManager.clear();
        storageManager.loadAll(menuManager);

        LMenus.getInstance().reloadConfig();

        MessageUtil.send(sender, "reload_success", "{count}", String.valueOf(menuManager.getLoadedMenus().size()));
    }

    @Subcommand("button command")
    @CommandPermission("lmenus.admin.button")
    @CommandCompletion("@menus true|false @nothing")
    @Syntax("<menu_name> <isPlayer> <command>")
    @Description("Adds a command button to selected slot in specified menu.")
    public void onButtonCommand(Player player, String name, boolean isPlayer, String commandArgs) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        player.openInventory(menu.buildInventory(player, false));
        sessionManager.startButtonSession(player, name, "command", commandArgs, isPlayer);
        MessageUtil.send(player, "click_to_bind");
    }

    @Subcommand("button menu")
    @CommandPermission("lmenus.admin.button")
    @CommandCompletion("@menus @menus")
    @Syntax("<menu_name> <target_menu>")
    @Description("Adds a button to selected slot to open target menu in specified menu.")
    public void onButtonMenu(Player player, String name, String targetMenu) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        player.openInventory(menu.buildInventory(player, false));
        sessionManager.startButtonSession(player, name, "menu", targetMenu, false);
        MessageUtil.send(player, "click_to_bind");
    }

    @Subcommand("button remove")
    @CommandPermission("lmenus.admin.button")
    @CommandCompletion("@menus @nothing")
    @Syntax("<menu_name>")
    @Description("Opens a GUI to removes a button from the specified menu.")
    public void onButtonRemove(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        player.openInventory(menu.buildInventory(player, false));
        sessionManager.startSession(player, SessionManager.SessionType.REMOVING_BUTTON, name);
        MessageUtil.send(player, "click_to_remove");
    }

    @Subcommand("flag menu")
    @CommandPermission("lmenus.admin.flags")
    @CommandCompletion("@menus auto_refresh|filler_item @nothing") // Suggests flags for the admin
    @Syntax("<menu_name> <flag_name> <value...>")
    @Description("Add a flag to a specific menu")
    public void onFlagMenu(Player player, String name, String flagName, String flagValue) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        // If they type "none" or "clear", remove the flag entirely
        if (flagValue.equalsIgnoreCase("none") || flagValue.equalsIgnoreCase("clear")) {
            menu.removeFlag(flagName);
            MessageUtil.send(player, "flag_removed", "{flag}", flagName);
        } else {
            menu.setFlag(flagName, flagValue);
            MessageUtil.send(player, "flag_set", "{flag}", flagName);
        }

        storageManager.saveMenu(menu);
    }

    @Subcommand("flag button")
    @CommandPermission("lmenus.admin.flags")
    @CommandCompletion("@menus <slot> cooldown|close_on_click @nothing")
    @Syntax("<menu_name> <slot> <flag_name> <value...>")
    @Description("Add a flag to specified button slot.")
    public void onFlagButton(Player player, String name, int slot, String flagName, String flagValue) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        if (!menu.getButtons().containsKey(slot)) {
            MessageUtil.send(player, "no_buttons_on_slot", "{slot}", String.valueOf(slot));
            return;
        }

        // Loop through all commands on this slot and apply the flag to them
        for (Menu.Button btn : menu.getButtons().get(slot)) {
            if (flagValue.equalsIgnoreCase("none") || flagValue.equalsIgnoreCase("clear")) {
                btn.flags().remove(flagName.toLowerCase());
            } else {
                btn.flags().put(flagName.toLowerCase(), flagValue);
            }
        }

        storageManager.saveMenu(menu);
        MessageUtil.send(player, "button_flag_set", "{slot}", String.valueOf(slot));
    }

    // PLAYER COMMANDS

    @Subcommand("open")
    @CommandPermission("lmenus.use.open")
    @CommandCompletion("@menus")
    @Syntax("<name>")
    @Description("Opens the specified menu.")
    public void onOpen(Player player, String name) {
        openMenu(player, name);
    }

    @Subcommand("info")
    @CommandPermission("lmenus.use.info")
    @CommandCompletion("@menus")
    @Syntax("<name>")
    @Description("Provides details about the specified menu.")
    public void onInfo(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        String creatorName = org.bukkit.Bukkit.getOfflinePlayer(menu.getCreator()).getName();
        if (creatorName == null) {
            creatorName = "Unknown (" + menu.getCreator().toString() + ")";
        }

        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("name", menu.getName());
        placeholders.put("title", menu.getTitle());
        placeholders.put("size", String.valueOf(menu.getSize()));
        placeholders.put("items", String.valueOf(menu.getItems().size()));
        placeholders.put("creator", creatorName);
        placeholders.put("date", dateFormat.format(new Date(menu.getCreationDate())));

        MessageUtil.sendList(player, "menu_info", placeholders);
    }

    @Subcommand("list")
    @CommandPermission("lmenus.use.list")
    @Description("Lists out all menus.")
    public void onList(Player player) {
        if (menuManager.getLoadedMenus().isEmpty()) {
            MessageUtil.send(player, "no_menus");
            return;
        }

        // Extract the names of all loaded menus
        String menuNames = menuManager.getLoadedMenus().stream()
                .map(Menu::getName)
                .collect(java.util.stream.Collectors.joining(ChatColor.GRAY + ", " + ChatColor.AQUA));

        MessageUtil.send(player, "menu_list", "{menus}", menuNames);
    }

    @HelpCommand
    @CommandPermission("lmenus.use.help")
    @Description("Opens this help menu.")
    public void onHelp(Player player, co.aikar.commands.CommandHelp help) {
        help.showHelp();
    }

    // Helper
    private void openMenu(Player player, String name) {
        Menu menu = menuManager.getMenu(name);
        if (menu == null) {
            MessageUtil.send(player, "menu_not_found");
            return;
        }

        player.openInventory(menu.buildInventory(player, false));
        sessionManager.startSession(player, SessionManager.SessionType.VIEWING, menu.getName());
    }
}