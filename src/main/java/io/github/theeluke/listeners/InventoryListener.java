package io.github.theeluke.listeners;

import io.github.theeluke.LMenus;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import io.github.theeluke.utils.MessageUtil;
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
        if (!sessionManager.hasSession(player)) return;

        SessionManager.Session session = sessionManager.getSession(player);

        if (session.type() == SessionManager.SessionType.VIEWING) {

            Inventory clickedInv = event.getClickedInventory();

            // ANTI-DUPE LOGIC
            if (clickedInv == null) return;
            if (clickedInv.equals(event.getView().getTopInventory())) {
                event.setCancelled(true);
                return;
            }
            // Prevents shift-clicking
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            // Prevents double-clicking
            else if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
            // Block offhand swapping
            else if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!sessionManager.hasSession(player)) return;

        SessionManager.Session session = sessionManager.getSession(player);

        if (session.type() == SessionManager.SessionType.VIEWING) {
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
        String menuName = session.menuName();

        // EDITING LOGIC
        if (session.type() == SessionManager.SessionType.CREATING || session.type() == SessionManager.SessionType.EDITING) {
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
