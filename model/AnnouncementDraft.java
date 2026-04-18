package com.bx.announcementGUI.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AnnouncementDraft {

    private String name = "";
    private String title = "";
    private List<String> descriptionLines = new ArrayList<>();
    private List<String> messageLines = new ArrayList<>();
    private long intervalSeconds = 60L;
    private TargetType targetType = TargetType.LOCAL;
    private Set<String> targets = new LinkedHashSet<>();
    private boolean enabled = true;

    public static AnnouncementDraft fromAnnouncement(Announcement announcement) {
        AnnouncementDraft draft = new AnnouncementDraft();
        draft.name = announcement.getName();
        draft.title = announcement.getTitle();
        draft.descriptionLines = new ArrayList<>(announcement.getDescriptionLines());
        draft.messageLines = new ArrayList<>(announcement.getMessageLines());
        draft.intervalSeconds = announcement.getIntervalSeconds();
        draft.targetType = announcement.getTargetType();
        draft.targets = new LinkedHashSet<>(announcement.getTargets());
        draft.enabled = announcement.isEnabled();
        return draft;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (messageLines.isEmpty()) {
            errors.add("Message is required.");
        }
        if (intervalSeconds <= 0) {
            errors.add("Interval must be greater than 0 seconds.");
        }
        if (targetType == null) {
            errors.add("Target type has not been selected.");
        } else if (targetType.requiresTargets() && targets.isEmpty()) {
            errors.add("Target server or group is required.");
        }
        return errors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title.trim();
    }

    public List<String> getDescriptionLines() {
        return List.copyOf(descriptionLines);
    }

    public void setDescriptionLines(List<String> descriptionLines) {
        this.descriptionLines = descriptionLines == null ? new ArrayList<>() : new ArrayList<>(descriptionLines);
    }

    public List<String> getMessageLines() {
        return List.copyOf(messageLines);
    }

    public void setMessageLines(List<String> messageLines) {
        this.messageLines = messageLines == null ? new ArrayList<>() : new ArrayList<>(messageLines);
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public Set<String> getTargets() {
        return Set.copyOf(targets);
    }

    public void setTargets(Set<String> targets) {
        this.targets = targets == null ? new LinkedHashSet<>() : new LinkedHashSet<>(targets);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
