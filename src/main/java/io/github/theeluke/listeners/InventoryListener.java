package io.github.theeluke.listeners;

import io.github.theeluke.LMenus;
import io.github.theeluke.managers.CooldownManager;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import io.github.theeluke.utils.MessageUtil;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Map;

public class InventoryListener implements Listener {

    private final SessionManager sessionManager;
    private final MenuManager menuManager;
    private final StorageManager storageManager;
    private final CooldownManager cooldownManager;

    public InventoryListener(LMenus plugin) {
        this.sessionManager = plugin.getSessionManager();
        this.menuManager = plugin.getMenuManager();
        this.storageManager = plugin.getStorageManager();
        this.cooldownManager = plugin.getCooldownManager();
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

                Menu menu = menuManager.getMenu(session.menuName());
                int slot = event.getRawSlot();

                if (menu != null && menu.getButtons().containsKey(slot)) {
                    List<Menu.Button> buttons = menu.getButtons().get(slot);

                    // 1. Check for Cooldown Flags
                    int cooldownSeconds = 0;
                    for (Menu.Button btn : buttons) {
                        if (btn.flags().containsKey("cooldown")) {
                            try {
                                cooldownSeconds = Math.max(cooldownSeconds, Integer.parseInt(btn.flags().get("cooldown")));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    // 2. Enforce the Cooldown
                    if (cooldownSeconds > 0) {
                        if (cooldownManager.isOnCooldown(player.getUniqueId(), menu.getName(), slot)) {
                            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), menu.getName(), slot);
                            MessageUtil.send(player, "on_cooldown", "{time}", String.valueOf(remaining));
                            return; // Stop them from running the commands!
                        }
                    }

                    boolean closeInventory = true;
                    for (Menu.Button btn : buttons) {
                        if (btn.flags().containsKey("close_on_click") &&
                                btn.flags().get("close_on_click").equalsIgnoreCase("false")) {
                            closeInventory = false;
                            break;
                        }
                        if (btn.type().equals("menu")) {
                            closeInventory = false;
                        }
                    }

                    if (closeInventory) {
                        player.closeInventory();
                    }

                    // 3. Execute the Commands
                    for (Menu.Button button : buttons) {
                        if (button.type().equals("command")) {
                            String cmd = button.action().replace("%player_name%", player.getName());
                            if (button.isPlayer()) {
                                player.performCommand(cmd);
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }
                        } else if (button.type().equals("menu")) {
                            Bukkit.getScheduler().runTask(LMenus.getInstance(), () -> {
                                player.performCommand("lm open " + button.action());
                            });
                        }
                    }

                    // 4. Apply the Cooldown (if applicable)
                    if (cooldownSeconds > 0) {
                        cooldownManager.setCooldown(player.getUniqueId(), menu.getName(), slot, cooldownSeconds);
                    }
                }
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

        if (session.type() == SessionManager.SessionType.ADDING_BUTTON) {
            event.setCancelled(true);
            Inventory clickedInv = event.getClickedInventory();

            // If they clicked the top menu (an item or an empty slot)
            if (clickedInv != null && clickedInv.equals(event.getView().getTopInventory())) {
                int slot = event.getRawSlot();
                Menu menu = menuManager.getMenu(session.menuName());

                if (menu != null) {
                    menu.addButton(slot, new Menu.Button(session.buttonType(), session.buttonAction(), session.isPlayer(), new java.util.HashMap<>()));
                    storageManager.saveMenu(menu);

                    MessageUtil.send(player, "button_added", "{slot}", String.valueOf(slot));
                    player.closeInventory();
                }
            }
        }

        if (session.type() == SessionManager.SessionType.REMOVING_BUTTON) {
            event.setCancelled(true);
            Inventory clickedInv = event.getClickedInventory();

            if (clickedInv != null && clickedInv.equals(event.getView().getTopInventory())) {
                int slot = event.getRawSlot();
                Menu menu = menuManager.getMenu(session.menuName());

                if (menu != null) {
                    menu.removeButtons(slot);
                    storageManager.saveMenu(menu);

                    MessageUtil.send(player, "button_removed", "{slot}", String.valueOf(slot));
                    player.closeInventory();
                }
            }
        }

        if (session.type() == SessionManager.SessionType.ADDING_BUTTON_FLAG) {
            event.setCancelled(true);
            Inventory clickedInv = event.getClickedInventory();

            if (clickedInv != null && clickedInv.equals(event.getView().getTopInventory())) {
                int slot = event.getRawSlot();
                Menu menu = menuManager.getMenu(session.menuName());

                if (menu != null) {
                    if (!menu.getButtons().containsKey(slot)) {
                        MessageUtil.send(player, "no_buttons_on_slot", "{slot}", String.valueOf(slot));
                        return;
                    }

                    String flagName = session.buttonType();
                    String flagValue = session.buttonAction();

                    for (Menu.Button btn : menu.getButtons().get(slot)) {
                        if (flagValue.equalsIgnoreCase("none") || flagValue.equalsIgnoreCase("clear")) {
                            btn.flags().remove(flagName.toLowerCase());
                        } else {
                            btn.flags().put(flagName.toLowerCase(), flagValue);
                        }
                    }

                    storageManager.saveMenu(menu);
                    MessageUtil.send(player, "button_flag_set", "{slot}", String.valueOf(slot));
                    player.closeInventory();
                }
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
