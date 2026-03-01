package io.github.theeluke.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    // Defines what the player is doing with the GUI
    public enum SessionType { CREATING, EDITING, VIEWING }

    public static class Session {
        private final SessionType type;
        private final String menuName;

        public Session(SessionType type, String menuName) {
            this.type = type;
            this.menuName = menuName;
        }

        public SessionType getType() { return type; }
        public String getMenuName() { return menuName; }
    }

    private final Map<UUID, Session> activeSessions = new HashMap<>();

    public void startSession(Player player, SessionType type, String menuName) {
        activeSessions.put(player.getUniqueId(), new Session(type, menuName));
    }

    public void endSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    public Session getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public boolean hasSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
}
