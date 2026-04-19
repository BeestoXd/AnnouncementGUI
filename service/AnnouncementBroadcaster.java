package com.bx.announcementGUI.service;

import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

public final class AnnouncementBroadcaster {

    private final JavaPlugin plugin;
    private final AnnouncementService announcementService;
    private final TargetMatcher targetMatcher;
    private final AnnouncementPanelFormatter panelFormatter;

    public AnnouncementBroadcaster(
            JavaPlugin plugin,
            AnnouncementService announcementService,
            TargetMatcher targetMatcher,
            AnnouncementPanelFormatter panelFormatter
    ) {
        this.plugin = plugin;
        this.announcementService = announcementService;
        this.targetMatcher = targetMatcher;
        this.panelFormatter = panelFormatter;
    }

    public void reloadSettings(PluginSettings settings) {
        this.panelFormatter.reloadSettings(settings);
    }

    public boolean broadcastNow(UUID announcementId) {
        Optional<Announcement> announcement = announcementService.findById(announcementId);
        return announcement.filter(this::broadcast).isPresent();
    }

    public boolean broadcast(Announcement announcement) {
        if (!announcement.isEnabled()) {
            return false;
        }
        if (!targetMatcher.matchesCurrentServer(announcement)) {
            return false;
        }

        boolean sent = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String rawLine : panelFormatter.render(announcement, player)) {
                player.sendMessage(ColorUtil.colorize(rawLine));
                sent = true;
            }
        }
        if (!sent) {
            plugin.getLogger().fine("No online players for announcement " + announcement.getId());
        }
        return true;
    }
}
