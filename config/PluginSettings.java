package com.bx.announcementGUI.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record PluginSettings(
        String serverId,
        Set<String> serverGroups,
        String storageFileName,
        boolean syncEnabled,
        SyncType syncType,
        String redisUri,
        String redisChannel,
        long schedulerCheckIntervalTicks,
        PanelFormat panelFormat,
        GuiTitles guiTitles
) {

    public static PluginSettings fromConfig(FileConfiguration config) {
        String serverId = config.getString("server.id", "server-1").trim().toLowerCase(Locale.ROOT);
        Set<String> groups = normalize(config.getStringList("server.groups"));
        String storageFileName = config.getString("storage.file", "announcements.yml").trim();
        boolean syncEnabled = config.getBoolean("sync.enabled", false);
        SyncType syncType = SyncType.from(config.getString("sync.type", "REDIS"));
        String redisUri = config.getString("sync.redis.uri", "redis://127.0.0.1:6379/0").trim();
        String redisChannel = config.getString("sync.redis.channel", "announcementgui:sync").trim();
        long schedulerCheckIntervalTicks = Math.max(20L, config.getLong("scheduler.check-interval-ticks", 20L));
        PanelFormat panelFormat = new PanelFormat(
                config.getString("formats.panel.top-border", "&6&m------------------------------------------------"),
                config.getString("formats.panel.body-divider", "&6&m------------------------------------------------"),
                config.getString("formats.panel.bottom-border", "&6&m------------------------------------------------"),
                PanelSeparatorMode.from(config.getString("formats.panel.body-separator-mode", "DIVIDER"))
        );
        GuiTitles titles = new GuiTitles(
                config.getString("gui.titles.main", "&0Announcement Manager"),
                config.getString("gui.titles.create", "&0Create Announcement"),
                config.getString("gui.titles.edit", "&0Edit Announcement"),
                config.getString("gui.titles.edit-list", "&0Edit Announcements"),
                config.getString("gui.titles.delete-list", "&0Delete Announcements"),
                config.getString("gui.titles.delete-confirm", "&0Confirm Deletion")
        );
        return new PluginSettings(
                serverId,
                groups,
                storageFileName,
                syncEnabled,
                syncType,
                redisUri,
                redisChannel,
                schedulerCheckIntervalTicks,
                panelFormat,
                titles
        );
    }

    private static Set<String> normalize(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    public enum SyncType {
        REDIS,
        NONE;

        public static SyncType from(String value) {
            if (value == null || value.isBlank()) {
                return REDIS;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return REDIS;
            }
        }
    }

    public record GuiTitles(
            String main,
            String create,
            String edit,
            String editList,
            String deleteList,
            String deleteConfirm
    ) {
    }

    public record PanelFormat(
            String topBorder,
            String bodyDivider,
            String bottomBorder,
            PanelSeparatorMode bodySeparatorMode
    ) {
    }

    public enum PanelSeparatorMode {
        DIVIDER,
        BLANK,
        NONE;

        public static PanelSeparatorMode from(String value) {
            if (value == null || value.isBlank()) {
                return DIVIDER;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return DIVIDER;
            }
        }
    }
}
