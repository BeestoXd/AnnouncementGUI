package com.bx.announcementGUI.service;

import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AnnouncementScheduler {

    private final JavaPlugin plugin;
    private final AnnouncementService announcementService;
    private final AnnouncementBroadcaster broadcaster;
    private BukkitTask task;
    private PluginSettings settings;
    private final Map<UUID, Long> nextRunTimes = new HashMap<>();
    private final Map<UUID, Long> knownVersions = new HashMap<>();

    public AnnouncementScheduler(
            JavaPlugin plugin,
            AnnouncementService announcementService,
            AnnouncementBroadcaster broadcaster,
            PluginSettings settings
    ) {
        this.plugin = plugin;
        this.announcementService = announcementService;
        this.broadcaster = broadcaster;
        this.settings = settings;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                settings.schedulerCheckIntervalTicks(),
                settings.schedulerCheckIntervalTicks()
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart(PluginSettings settings) {
        this.settings = settings;
        nextRunTimes.clear();
        knownVersions.clear();
        start();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        Set<UUID> seenIds = new HashSet<>();

        for (Announcement announcement : announcementService.getAnnouncements()) {
            seenIds.add(announcement.getId());
            if (!announcement.isEnabled()) {
                nextRunTimes.remove(announcement.getId());
                knownVersions.remove(announcement.getId());
                continue;
            }

            Long knownVersion = knownVersions.get(announcement.getId());
            if (knownVersion == null || knownVersion != announcement.getVersion()) {
                knownVersions.put(announcement.getId(), announcement.getVersion());
                nextRunTimes.put(announcement.getId(), now + announcement.getIntervalSeconds() * 1000L);
                continue;
            }

            long nextRun = nextRunTimes.getOrDefault(announcement.getId(), now + announcement.getIntervalSeconds() * 1000L);
            if (now < nextRun) {
                continue;
            }

            broadcaster.broadcast(announcement);
            nextRunTimes.put(announcement.getId(), now + announcement.getIntervalSeconds() * 1000L);
        }

        nextRunTimes.keySet().removeIf(id -> !seenIds.contains(id));
        knownVersions.keySet().removeIf(id -> !seenIds.contains(id));
    }
}
