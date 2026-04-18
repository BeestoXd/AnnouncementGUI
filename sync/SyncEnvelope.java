package com.bx.announcementGUI.sync;

import com.bx.announcementGUI.model.Announcement;

import java.util.UUID;

public record SyncEnvelope(
        String type,
        String originServerId,
        long sentAtEpochMilli,
        String announcementId,
        long version,
        AnnouncementPayload announcement
) {

    public static SyncEnvelope upsert(String originServerId, Announcement announcement) {
        return new SyncEnvelope(
                SyncType.UPSERT.name(),
                originServerId,
                System.currentTimeMillis(),
                announcement.getId().toString(),
                announcement.getVersion(),
                AnnouncementPayload.fromAnnouncement(announcement)
        );
    }

    public static SyncEnvelope delete(String originServerId, UUID announcementId, long version) {
        return new SyncEnvelope(
                SyncType.DELETE.name(),
                originServerId,
                System.currentTimeMillis(),
                announcementId.toString(),
                version,
                null
        );
    }

    public static SyncEnvelope forceBroadcast(String originServerId, UUID announcementId) {
        return new SyncEnvelope(
                SyncType.FORCE_BROADCAST.name(),
                originServerId,
                System.currentTimeMillis(),
                announcementId.toString(),
                0L,
                null
        );
    }

    public SyncType syncType() {
        return SyncType.valueOf(type);
    }

    public enum SyncType {
        UPSERT,
        DELETE,
        FORCE_BROADCAST
    }
}
