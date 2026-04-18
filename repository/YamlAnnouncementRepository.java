package com.bx.announcementGUI.repository;

import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.model.TargetType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class YamlAnnouncementRepository implements AnnouncementRepository {

    private final JavaPlugin plugin;
    private final File file;

    public YamlAnnouncementRepository(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder.");
        }
        this.file = new File(plugin.getDataFolder(), fileName);
        ensureFile();
    }

    @Override
    public synchronized Map<UUID, Announcement> loadAll() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("announcements");
        Map<UUID, Announcement> loaded = new LinkedHashMap<>();
        if (root == null) {
            return loaded;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            try {
                UUID id = UUID.fromString(key);
                List<String> messageLines = section.getStringList("message-lines");
                Set<String> targets = new LinkedHashSet<>(section.getStringList("targets"));
                Announcement announcement = new Announcement(
                        id,
                        section.getString("name", ""),
                        section.getString("title", ""),
                        section.getStringList("description-lines"),
                        messageLines,
                        Math.max(1L, section.getLong("interval-seconds", 60L)),
                        TargetType.valueOf(section.getString("target-type", TargetType.LOCAL.name())),
                        targets,
                        section.getBoolean("enabled", true),
                        section.getString("created-by", "console"),
                        section.getString("owner-server-id", "server-1"),
                        Instant.ofEpochMilli(section.getLong("created-at", System.currentTimeMillis())),
                        Instant.ofEpochMilli(section.getLong("updated-at", System.currentTimeMillis())),
                        Math.max(1L, section.getLong("version", 1L))
                );
                loaded.put(id, announcement);
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to load announcement " + key + ": " + exception.getMessage());
            }
        }
        return loaded;
    }

    @Override
    public synchronized void save(Announcement announcement) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String path = "announcements." + announcement.getId();
        yaml.set(path + ".name", announcement.getName());
        yaml.set(path + ".title", announcement.getTitle());
        yaml.set(path + ".description-lines", new ArrayList<>(announcement.getDescriptionLines()));
        yaml.set(path + ".message-lines", new ArrayList<>(announcement.getMessageLines()));
        yaml.set(path + ".interval-seconds", announcement.getIntervalSeconds());
        yaml.set(path + ".target-type", announcement.getTargetType().name());
        yaml.set(path + ".targets", new ArrayList<>(announcement.getTargets()));
        yaml.set(path + ".enabled", announcement.isEnabled());
        yaml.set(path + ".created-by", announcement.getCreatedBy());
        yaml.set(path + ".owner-server-id", announcement.getOwnerServerId());
        yaml.set(path + ".created-at", announcement.getCreatedAt().toEpochMilli());
        yaml.set(path + ".updated-at", announcement.getUpdatedAt().toEpochMilli());
        yaml.set(path + ".version", announcement.getVersion());
        saveYaml(yaml);
    }

    @Override
    public synchronized void delete(UUID announcementId) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("announcements." + announcementId, null);
        saveYaml(yaml);
    }

    private void ensureFile() {
        if (file.exists()) {
            return;
        }
        try {
            if (!file.createNewFile()) {
                plugin.getLogger().warning("Failed to create " + file.getName());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create storage file " + file.getAbsolutePath(), exception);
        }
    }

    private void saveYaml(YamlConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save announcements to " + file.getAbsolutePath(), exception);
        }
    }
}
