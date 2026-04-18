package com.bx.announcementGUI.sync;

import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.model.TargetType;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AnnouncementPayload(
        String id,
        String name,
        String title,
        List<String> descriptionLines,
        List<String> messageLines,
        long intervalSeconds,
        String targetType,
        List<String> targets,
        boolean enabled,
        String createdBy,
        String ownerServerId,
        long createdAtEpochMilli,
        long updatedAtEpochMilli,
        long version
) {

    public static AnnouncementPayload fromAnnouncement(Announcement announcement) {
        return new AnnouncementPayload(
                announcement.getId().toString(),
                announcement.getName(),
                announcement.getTitle(),
                announcement.getDescriptionLines(),
                announcement.getMessageLines(),
                announcement.getIntervalSeconds(),
                announcement.getTargetType().name(),
                List.copyOf(announcement.getTargets()),
                announcement.isEnabled(),
                announcement.getCreatedBy(),
                announcement.getOwnerServerId(),
                announcement.getCreatedAt().toEpochMilli(),
                announcement.getUpdatedAt().toEpochMilli(),
                announcement.getVersion()
        );
    }

    public Announcement toAnnouncement() {
        Set<String> normalizedTargets = new LinkedHashSet<>(targets == null ? List.of() : targets);
        return new Announcement(
                UUID.fromString(id),
                name,
                title,
                descriptionLines == null ? List.of() : descriptionLines,
                messageLines,
                intervalSeconds,
                TargetType.valueOf(targetType),
                normalizedTargets,
                enabled,
                createdBy,
                ownerServerId,
                Instant.ofEpochMilli(createdAtEpochMilli),
                Instant.ofEpochMilli(updatedAtEpochMilli),
                version
        );
    }
}
