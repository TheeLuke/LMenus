package io.github.theeluke.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID player, String menuName, int slot) {
        if (!cooldowns.containsKey(player)) return false;

        String key = menuName + "_" + slot;
        Long expiry = cooldowns.get(player).get(key);

        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public long getRemainingSeconds(UUID player, String menuName, int slot) {
        if (!isOnCooldown(player, menuName, slot)) return 0;

        String key = menuName + "_" + slot;
        long expiry = cooldowns.get(player).get(key);

        return (expiry - System.currentTimeMillis()) / 1000;
    }

    public void setCooldown(UUID player, String menuName, int slot, int seconds) {
        cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                .put(menuName + "_" + slot, System.currentTimeMillis() + (seconds * 1000L));
    }
}
