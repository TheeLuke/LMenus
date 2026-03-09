package io.github.theeluke.listeners;

import io.github.theeluke.LMenus;
import io.github.theeluke.managers.CooldownManager;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.managers.StorageManager;
import io.github.theeluke.models.Menu;
import io.github.theeluke.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
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
                    ItemStack clickedItem = event.getCurrentItem();
                    if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                        return;
                    }

                    executeButtons(player, menu, slot, menu.getButtons().get(slot));
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
            else if (event.getClick() == ClickType.SWAP_OFFHAND) {
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

                    MessageUtil.send(player, "button_added", "{slot}", String.valueOf(slot));
                    player.closeInventory();

                    Bukkit.getScheduler().runTaskAsynchronously(LMenus.getInstance(), () -> {
                        storageManager.saveMenu(menu);
                    });
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
                    MessageUtil.send(player, "button_removed", "{slot}", String.valueOf(slot));
                    player.closeInventory();

                    Bukkit.getScheduler().runTaskAsynchronously(LMenus.getInstance(), () -> {
                        storageManager.saveMenu(menu);
                    });
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

                    MessageUtil.send(player, "button_flag_set", "{slot}", String.valueOf(slot));
                    player.closeInventory();

                    Bukkit.getScheduler().runTaskAsynchronously(LMenus.getInstance(), () -> {
                        storageManager.saveMenu(menu);
                    });
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
                MessageUtil.send(player, "menu_saved", "{name}", menuName);

                Bukkit.getScheduler().runTaskAsynchronously(LMenus.getInstance(), () -> {
                    storageManager.saveMenu(menu);
                });
            }
        }

        sessionManager.endSession(player);
    }

    private void executeButtons(Player player, Menu menu, int slot, List<Menu.Button> buttons) {
        // 1. PERMISSION CHECK
        for (Menu.Button btn : buttons) {
            if (btn.flags().containsKey("permission")) {
                String requiredPerm = btn.flags().get("permission");
                if (!requiredPerm.equalsIgnoreCase("none") && !player.hasPermission(requiredPerm)) {
                    MessageUtil.send(player, "no_permission_button");
                    return;
                }
            }
        }

        // 2. ECONOMY CHECK
        double totalCost = 0.0;
        for (Menu.Button btn : buttons) {
            if (btn.flags().containsKey("cost")) {
                try {
                    totalCost += Double.parseDouble(btn.flags().get("cost"));
                } catch (NumberFormatException ignored) {}
            }
        }

        Economy econ = LMenus.getInstance().getEconomy();
        if (totalCost > 0) {
            if (econ == null) {
                player.sendMessage("§cVault is not installed! This button is currently disabled.");
                return;
            }
            if (!econ.has(player, totalCost)) {
                MessageUtil.send(player, "not_enough_money", "{cost}", String.valueOf(totalCost));
                return;
            }
        }

        // 3. COOLDOWN CHECK
        int cooldownSeconds = 0;
        for (Menu.Button btn : buttons) {
            if (btn.flags().containsKey("cooldown")) {
                try {
                    cooldownSeconds = Math.max(cooldownSeconds, Integer.parseInt(btn.flags().get("cooldown")));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (cooldownSeconds > 0 && cooldownManager.isOnCooldown(player.getUniqueId(), menu.getName(), slot)) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), menu.getName(), slot);
            MessageUtil.send(player, "on_cooldown", "{time}", String.valueOf(remaining));
            return;
        }

        // 4. CLOSE INVENTORY LOGIC
        boolean closeInventory = true;
        for (Menu.Button btn : buttons) {
            if ((btn.flags().containsKey("close_on_click") && btn.flags().get("close_on_click").equalsIgnoreCase("false"))
                    || btn.type().equals("menu")) {
                closeInventory = false;
                break;
            }
        }
        if (closeInventory) {
            player.closeInventory();
        }

        // 5. WITHDRAW ECONOMY
        if (totalCost > 0 && econ != null) {
            econ.withdrawPlayer(player, totalCost);
            MessageUtil.send(player, "money_withdrawn", "{cost}", String.valueOf(totalCost));
        }

        // 6. EXECUTE COMMANDS
        for (Menu.Button button : buttons) {
            if (button.type().equals("command")) {
                String cmd = button.action().replace("%player_name%", player.getName());
                if (button.isPlayer()) {
                    player.performCommand(cmd);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            } else if (button.type().equals("menu")) {
                Bukkit.getScheduler().runTask(io.github.theeluke.LMenus.getInstance(), () -> {
                    player.performCommand("lm open " + button.action());
                });
            }
        }

        // 7. APPLY COOLDOWN
        if (cooldownSeconds > 0) {
            cooldownManager.setCooldown(player.getUniqueId(), menu.getName(), slot, cooldownSeconds);
        }
    }
}
