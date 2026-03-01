package io.github.theeluke;

import co.aikar.commands.MessageType;
import co.aikar.commands.PaperCommandManager;
import io.github.theeluke.commands.LMenusCommand;
import io.github.theeluke.listeners.InventoryListener;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class LMenus extends JavaPlugin {

    private static LMenus instance;

    private PaperCommandManager commandManager;
    private MenuManager menuManager;
    private SessionManager sessionManager;
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        instance = this;

        // load managers
        this.menuManager = new MenuManager();
        this.sessionManager = new SessionManager();
        this.storageManager = new StorageManager(this);
        this.commandManager = new PaperCommandManager(this);

        // load menus
        this.storageManager.loadAll(menuManager);

        // register listener
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        // register command & tab completion
        this.commandManager.getCommandCompletions().registerAsyncCompletion("menus", c -> {
            return this.menuManager.getLoadedMenus().stream()
                    .map(Menu::getName)
                    .collect(java.util.stream.Collectors.toList());
        });
        this.commandManager.registerCommand(new LMenusCommand(this));
        this.commandManager.enableUnstableAPI("help");
        // Set the colors for the Help Menu
        this.commandManager.getFormat(MessageType.HELP).setColor(1, ChatColor.WHITE);
        this.commandManager.getFormat(MessageType.HELP).setColor(2, ChatColor.GRAY);
        this.commandManager.getFormat(MessageType.HELP).setColor(3, ChatColor.AQUA);
        // Set the colors for Syntax Errors
        this.commandManager.getFormat(MessageType.SYNTAX).setColor(1, ChatColor.AQUA);
        this.commandManager.getFormat(MessageType.SYNTAX).setColor(2, ChatColor.WHITE);
        this.commandManager.usePerIssuerLocale(true, false);

        getLogger().info("LMenus is enabled.");
    }

    @Override
    public void onDisable() {
        // Save any pending data to prevent data loss on server stop/reload
        if (this.storageManager != null) {
            this.storageManager.saveAll(menuManager);
        }

        getLogger().info("LMenus is enabled.");
    }

    public static LMenus getInstance() { return instance; }
    public SessionManager getSessionManager() { return sessionManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public MenuManager getMenuManager() { return menuManager; }
}
