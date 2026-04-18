package com.bx.announcementGUI.service;

import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.model.AnnouncementDraft;
import com.bx.announcementGUI.repository.AnnouncementRepository;
import com.bx.announcementGUI.sync.AnnouncementSyncService;
import com.bx.announcementGUI.sync.NoopAnnouncementSyncService;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnnouncementService {

    private static final Comparator<Announcement> SORT_ORDER =
            Comparator.comparing(Announcement::getSortName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Announcement::getId);

    private final AnnouncementRepository repository;
    private final Map<UUID, Announcement> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tombstones = new ConcurrentHashMap<>();

    private PluginSettings settings;
    private AnnouncementSyncService syncService = new NoopAnnouncementSyncService();

    public AnnouncementService(AnnouncementRepository repository, PluginSettings settings) {
        this.repository = repository;
        this.settings = settings;
        reloadFromStorage();
    }

    public void reloadSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public void setSyncService(AnnouncementSyncService syncService) {
        this.syncService = syncService == null ? new NoopAnnouncementSyncService() : syncService;
    }

    public void reloadFromStorage() {
        cache.clear();
        cache.putAll(repository.loadAll());
    }

    public List<Announcement> getAnnouncements() {
        return cache.values().stream().sorted(SORT_ORDER).toList();
    }

    public Optional<Announcement> findById(UUID id) {
        return Optional.ofNullable(cache.get(id));
    }

    public Optional<Announcement> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            return findById(UUID.fromString(token));
        } catch (IllegalArgumentException ignored) {
            String normalized = token.toLowerCase(Locale.ROOT);
            return getAnnouncements().stream()
                    .filter(announcement -> announcement.matchesIdPrefix(normalized))
                    .findFirst();
        }
    }

    public Announcement create(AnnouncementDraft draft, String actor) {
        return upsert(null, draft, actor, true);
    }

    public Announcement update(UUID announcementId, AnnouncementDraft draft, String actor) {
        return upsert(announcementId, draft, actor, true);
    }

    public Announcement upsert(UUID existingId, AnnouncementDraft draft, String actor, boolean publishSync) {
        List<String> errors = draft.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }

        Announcement existing = existingId == null ? null : cache.get(existingId);
        Instant now = Instant.now();
        Announcement announcement;

        if (existing == null) {
            announcement = new Announcement(
                    UUID.randomUUID(),
                    draft.getName(),
                    draft.getTitle(),
                    draft.getDescriptionLines(),
                    draft.getMessageLines(),
                    draft.getIntervalSeconds(),
                    draft.getTargetType(),
                    draft.getTargets(),
                    draft.isEnabled(),
                    actor,
                    settings.serverId(),
                    now,
                    now,
                    1L
            );
        } else {
            announcement = new Announcement(
                    existing.getId(),
                    draft.getName(),
                    draft.getTitle(),
                    draft.getDescriptionLines(),
                    draft.getMessageLines(),
                    draft.getIntervalSeconds(),
                    draft.getTargetType(),
                    draft.getTargets(),
                    draft.isEnabled(),
                    existing.getCreatedBy(),
                    existing.getOwnerServerId(),
                    existing.getCreatedAt(),
                    now,
                    existing.getVersion() + 1L
            );
        }

        repository.save(announcement);
        cache.put(announcement.getId(), announcement);
        tombstones.remove(announcement.getId());
        if (publishSync) {
            syncService.publishUpsert(announcement);
        }
        return announcement;
    }

    public boolean delete(UUID announcementId, boolean publishSync) {
        Announcement existing = cache.remove(announcementId);
        if (existing == null) {
            return false;
        }

        long deleteVersion = existing.getVersion() + 1L;
        repository.delete(announcementId);
        tombstones.put(announcementId, deleteVersion);
        if (publishSync) {
            syncService.publishDelete(announcementId, deleteVersion);
        }
        return true;
    }

    public void applyRemoteUpsert(Announcement remoteAnnouncement) {
        Long tombstoneVersion = tombstones.get(remoteAnnouncement.getId());
        if (tombstoneVersion != null && remoteAnnouncement.getVersion() <= tombstoneVersion) {
            return;
        }

        Announcement localAnnouncement = cache.get(remoteAnnouncement.getId());
        if (localAnnouncement != null && localAnnouncement.getVersion() > remoteAnnouncement.getVersion()) {
            return;
        }

        repository.save(remoteAnnouncement);
        cache.put(remoteAnnouncement.getId(), remoteAnnouncement);
    }

    public void applyRemoteDelete(UUID announcementId, long deleteVersion) {
        Announcement localAnnouncement = cache.get(announcementId);
        if (localAnnouncement != null && localAnnouncement.getVersion() > deleteVersion) {
            return;
        }

        repository.delete(announcementId);
        cache.remove(announcementId);
        tombstones.merge(announcementId, deleteVersion, Math::max);
    }

    public Map<UUID, Announcement> snapshot() {
        return new LinkedHashMap<>(cache);
    }
}
