package io.github.theeluke.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    // Defines what the player is doing with the GUI
    public enum SessionType { CREATING, EDITING, VIEWING, ADDING_BUTTON, REMOVING_BUTTON }

    public record Session(SessionType type, String menuName, String buttonType, String buttonAction, boolean isPlayer) {
    }

    private final Map<UUID, Session> activeSessions = new HashMap<>();

    public void startSession(Player player, SessionType type, String menuName) {
        activeSessions.put(player.getUniqueId(), new Session(type, menuName, null, null, false));
    }

    public void startButtonSession(Player player, String menuName, String buttonType, String buttonAction, boolean isPlayer) {
        activeSessions.put(player.getUniqueId(), new Session(SessionType.ADDING_BUTTON, menuName, buttonType, buttonAction, isPlayer));
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
