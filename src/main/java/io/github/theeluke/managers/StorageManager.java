package io.github.theeluke.managers;

import io.github.theeluke.LMenus;
import io.github.theeluke.models.Menu;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StorageManager {

    private final LMenus plugin;
    private final File menuFolder;

    public StorageManager(LMenus plugin) {
        this.plugin = plugin;
        // Creates a subfolder called "menus"
        this.menuFolder = new File(plugin.getDataFolder(), "menus");
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }
    }

    public void saveMenu(Menu menu) {
        File file = new File(menuFolder, menu.getName().toLowerCase() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Save metadata
        config.set("name", menu.getName());
        config.set("size", menu.getSize());
        config.set("title", menu.getTitle());
        config.set("creator", menu.getCreator().toString());
        config.set("creationDate", menu.getCreationDate());

        // Clear existing items in config to prevent "ghost" items if the menu was edited and an item was removed
        config.set("items", null);

        // Save items by their slot number
        for (Map.Entry<Integer, ItemStack> entry : menu.getItems().entrySet()) {
            config.set("items." + entry.getKey(), entry.getValue());
        }

        // Save Menu Flags
        config.set("flags", null); // Clear old flags
        for (Map.Entry<String, String> flag : menu.getFlags().entrySet()) {
            config.set("flags." + flag.getKey(), flag.getValue());
        }

        // When saving buttons, update the loop to include their flags:
        config.set("buttons", null);
        for (Map.Entry<Integer, java.util.List<Menu.Button>> entry : menu.getButtons().entrySet()) {
            int slot = entry.getKey();
            java.util.List<Menu.Button> btnList = entry.getValue();

            for (int i = 0; i < btnList.size(); i++) {
                Menu.Button btn = btnList.get(i);
                config.set("buttons." + slot + "." + i + ".type", btn.type());
                config.set("buttons." + slot + "." + i + ".action", btn.action());
                config.set("buttons." + slot + "." + i + ".isPlayer", btn.isPlayer());

                // Save Button Flags
                if (btn.flags() != null && !btn.flags().isEmpty()) {
                    for (Map.Entry<String, String> flag : btn.flags().entrySet()) {
                        config.set("buttons." + slot + "." + i + ".flags." + flag.getKey(), flag.getValue());
                    }
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save menu: " + menu.getName());
            e.printStackTrace();
        }
    }

    public int loadAll(MenuManager menuManager) {
        if (!menuFolder.exists()) return 0;

        File[] files = menuFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return 0;

        int count = 0;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            try {
                String name = config.getString("name");
                int size = config.getInt("size");
                String title = config.getString("title");
                UUID creator = UUID.fromString(Objects.requireNonNull(config.getString("creator")));
                long creationDate = config.getLong("creationDate");

                Menu menu = new Menu(name, size, title, creator, creationDate);

                // Load items if the configuration section exists
                if (config.contains("items")) {
                    for (String key : Objects.requireNonNull(config.getConfigurationSection("items")).getKeys(false)) {
                        int slot = Integer.parseInt(key);
                        ItemStack item = config.getItemStack("items." + slot);
                        if (item != null) {
                            menu.setItem(slot, item);
                        }
                    }
                }

                if (config.contains("flags")) {
                    for (String key : Objects.requireNonNull(config.getConfigurationSection("flags")).getKeys(false)) {
                        menu.setFlag(key, config.getString("flags." + key));
                    }
                }

                if (config.contains("buttons")) {
                    for (String slotKey : Objects.requireNonNull(config.getConfigurationSection("buttons")).getKeys(false)) {
                        int slot = Integer.parseInt(slotKey);
                        for (String indexKey : Objects.requireNonNull(config.getConfigurationSection("buttons." + slotKey)).getKeys(false)) {
                            String type = config.getString("buttons." + slotKey + "." + indexKey + ".type");
                            String action = config.getString("buttons." + slotKey + "." + indexKey + ".action");
                            boolean isPlayer = config.getBoolean("buttons." + slotKey + "." + indexKey + ".isPlayer");

                            // Load Button Flags
                            java.util.Map<String, String> btnFlags = new java.util.HashMap<>();
                            String flagPath = "buttons." + slotKey + "." + indexKey + ".flags";
                            if (config.contains(flagPath)) {
                                for (String flagKey : Objects.requireNonNull(config.getConfigurationSection(flagPath)).getKeys(false)) {
                                    btnFlags.put(flagKey, config.getString(flagPath + "." + flagKey));
                                }
                            }

                            menu.addButton(slot, new Menu.Button(type, action, isPlayer, btnFlags));
                        }
                    }
                }

                menuManager.addMenu(menu);
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load menu file: " + file.getName() + " - " + e.getMessage());
            }

        }
        return count;
    }

    public void saveAll(MenuManager menuManager) {
        for (Menu menu : menuManager.getLoadedMenus()) {
            saveMenu(menu);
        }
        plugin.getLogger().info("Saved " + menuManager.getLoadedMenus().size() + " menus to disk.");
    }

    public void deleteMenuFile(String name) {
        File file = new File(menuFolder, name.toLowerCase() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
}
