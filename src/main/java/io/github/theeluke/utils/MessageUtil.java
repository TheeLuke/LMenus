package io.github.theeluke.utils;

import io.github.theeluke.LMenus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String format(Player player, String text) {
        if (text == null) return "";

        // 1. Apply PlaceholderAPI if it is installed on the server
        if (player != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }

        // 2. Parse Hex Colors (e.g., &#FF5555)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            // Converts &#FF5555 to BungeeCord's native hex format
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);
        text = buffer.toString();

        // 3. Parse Legacy Colors (e.g., &a, &l)
        return ChatColor.translateAlternateColorCodes('&', text);
    }

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
