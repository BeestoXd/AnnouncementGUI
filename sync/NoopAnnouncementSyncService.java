package com.bx.announcementGUI.sync;

import com.bx.announcementGUI.model.Announcement;

import java.util.UUID;

public final class NoopAnnouncementSyncService implements AnnouncementSyncService {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void publishUpsert(Announcement announcement) {
    }

    @Override
    public void publishDelete(UUID announcementId, long version) {
    }

    @Override
    public void publishForceBroadcast(UUID announcementId) {
    }
}
