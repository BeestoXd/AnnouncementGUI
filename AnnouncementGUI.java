package com.bx.announcementGUI;

import com.bx.announcementGUI.command.AnnouncementCommand;
import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.gui.AnnouncementGuiService;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.repository.YamlAnnouncementRepository;
import com.bx.announcementGUI.service.AnnouncementBroadcaster;
import com.bx.announcementGUI.service.AnnouncementPanelFormatter;
import com.bx.announcementGUI.service.AnnouncementScheduler;
import com.bx.announcementGUI.service.AnnouncementService;
import com.bx.announcementGUI.service.TargetMatcher;
import com.bx.announcementGUI.sync.AnnouncementSyncListener;
import com.bx.announcementGUI.sync.AnnouncementSyncService;
import com.bx.announcementGUI.sync.NoopAnnouncementSyncService;
import com.bx.announcementGUI.sync.RedisAnnouncementSyncService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class AnnouncementGUI extends JavaPlugin implements AnnouncementSyncListener {

    private PluginSettings settings;
    private YamlAnnouncementRepository repository;
    private TargetMatcher targetMatcher;
    private AnnouncementService announcementService;
    private AnnouncementPanelFormatter panelFormatter;
    private AnnouncementBroadcaster broadcaster;
    private AnnouncementScheduler scheduler;
    private AnnouncementGuiService guiService;
    private AnnouncementSyncService syncService;
    private String activeStorageFileName;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializePlugin();
        getLogger().info("AnnouncementGUI enabled.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (syncService != null) {
            syncService.stop();
        }
    }

    public void reloadPluginState() {
        reloadConfig();
        PluginSettings newSettings = PluginSettings.fromConfig(getConfig());
        settings = newSettings;

        if (repository == null) {
            repository = new YamlAnnouncementRepository(this, settings.storageFileName());
            activeStorageFileName = settings.storageFileName();
        } else if (!activeStorageFileName.equals(settings.storageFileName())) {
            getLogger().warning("Changing storage.file at runtime is not supported. Keeping " + activeStorageFileName);
        }

        if (targetMatcher == null) {
            targetMatcher = new TargetMatcher(settings);
        } else {
            targetMatcher.reloadSettings(settings);
        }

        if (announcementService == null) {
            announcementService = new AnnouncementService(repository, settings);
        } else {
            announcementService.reloadSettings(settings);
            announcementService.reloadFromStorage();
        }

        if (panelFormatter == null) {
            panelFormatter = new AnnouncementPanelFormatter(settings);
        } else {
            panelFormatter.reloadSettings(settings);
        }

        if (broadcaster == null) {
            broadcaster = new AnnouncementBroadcaster(this, announcementService, targetMatcher, panelFormatter);
        } else {
            broadcaster.reloadSettings(settings);
        }

        if (guiService == null) {
            guiService = new AnnouncementGuiService(this, announcementService, settings);
        } else {
            guiService.reloadSettings(settings);
        }

        restartSyncService();

        if (scheduler == null) {
            scheduler = new AnnouncementScheduler(this, announcementService, broadcaster, settings);
            scheduler.start();
        } else {
            scheduler.restart(settings);
        }
    }

    public AnnouncementService getAnnouncementService() {
        return announcementService;
    }

    public AnnouncementGuiService getGuiService() {
        return guiService;
    }

    public boolean forceBroadcast(UUID announcementId) {
        boolean localBroadcast = broadcaster.broadcastNow(announcementId);
        if (settings.syncEnabled()) {
            syncService.publishForceBroadcast(announcementId);
        }
        return localBroadcast;
    }

    @Override
    public void onRemoteUpsert(Announcement announcement) {
        announcementService.applyRemoteUpsert(announcement);
    }

    @Override
    public void onRemoteDelete(UUID announcementId, long version) {
        announcementService.applyRemoteDelete(announcementId, version);
    }

    @Override
    public void onRemoteForceBroadcast(UUID announcementId) {
        broadcaster.broadcastNow(announcementId);
    }

    private void initializePlugin() {
        reloadPluginState();

        AnnouncementCommand commandExecutor = new AnnouncementCommand(this);
        PluginCommand announcementGuiCommand = getCommand("announcementgui");
        PluginCommand announcementCommand = getCommand("announcement");
        if (announcementGuiCommand != null) {
            announcementGuiCommand.setExecutor(commandExecutor);
            announcementGuiCommand.setTabCompleter(commandExecutor);
        }
        if (announcementCommand != null) {
            announcementCommand.setExecutor(commandExecutor);
            announcementCommand.setTabCompleter(commandExecutor);
        }

        getServer().getPluginManager().registerEvents(guiService, this);
    }

    private void restartSyncService() {
        if (syncService != null) {
            syncService.stop();
        }

        if (!settings.syncEnabled() || settings.syncType() == PluginSettings.SyncType.NONE) {
            syncService = new NoopAnnouncementSyncService();
        } else {
            syncService = new RedisAnnouncementSyncService(this, settings, this);
        }

        announcementService.setSyncService(syncService);
        syncService.start();
    }
}
