package io.github.theeluke;

import co.aikar.commands.MessageType;
import co.aikar.commands.PaperCommandManager;
import io.github.theeluke.commands.LMenusCommand;
import io.github.theeluke.listeners.InventoryListener;
import io.github.theeluke.managers.CooldownManager;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import io.github.theeluke.tasks.MenuRefreshTask;
import io.github.theeluke.utils.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public final class LMenus extends JavaPlugin {

    private static LMenus instance;

    private MenuManager menuManager;
    private SessionManager sessionManager;
    private StorageManager storageManager;
    private CooldownManager cooldownManager;

    Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;

        // config
        syncConfig();

        // bstats
        int pluginId = 29946;
        Metrics metrics = new Metrics(this, pluginId);

        // check for updates
        new UpdateChecker(this, 00000).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("There is not a new update available.");
            } else {
                getLogger().info("There is a new update available for LMenus! Download it here: https://www.spigotmc.org/resources/00000/");
            }
        });

        if (!setupEconomy()) {
            getLogger().warning("Vault not found! The 'cost' flag will be disabled.");
        } else {
            getLogger().info("Vault hooked successfully! Economy features enabled.");
        }

        // load managers
        this.menuManager = new MenuManager();
        this.sessionManager = new SessionManager();
        this.storageManager = new StorageManager(this);
        PaperCommandManager commandManager = new PaperCommandManager(this);

        File menusFolder = new File(getDataFolder(), "menus");
        if (!menusFolder.exists() || (menusFolder.isDirectory() && menusFolder.list() != null && Objects.requireNonNull(menusFolder.list()).length == 0)) {
            saveResource("menus/example_menu_1.yml", false);
            saveResource("menus/example_menu_2.yml", false);
            getLogger().info("First time install detected: Generated example menus!");
        }

        // cooldowns (cooldown flag)
        this.cooldownManager = new CooldownManager();

        // load menus
        int loadedMenus = this.storageManager.loadAll(this.menuManager);
        getLogger().info("Successfully loaded " + loadedMenus + " menus.");

        // MenuRefresh task (auto-refresh flag)
        Bukkit.getScheduler().runTaskTimer(this, new MenuRefreshTask(this), 0L, 1L);

        // register listener
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        // register command & tab completion
        commandManager.getCommandCompletions().registerAsyncCompletion("menus", c -> this.menuManager.getLoadedMenus().stream()
                .map(Menu::getName)
                .collect(java.util.stream.Collectors.toList()));
        commandManager.registerCommand(new LMenusCommand(this));
        commandManager.enableUnstableAPI("help");
        // Set the colors for the Help Menu
        commandManager.getFormat(MessageType.HELP).setColor(1, ChatColor.WHITE);
        commandManager.getFormat(MessageType.HELP).setColor(2, ChatColor.GRAY);
        commandManager.getFormat(MessageType.HELP).setColor(3, ChatColor.AQUA);
        // Set the colors for Syntax Errors
        commandManager.getFormat(MessageType.SYNTAX).setColor(1, ChatColor.AQUA);
        commandManager.getFormat(MessageType.SYNTAX).setColor(2, ChatColor.WHITE);
        commandManager.usePerIssuerLocale(true, false);

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

    public void syncConfig() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        InputStream defConfigStream = getResource("config.yml");

        if(defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            boolean needsSave = false;

            for (String key : defConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defConfig.get(key));
                    needsSave = true;
                }
            }

            if (needsSave) {
                saveConfig();
                getLogger().info("Injected missing configuration keys into config.yml!");
            }
        }
    }

    private boolean setupEconomy() {
        if(getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }


    public static LMenus getInstance() { return instance; }
    public SessionManager getSessionManager() { return sessionManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public Economy getEconomy() { return economy; }

}
