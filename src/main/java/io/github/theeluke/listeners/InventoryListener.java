package io.github.theeluke.listeners;

import io.github.theeluke.LMenus;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import io.github.theeluke.utils.MessageUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class InventoryListener implements Listener {

    private final SessionManager sessionManager;
    private final MenuManager menuManager;
    private final StorageManager storageManager;

    public InventoryListener(LMenus plugin) {
        this.sessionManager = plugin.getSessionManager();
        this.menuManager = plugin.getMenuManager();
        this.storageManager = plugin.getStorageManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // no active session? dont care about your clicks!
        if (!sessionManager.hasSession(player)) return;

        SessionManager.Session session = sessionManager.getSession(player);

        // View logic
        if (session.getType() == SessionManager.SessionType.VIEWING) {

            // Prevent if they clicked directly inside the top GUI menu
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
            }
            // Prevent shift-clicking items from their bottom inventory up into the GUI
            else {
                if (event.isShiftClick()) {
                    event.getView().getTopInventory();
                    event.setCancelled(true);
                }
            }
            // Prevent double-clicking to gather items into the cursor
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
        }
        // If CREATING or EDITING, do nothing and let them manipulate
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!sessionManager.hasSession(player)) return;

        SessionManager.Session session = sessionManager.getSession(player);

        if (session.getType() == SessionManager.SessionType.VIEWING) {
            // Check if any of the dragged slots fall within the top inventory.
            int topSize = event.getView().getTopInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (!sessionManager.hasSession(player)) return;

        SessionManager.Session session = sessionManager.getSession(player);
        String menuName = session.getMenuName();

        // EDITING LOGIC
        if (session.getType() == SessionManager.SessionType.CREATING || session.getType() == SessionManager.SessionType.EDITING) {
            Menu menu = menuManager.getMenu(menuName);

            if (menu != null) {
                Inventory inv = event.getInventory();
                Map<Integer, ItemStack> newItems = new HashMap<>();

                // Loop through the top inventory and map the non-empty items
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && !item.getType().isAir()) {
                        newItems.put(i, item.clone());
                    }
                }

                menu.setItems(newItems);
                storageManager.saveMenu(menu);

                MessageUtil.send(player, "menu_saved", "{name}", menuName);
            }
        }

        sessionManager.endSession(player);
    }
}
