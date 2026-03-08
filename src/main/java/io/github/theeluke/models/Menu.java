package io.github.theeluke.models;

import io.github.theeluke.utils.MessageUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Menu {

    private final String name;
    private int size;
    private String title;
    private final UUID creator;
    private final long creationDate;
    private Map<Integer, ItemStack> items;
    private Map<Integer, java.util.List<Button>> buttons = new java.util.HashMap<>();
    private java.util.Map<String, String> flags = new java.util.HashMap<>();

    public Menu(String name, int size, String title, UUID creator, long creationDate) {
        this.name = name;
        this.size = size;
        this.title = (title != null && !title.isEmpty()) ? title : name;
        this.creator = creator;
        this.creationDate = creationDate;
        this.items = new HashMap<>();
    }

    public String getName() { return name; }
    public int getSize() { return size; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public void setSize(int size) { this.size = size; }
    public UUID getCreator() { return creator; }
    public long getCreationDate() { return creationDate; }
    public Map<Integer, ItemStack> getItems() { return items; }

    public void setItem(int slot, ItemStack item) {
        this.items.put(slot, item);
    }

    public void setItems(Map<Integer, ItemStack> items) {
        this.items = items;
    }

    public Inventory buildInventory(Player player, boolean isEditing) {
        String formattedTitle = MessageUtil.format(player, this.title);
        Inventory inv = Bukkit.createInventory(null, this.size, formattedTitle);


        // Place physical items
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            ItemStack originalItem = entry.getValue();
            int slot = entry.getKey();

            // If the admin is editing, just show the raw item so they don't lose the placeholder strings
            if (isEditing) {
                inv.setItem(entry.getKey(), originalItem.clone());
                continue;
            }

            // If a player is viewing, clone the item and format its text!
            ItemStack displayItem = originalItem.clone();
            ItemMeta meta = displayItem.getItemMeta();

            if (meta != null) {
                // Parse PlaceholderAPI and Hex Colors for the Display Name
                if (meta.hasDisplayName()) {
                    meta.setDisplayName(MessageUtil.format(player, meta.getDisplayName()));
                }

                // Parse PlaceholderAPI and Hex Colors for every line of Lore
                if (meta.hasLore()) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : meta.getLore()) {
                        newLore.add(MessageUtil.format(player, line));
                    }
                    meta.setLore(newLore);
                }
                displayItem.setItemMeta(meta);
            }

            if (slot < this.size) {
                boolean showItem = true;

                if (this.buttons.containsKey(slot)) {
                    for (Button btn : this.buttons.get(slot)) {

                        if (btn.flags().containsKey("permission")) {
                            String requiredPerm = btn.flags().get("permission");

                            if (!requiredPerm.equalsIgnoreCase("none") && !player.hasPermission(requiredPerm)) {

                                if (btn.flags().containsKey("visible_no_permission") &&
                                        btn.flags().get("visible_no_permission").equalsIgnoreCase("false")) {

                                    showItem = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                if(showItem) inv.setItem(entry.getKey(), displayItem);
            }
        }

        // Only run Smart Fill if the admin is NOT editing the menu
        if (!isEditing) {
            Material fillerMat = getFillerMaterial();
            if (fillerMat != null && fillerMat != Material.AIR) {
                ItemStack fillerItem = new ItemStack(fillerMat);
                ItemMeta meta = fillerItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(" ");
                    fillerItem.setItemMeta(meta);
                }

                for (int i = 0; i < this.size; i++) {
                    if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                        inv.setItem(i, fillerItem);
                    }
                }
            }
        }

        return inv;
    }

    public record Button(String type, String action, boolean isPlayer, Map<String, String> flags) {}
    public Map<Integer, List<Button>> getButtons() { return buttons; }

    public void addButton(int slot, Button button) {
        this.buttons.computeIfAbsent(slot, k -> new ArrayList<>()).add(button);
    }

    public void removeButtons(int slot) {
        this.buttons.remove(slot);
    }

    public Map<String, String> getFlags() { return flags; }

    public void setFlag(String key, String value) {
        this.flags.put(key.toLowerCase(), value);
    }

    public void removeFlag(String key) {
        this.flags.remove(key.toLowerCase());
    }

    public String getFlag(String key) {
        return this.flags.get(key.toLowerCase());
    }

    public boolean isAutoRefresh() {
        return Boolean.parseBoolean(flags.getOrDefault("auto_refresh", "true"));
    }

    public int getRefreshRate() {
        try {
            return Integer.parseInt(flags.getOrDefault("refresh_ticks", "20"));
        } catch (NumberFormatException e) {
            return 20; // Default to 1 second if the admin typed a bad number
        }
    }

    public Material getFillerMaterial() {
        String matName = flags.get("filler_item");
        if (matName == null) return null;
        return Material.matchMaterial(matName.toUpperCase());
    }
}
