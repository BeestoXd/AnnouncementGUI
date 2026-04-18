package com.bx.announcementGUI.service;

import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.model.TargetType;

public final class TargetMatcher {

    private PluginSettings settings;

    public TargetMatcher(PluginSettings settings) {
        this.settings = settings;
    }

    public void reloadSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public boolean matchesCurrentServer(Announcement announcement) {
        TargetType targetType = announcement.getTargetType();
        return switch (targetType) {
            case GLOBAL -> true;
            case LOCAL -> settings.serverId().equalsIgnoreCase(announcement.getOwnerServerId());
            case SERVER, SERVERS -> announcement.getTargets().stream()
                    .anyMatch(target -> target.equalsIgnoreCase(settings.serverId()));
            case GROUP -> announcement.getTargets().stream()
                    .anyMatch(target -> settings.serverGroups().contains(target.toLowerCase()));
        };
    }
}
