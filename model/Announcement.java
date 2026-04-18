package com.bx.announcementGUI.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Announcement {

    public static final Comparator<Announcement> DISPLAY_ORDER =
            Comparator.comparing(Announcement::getSortName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Announcement::getId);

    private final UUID id;
    private final String name;
    private final String title;
    private final List<String> descriptionLines;
    private final List<String> messageLines;
    private final long intervalSeconds;
    private final TargetType targetType;
    private final Set<String> targets;
    private final boolean enabled;
    private final String createdBy;
    private final String ownerServerId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;

    public Announcement(
            UUID id,
            String name,
            String title,
            List<String> descriptionLines,
            List<String> messageLines,
            long intervalSeconds,
            TargetType targetType,
            Set<String> targets,
            boolean enabled,
            String createdBy,
            String ownerServerId,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = sanitize(name);
        this.title = sanitize(title);
        this.descriptionLines = List.copyOf(normalizeLines(descriptionLines));
        this.messageLines = List.copyOf(normalizeLines(messageLines));
        this.intervalSeconds = intervalSeconds;
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.targets = Set.copyOf(normalizeTargets(targets));
        this.enabled = enabled;
        this.createdBy = sanitize(createdBy);
        this.ownerServerId = sanitize(ownerServerId);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getDescriptionLines() {
        return descriptionLines;
    }

    public List<String> getMessageLines() {
        return messageLines;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public Set<String> getTargets() {
        return targets;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getOwnerServerId() {
        return ownerServerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public String getDisplayName() {
        if (!name.isBlank()) {
            return name;
        }
        if (!title.isBlank()) {
            return title;
        }
        return "Announcement " + id.toString().substring(0, 8);
    }

    public String getPanelTitle() {
        if (!title.isBlank()) {
            return title;
        }
        if (!name.isBlank()) {
            return name;
        }
        return "";
    }

    public boolean hasHeaderContent() {
        return !getPanelTitle().isBlank() || !descriptionLines.isEmpty();
    }

    public String getSortName() {
        return getDisplayName().toLowerCase();
    }

    public String getTargetSummary() {
        return switch (targetType) {
            case LOCAL -> "LOCAL (" + ownerServerId + ")";
            case GLOBAL -> "GLOBAL";
            case SERVER, SERVERS, GROUP -> targetType.name() + " " + String.join(", ", targets);
        };
    }

    public boolean matchesIdPrefix(String value) {
        String normalized = value.toLowerCase();
        return id.toString().startsWith(normalized) || getDisplayName().toLowerCase().contains(normalized);
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeLines(List<String> lines) {
        List<String> normalized = new ArrayList<>();
        if (lines == null) {
            return normalized;
        }
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                normalized.add(line.trim());
            }
        }
        return normalized;
    }

    private static Set<String> normalizeTargets(Set<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase());
            }
        }
        return normalized;
    }
}
