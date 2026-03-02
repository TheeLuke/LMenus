package io.github.theeluke.models;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Menu {

    private final String name;
    private final int size;
    private String title;
    private final UUID creator;
    private final long creationDate;
    private Map<Integer, ItemStack> items;

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
    public UUID getCreator() { return creator; }
    public long getCreationDate() { return creationDate; }
    public Map<Integer, ItemStack> getItems() { return items; }

    public void setItem(int slot, ItemStack item) {
        this.items.put(slot, item);
    }

    public void setItems(Map<Integer, ItemStack> items) {
        this.items = items;
    }

    public Inventory buildInventory() {
        String formattedTitle = ChatColor.translateAlternateColorCodes('&', this.title);

        Inventory inv = Bukkit.createInventory(null, this.size, formattedTitle);
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue());
        }

        return inv;
    }
}
