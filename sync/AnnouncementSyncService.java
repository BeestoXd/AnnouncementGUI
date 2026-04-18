package com.bx.announcementGUI.sync;

import com.bx.announcementGUI.model.Announcement;

import java.util.UUID;

public interface AnnouncementSyncService {

    void start();

    void stop();

    void publishUpsert(Announcement announcement);

    void publishDelete(UUID announcementId, long version);

    void publishForceBroadcast(UUID announcementId);
}
