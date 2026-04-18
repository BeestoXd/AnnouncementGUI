package com.bx.announcementGUI.service;

import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.util.ColorUtil;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public final class AnnouncementPanelFormatter {

    private PluginSettings settings;

    public AnnouncementPanelFormatter(PluginSettings settings) {
        this.settings = settings;
    }

    public void reloadSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public List<String> render(Announcement announcement) {
        List<String> rendered = new ArrayList<>();
        addIfNotBlank(rendered, settings.panelFormat().topBorder());

        String title = applyPlaceholders(announcement.getPanelTitle());
        if (!title.isBlank()) {
            rendered.add(ColorUtil.centerLine(title));
        }

        for (String descriptionLine : announcement.getDescriptionLines()) {
            String line = applyPlaceholders(descriptionLine);
            if (!line.isBlank()) {
                rendered.add(ColorUtil.centerLine(line));
            }
        }

        if (announcement.hasHeaderContent() && !announcement.getMessageLines().isEmpty()) {
            switch (settings.panelFormat().bodySeparatorMode()) {
                case DIVIDER -> addIfNotBlank(rendered, settings.panelFormat().bodyDivider());
                case BLANK -> rendered.add("");
                case NONE -> {
                }
            }
        }

        for (String bodyLine : announcement.getMessageLines()) {
            rendered.add(ColorUtil.applyCenterTag(applyPlaceholders(bodyLine)));
        }

        addIfNotBlank(rendered, settings.panelFormat().bottomBorder());
        return rendered;
    }

    private String applyPlaceholders(String value) {
        return value
                .replace("%server%", settings.serverId())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
    }

    private void addIfNotBlank(List<String> rendered, String line) {
        if (line != null && !line.isBlank()) {
            rendered.add(line);
        }
    }
}
