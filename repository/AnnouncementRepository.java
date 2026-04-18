package com.bx.announcementGUI.repository;

import com.bx.announcementGUI.model.Announcement;

import java.util.Map;
import java.util.UUID;

public interface AnnouncementRepository {

    Map<UUID, Announcement> loadAll();

    void save(Announcement announcement);

    void delete(UUID announcementId);
}
