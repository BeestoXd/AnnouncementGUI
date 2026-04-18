package com.bx.announcementGUI.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&?#([0-9A-F]{6})");
    private static final int CHAT_CENTER_PX = 154;

    private ColorUtil() {
    }

    public static String colorize(String value) {
        String raw = value == null ? "" : value;
        return ChatColor.translateAlternateColorCodes('&', applyHexColors(raw));
    }

    public static String applyCenterTag(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!trimmed.toLowerCase().startsWith("<center>") || !trimmed.toLowerCase().endsWith("</center>")) {
            return value;
        }

        String content = trimmed.substring("<center>".length(), trimmed.length() - "</center>".length()).trim();
        return centerLine(content);
    }

    public static String centerLine(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isBlank()) {
            return raw;
        }

        String colored = colorize(raw);
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (int index = 0; index < colored.length(); index++) {
            char character = colored.charAt(index);

            if (character == ChatColor.COLOR_CHAR) {
                previousCode = true;
                continue;
            }

            if (previousCode) {
                previousCode = false;
                char lower = Character.toLowerCase(character);
                if (lower == 'l') {
                    isBold = true;
                } else if (isColorCode(lower) || lower == 'r') {
                    isBold = false;
                }
                continue;
            }

            int charWidth = getFontWidth(character);
            messagePxSize += isBold && character != ' ' ? charWidth + 1 : charWidth;
            messagePxSize++;
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CHAT_CENTER_PX - halvedMessageSize;
        if (toCompensate <= 0) {
            return raw;
        }

        int spaceLength = getFontWidth(' ') + 1;
        StringBuilder padding = new StringBuilder();
        int compensated = 0;
        while (compensated < toCompensate) {
            padding.append(' ');
            compensated += spaceLength;
        }
        return padding + raw;
    }

    private static String applyHexColors(String value) {
        Matcher matcher = HEX_PATTERN.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(toLegacyHex(hex)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private static String toLegacyHex(String hex) {
        StringBuilder builder = new StringBuilder()
                .append(ChatColor.COLOR_CHAR)
                .append('x');
        for (char character : hex.toCharArray()) {
            builder.append(ChatColor.COLOR_CHAR).append(character);
        }
        return builder.toString();
    }

    private static boolean isColorCode(char character) {
        return (character >= '0' && character <= '9') || (character >= 'a' && character <= 'f');
    }

    private static int getFontWidth(char character) {
        return switch (character) {
            case 'I', 'i', '!', '\'', ',', '.', ':', ';', '|', '`' -> 1;
            case 'l' -> 2;
            case 't', '(', ')', '[', ']', '{', '}', '"', '*', '<', '>' -> 3;
            case ' ', 'f', 'k' -> 4;
            case '@', '~' -> 6;
            default -> 5;
        };
    }
}
