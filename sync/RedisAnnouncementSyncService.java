package com.bx.announcementGUI.sync;

import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RedisAnnouncementSyncService implements AnnouncementSyncService {

    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final AnnouncementSyncListener listener;
    private final Gson gson = new Gson();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread subscriberThread;
    private JedisPubSub subscriber;

    public RedisAnnouncementSyncService(JavaPlugin plugin, PluginSettings settings, AnnouncementSyncListener listener) {
        this.plugin = plugin;
        this.settings = settings;
        this.listener = listener;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleMessage(message);
            }
        };

        subscriberThread = new Thread(() -> {
            try (Jedis jedis = new Jedis(settings.redisUri())) {
                jedis.subscribe(subscriber, settings.redisChannel());
            } catch (Exception exception) {
                if (running.get()) {
                    plugin.getLogger().warning("Announcement sync subscriber stopped: " + exception.getMessage());
                }
            }
        }, "AnnouncementGUI-RedisSync");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
        plugin.getLogger().info("Announcement sync enabled using Redis channel " + settings.redisChannel());
    }

    @Override
    public void stop() {
        running.set(false);
        if (subscriber != null) {
            try {
                subscriber.unsubscribe();
            } catch (Exception ignored) {
            }
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
            subscriberThread = null;
        }
        subscriber = null;
    }

    @Override
    public void publishUpsert(Announcement announcement) {
        publish(SyncEnvelope.upsert(settings.serverId(), announcement));
    }

    @Override
    public void publishDelete(UUID announcementId, long version) {
        publish(SyncEnvelope.delete(settings.serverId(), announcementId, version));
    }

    @Override
    public void publishForceBroadcast(UUID announcementId) {
        publish(SyncEnvelope.forceBroadcast(settings.serverId(), announcementId));
    }

    private void publish(SyncEnvelope envelope) {
        if (!running.get()) {
            return;
        }

        try (Jedis jedis = new Jedis(settings.redisUri())) {
            jedis.publish(settings.redisChannel(), gson.toJson(envelope));
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to publish announcement sync event: " + exception.getMessage());
        }
    }

    private void handleMessage(String rawMessage) {
        try {
            SyncEnvelope envelope = gson.fromJson(rawMessage, SyncEnvelope.class);
            if (envelope == null || settings.serverId().equalsIgnoreCase(envelope.originServerId())) {
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                switch (envelope.syncType()) {
                    case UPSERT -> {
                        if (envelope.announcement() != null) {
                            listener.onRemoteUpsert(envelope.announcement().toAnnouncement());
                        }
                    }
                    case DELETE -> listener.onRemoteDelete(UUID.fromString(envelope.announcementId()), envelope.version());
                    case FORCE_BROADCAST -> listener.onRemoteForceBroadcast(UUID.fromString(envelope.announcementId()));
                }
            });
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to handle sync message: " + exception.getMessage());
        }
    }
}
