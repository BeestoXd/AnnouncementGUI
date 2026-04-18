package com.bx.announcementGUI.sync;

import com.bx.announcementGUI.model.Announcement;

import java.util.UUID;

public interface AnnouncementSyncListener {

    void onRemoteUpsert(Announcement announcement);

    void onRemoteDelete(UUID announcementId, long version);

    void onRemoteForceBroadcast(UUID announcementId);
}
