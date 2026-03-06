package io.github.theeluke.models;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
    private Map<Integer, java.util.List<Button>> buttons = new java.util.HashMap<>();

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

    public Inventory buildInventory(Player player) {
        // Use our new formatter to parse Hex and PAPI!
        String formattedTitle = io.github.theeluke.utils.MessageUtil.format(player, this.title);

        Inventory inv = Bukkit.createInventory(null, this.size, formattedTitle);

        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue());
        }

        return inv;
    }

    public record Button(String type, String action, boolean isPlayer) {}
    public Map<Integer, java.util.List<Button>> getButtons() { return buttons; }

    public void addButton(int slot, Button button) {
        this.buttons.computeIfAbsent(slot, k -> new java.util.ArrayList<>()).add(button);
    }

    public void removeButtons(int slot) {
        this.buttons.remove(slot);
    }
}
