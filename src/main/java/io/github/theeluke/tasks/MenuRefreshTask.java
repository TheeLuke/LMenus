package io.github.theeluke.tasks;

import io.github.theeluke.LMenus;
import io.github.theeluke.managers.MenuManager;
import io.github.theeluke.managers.SessionManager;
import io.github.theeluke.models.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class MenuRefreshTask implements Runnable {

    private final SessionManager sessionManager;
    private final MenuManager menuManager;
    private int tickCounter = 0;

    public MenuRefreshTask(LMenus plugin) {
        this.sessionManager = plugin.getSessionManager();
        this.menuManager = plugin.getMenuManager();
    }

    @Override
    public void run() {
        tickCounter++;

        // Loop through all online players to see who is viewing a menu
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!sessionManager.hasSession(player)) continue;

            SessionManager.Session session = sessionManager.getSession(player);

            // We only refresh if they are just viewing it, NOT editing it!
            if (session.type() != SessionManager.SessionType.VIEWING) continue;

            Menu menu = menuManager.getMenu(session.menuName());
            if (menu == null || !menu.isAutoRefresh()) continue;

            // Check if it's time to refresh this specific menu based on its custom tick rate
            if (tickCounter % menu.getRefreshRate() == 0) {

                // Build the new inventory layout (which processes PlaceholderAPI variables for this exact moment)
                Inventory updatedInventory = menu.buildInventory(player, false);

                // Silently update the contents of their currently open top inventory
                player.getOpenInventory().getTopInventory().setContents(updatedInventory.getContents());
            }
        }
    }
}
