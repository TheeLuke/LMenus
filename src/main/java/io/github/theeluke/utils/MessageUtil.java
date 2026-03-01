package io.github.theeluke.utils;

import io.github.theeluke.LMenus;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.List;

public class MessageUtil {

    public static void send(CommandSender sender, String configPath) {
        String prefix = LMenus.getInstance().getConfig().getString("settings.prefix", "&8[&bLMenus&8] ");
        String message = LMenus.getInstance().getConfig().getString("messages." + configPath, "&cMissing message: " + configPath);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    public static void send(CommandSender sender, String configPath, String placeholder, String replacement) {
        String prefix = LMenus.getInstance().getConfig().getString("settings.prefix", "&8[&bLMenus&8] ");
        String message = LMenus.getInstance().getConfig().getString("messages." + configPath, "&cMissing message: " + configPath);

        message = message.replace(placeholder, replacement);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    public static void sendList(CommandSender sender, String configPath, java.util.Map<String, String> placeholders) {
        java.util.List<String> messages = LMenus.getInstance().getConfig().getStringList("messages." + configPath);

        if (messages.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Missing list message: " + configPath);
            return;
        }

        for (String line : messages) {
            // Swap out every placeholder provided in the map
            if (placeholders != null) {
                for (java.util.Map.Entry<String, String> entry : placeholders.entrySet()) {
                    line = line.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
            // Translate colors and send the individual line
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }
}
