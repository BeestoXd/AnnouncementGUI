package com.bx.announcementGUI.gui;

import com.bx.announcementGUI.AnnouncementGUI;
import com.bx.announcementGUI.config.PluginSettings;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.model.AnnouncementDraft;
import com.bx.announcementGUI.model.TargetType;
import com.bx.announcementGUI.service.AnnouncementService;
import com.bx.announcementGUI.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnnouncementGuiService implements Listener {

    private static final int LIST_PAGE_SIZE = 45;

    private final AnnouncementGUI plugin;
    private final AnnouncementService announcementService;
    private final Map<UUID, MenuView> menuViews = new ConcurrentHashMap<>();
    private final Map<UUID, EditorSession> editorSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Conversation> activeConversations = new ConcurrentHashMap<>();

    private PluginSettings settings;

    public AnnouncementGuiService(AnnouncementGUI plugin, AnnouncementService announcementService, PluginSettings settings) {
        this.plugin = plugin;
        this.announcementService = announcementService;
        this.settings = settings;
    }

    public void reloadSettings(PluginSettings settings) {
        this.settings = settings;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 27, color(settings.guiTitles().main()));
        inventory.setItem(11, item(Material.LIME_DYE, "&a&lCreate Announcement", List.of(
                "&7Create a new announcement",
                "&7with local/server/group/global targets."
        )));
        inventory.setItem(13, item(Material.WRITABLE_BOOK, "&e&lEdit Announcements", List.of(
                "&7View and edit all",
                "&7saved announcements."
        )));
        inventory.setItem(15, item(Material.BARRIER, "&c&lDelete Announcements", List.of(
                "&7Remove announcements",
                "&7that are no longer needed."
        )));
        inventory.setItem(22, item(Material.COMPASS, "&7Close", List.of("&8Click to close the menu")));
        player.openInventory(inventory);
        menuViews.put(player.getUniqueId(), new MenuView(MenuType.MAIN, 0, null));
    }

    public void openCreate(Player player) {
        editorSessions.put(player.getUniqueId(), new EditorSession(null, new AnnouncementDraft()));
        openEditor(player);
    }

    public void openEditor(Player player, UUID announcementId) {
        Announcement announcement = announcementService.findById(announcementId).orElse(null);
        if (announcement == null) {
            player.sendMessage(color("&cAnnouncement not found."));
            openEditList(player, 0);
            return;
        }
        editorSessions.put(player.getUniqueId(), new EditorSession(announcement.getId(), AnnouncementDraft.fromAnnouncement(announcement)));
        openEditor(player);
    }

    public void openEditList(Player player, int page) {
        openList(player, MenuType.EDIT_LIST, page);
    }

    public void openDeleteList(Player player, int page) {
        openList(player, MenuType.DELETE_LIST, page);
    }

    public void openDeleteConfirm(Player player, UUID announcementId) {
        Announcement announcement = announcementService.findById(announcementId).orElse(null);
        if (announcement == null) {
            player.sendMessage(color("&cAnnouncement not found."));
            openDeleteList(player, 0);
            return;
        }

        Inventory inventory = Bukkit.createInventory(player, 27, color(settings.guiTitles().deleteConfirm()));
        inventory.setItem(11, item(Material.LIME_CONCRETE, "&a&lConfirm Delete", List.of(
                "&7About to delete:",
                "&f" + announcement.getDisplayName(),
                "&8ID: " + announcement.getId().toString().substring(0, 8)
        )));
        inventory.setItem(15, item(Material.RED_CONCRETE, "&c&lCancel", List.of(
                "&7Cancel deletion",
                "&7and return to the list."
        )));
        player.openInventory(inventory);
        menuViews.put(player.getUniqueId(), new MenuView(MenuType.DELETE_CONFIRM, 0, announcementId));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        MenuView menuView = menuViews.get(player.getUniqueId());
        if (menuView == null) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }

        event.setCancelled(true);
        switch (menuView.type()) {
            case MAIN -> handleMainClick(player, event.getSlot());
            case EDITOR -> handleEditorClick(player, event.getSlot(), event.isRightClick());
            case EDIT_LIST -> handleListClick(player, MenuType.EDIT_LIST, menuView.page(), event.getSlot());
            case DELETE_LIST -> handleListClick(player, MenuType.DELETE_LIST, menuView.page(), event.getSlot());
            case DELETE_CONFIRM -> handleDeleteConfirmClick(player, menuView.targetAnnouncementId(), event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (menuViews.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            menuViews.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Conversation conversation = activeConversations.remove(playerId);
        if (conversation != null) {
            event.getPlayer().abandonConversation(conversation);
        }
        menuViews.remove(playerId);
        editorSessions.remove(playerId);
    }

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case 11 -> {
                if (requirePermission(player, "announcementgui.create")) {
                    openCreate(player);
                }
            }
            case 13 -> {
                if (requirePermission(player, "announcementgui.edit")) {
                    openEditList(player, 0);
                }
            }
            case 15 -> {
                if (requirePermission(player, "announcementgui.delete")) {
                    openDeleteList(player, 0);
                }
            }
            case 22 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleEditorClick(Player player, int slot, boolean rightClick) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            openMain(player);
            return;
        }

        AnnouncementDraft draft = session.draft();
        switch (slot) {
            case 10 -> beginPrompt(player, PromptField.NAME,
                    "&eType the internal announcement name in chat. Type &ccancel &eto abort.");
            case 12 -> beginPrompt(player, PromptField.TITLE,
                    "&eType the announcement title in chat. Type &ccancel &eto abort.");
            case 14 -> beginPrompt(player, PromptField.DESCRIPTION,
                    "&eType the description. Use &f| &eas a separator for multiple lines. Type &ccancel &eto abort.");
            case 16 -> beginPrompt(player, PromptField.MESSAGE,
                    "&eType the message. Use &f| &eas a separator for multiple lines. Type &ccancel &eto abort.");
            case 21 -> beginPrompt(player, PromptField.INTERVAL,
                    "&eType the interval in seconds. Type &ccancel &eto abort.");
            case 23 -> {
                if (rightClick) {
                    if (draft.getTargetType().requiresTargets()) {
                        beginPrompt(player, PromptField.TARGETS,
                                "&eType targets separated by commas, for example: &fserver-1,server-2");
                    } else {
                        player.sendMessage(color("&7The current target type does not require additional target values."));
                    }
                } else {
                    TargetType nextType = draft.getTargetType().next();
                    draft.setTargetType(nextType);
                    if (!nextType.requiresTargets()) {
                        draft.setTargets(Set.of());
                    }
                    openEditor(player);
                }
            }
            case 25 -> {
                draft.setEnabled(!draft.isEnabled());
                openEditor(player);
            }
            case 30 -> openMain(player);
            case 34 -> saveAnnouncement(player, session);
            default -> {
            }
        }
    }

    private void handleListClick(Player player, MenuType mode, int page, int slot) {
        if (slot >= 0 && slot < LIST_PAGE_SIZE) {
            Announcement announcement = getAnnouncementForSlot(page, slot);
            if (announcement == null) {
                return;
            }
            if (mode == MenuType.EDIT_LIST) {
                openEditor(player, announcement.getId());
            } else {
                openDeleteConfirm(player, announcement.getId());
            }
            return;
        }

        switch (slot) {
            case 45 -> openList(player, mode, Math.max(0, page - 1));
            case 49 -> openMain(player);
            case 53 -> openList(player, mode, page + 1);
            default -> {
            }
        }
    }

    private void handleDeleteConfirmClick(Player player, UUID announcementId, int slot) {
        if (slot == 11) {
            if (announcementService.delete(announcementId, true)) {
                player.sendMessage(color("&aAnnouncement deleted successfully."));
            } else {
                player.sendMessage(color("&cAnnouncement not found."));
            }
            openDeleteList(player, 0);
            return;
        }
        if (slot == 15) {
            openDeleteList(player, 0);
        }
    }

    private void handlePrompt(Player player, PromptField promptField, String input) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(color("&cEditor session not found."));
            openMain(player);
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(color("&7Input cancelled."));
            openEditor(player);
            return;
        }

        AnnouncementDraft draft = session.draft();
        switch (promptField) {
            case NAME -> draft.setName(input);
            case TITLE -> draft.setTitle(input);
            case DESCRIPTION -> draft.setDescriptionLines(parseMessageLines(input));
            case MESSAGE -> draft.setMessageLines(parseMessageLines(input));
            case INTERVAL -> {
                try {
                    long seconds = Long.parseLong(input);
                    if (seconds <= 0) {
                        throw new NumberFormatException("Interval must be positive");
                    }
                    draft.setIntervalSeconds(seconds);
                } catch (NumberFormatException exception) {
                    player.sendMessage(color("&cInterval must be a positive number."));
                }
            }
            case TARGETS -> draft.setTargets(parseTargets(input));
        }
        openEditor(player);
    }

    private void saveAnnouncement(Player player, EditorSession session) {
        try {
            if (session.announcementId() == null) {
                Announcement announcement = announcementService.create(session.draft(), player.getName());
                player.sendMessage(color("&aAnnouncement created successfully: &f" + announcement.getDisplayName()));
            } else {
                Announcement announcement = announcementService.update(session.announcementId(), session.draft(), player.getName());
                player.sendMessage(color("&aAnnouncement updated successfully: &f" + announcement.getDisplayName()));
            }
            editorSessions.remove(player.getUniqueId());
            openMain(player);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(color("&c" + exception.getMessage()));
            openEditor(player);
        }
    }

    private void openEditor(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            openMain(player);
            return;
        }

        AnnouncementDraft draft = session.draft();
        Inventory inventory = Bukkit.createInventory(player, 36, color(settings.guiTitles().edit()));
        inventory.setItem(10, item(Material.NAME_TAG, "&b&lName", List.of(
                "&7Current: &f" + displayValue(draft.getName()),
                "&8Internal list/debug name"
        )));
        inventory.setItem(12, item(Material.GOLD_INGOT, "&6&lTitle", buildTitleLore(draft)));
        inventory.setItem(14, item(Material.FEATHER, "&f&lDescription", buildDescriptionLore(draft)));
        inventory.setItem(16, item(Material.BOOK, "&e&lMessage Body", buildMessageLore(draft)));
        inventory.setItem(21, item(Material.CLOCK, "&d&lInterval", List.of(
                "&7Current: &f" + draft.getIntervalSeconds() + " seconds",
                "&8Click to edit in chat"
        )));
        inventory.setItem(23, item(Material.COMPASS, "&9&lTarget", buildTargetLore(draft)));
        inventory.setItem(25, item(
                draft.isEnabled() ? Material.SLIME_BALL : Material.FIRE_CHARGE,
                draft.isEnabled() ? "&a&lEnabled" : "&c&lDisabled",
                List.of("&7Click to toggle active status")
        ));
        inventory.setItem(30, item(Material.BARRIER, "&c&lCancel", List.of(
                "&7Return to the main menu"
        )));
        inventory.setItem(32, item(Material.PAPER, "&7&lPanel Preview", buildPreviewLore(draft)));
        inventory.setItem(34, item(Material.EMERALD_BLOCK, "&a&lSave Announcement", buildSaveLore(draft)));
        player.openInventory(inventory);
        menuViews.put(player.getUniqueId(), new MenuView(MenuType.EDITOR, 0, session.announcementId()));
    }

    private void openList(Player player, MenuType mode, int requestedPage) {
        List<Announcement> announcements = announcementService.getAnnouncements();
        int maxPage = Math.max(0, (announcements.size() - 1) / LIST_PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        Inventory inventory = Bukkit.createInventory(player, 54, color(
                mode == MenuType.EDIT_LIST ? settings.guiTitles().editList() : settings.guiTitles().deleteList()
        ));

        int startIndex = page * LIST_PAGE_SIZE;
        int endIndex = Math.min(startIndex + LIST_PAGE_SIZE, announcements.size());
        for (int index = startIndex; index < endIndex; index++) {
            Announcement announcement = announcements.get(index);
            int slot = index - startIndex;
            inventory.setItem(slot, item(
                    mode == MenuType.EDIT_LIST ? Material.PAPER : Material.BARRIER,
                    (mode == MenuType.EDIT_LIST ? "&e" : "&c") + announcement.getDisplayName(),
                    buildListLore(announcement, mode)
            ));
        }

        if (announcements.isEmpty()) {
            inventory.setItem(22, item(Material.GRAY_DYE, "&7No announcements yet", List.of(
                    "&8Create a new announcement from the main menu."
            )));
        }

        inventory.setItem(45, item(Material.ARROW, "&7Previous Page", List.of("&8Go to the previous page")));
        inventory.setItem(49, item(Material.COMPASS, "&7Back", List.of("&8Return to the main menu")));
        inventory.setItem(53, item(Material.ARROW, "&7Next Page", List.of("&8Go to the next page")));
        player.openInventory(inventory);
        menuViews.put(player.getUniqueId(), new MenuView(mode, page, null));
    }

    private void beginPrompt(Player player, PromptField promptField, String instruction) {
        player.closeInventory();
        Conversation existing = activeConversations.remove(player.getUniqueId());
        if (existing != null) {
            player.abandonConversation(existing);
        }

        Conversation conversation = new ConversationFactory(plugin)
                .withModality(true)
                .withLocalEcho(false)
                .withEscapeSequence("cancel")
                .withFirstPrompt(new ChatInputPrompt(promptField, instruction))
                .addConversationAbandonedListener(event -> onConversationAbandoned(player, event))
                .buildConversation(player);

        activeConversations.put(player.getUniqueId(), conversation);
        player.beginConversation(conversation);
    }

    private void onConversationAbandoned(Player player, ConversationAbandonedEvent event) {
        activeConversations.remove(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        if (!event.gracefulExit()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(color("&7Input cancelled."));
                openEditor(player);
            });
        }
    }

    private List<String> buildMessageLore(AnnouncementDraft draft) {
        List<String> lore = new ArrayList<>();
        if (draft.getMessageLines().isEmpty()) {
            lore.add("&7Current: &cNot set");
        } else {
            lore.add("&7Current:");
            draft.getMessageLines().stream().limit(3).forEach(line -> lore.add("&f- " + line));
            if (draft.getMessageLines().size() > 3) {
                lore.add("&8+" + (draft.getMessageLines().size() - 3) + " line(s)");
            }
        }
        lore.add("&8Click to edit in chat");
        return lore;
    }

    private List<String> buildTitleLore(AnnouncementDraft draft) {
        return List.of(
                "&7Current: &f" + displayValue(draft.getTitle()),
                "&8Centered header title"
        );
    }

    private List<String> buildDescriptionLore(AnnouncementDraft draft) {
        List<String> lore = new ArrayList<>();
        if (draft.getDescriptionLines().isEmpty()) {
            lore.add("&7Current: &cNot set");
        } else {
            lore.add("&7Current:");
            draft.getDescriptionLines().stream().limit(2).forEach(line -> lore.add("&f- " + line));
            if (draft.getDescriptionLines().size() > 2) {
                lore.add("&8+" + (draft.getDescriptionLines().size() - 2) + " line(s)");
            }
        }
        lore.add("&8Centered description lines");
        return lore;
    }

    private List<String> buildTargetLore(AnnouncementDraft draft) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Type: &f" + draft.getTargetType().name());
        lore.add("&7Values: &f" + (draft.getTargets().isEmpty() ? "-" : String.join(", ", draft.getTargets())));
        lore.add("&8Left click: cycle target type");
        lore.add("&8Right click: set target values");
        return lore;
    }

    private List<String> buildPreviewLore(AnnouncementDraft draft) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Broadcast preview layout:");
        lore.add("&6&m------------------------------");
        if (!draft.getTitle().isBlank()) {
            lore.add("&f" + draft.getTitle());
        }
        if (!draft.getDescriptionLines().isEmpty()) {
            draft.getDescriptionLines().stream().limit(2).forEach(line -> lore.add("&7" + line));
        }
        if (!draft.getMessageLines().isEmpty()) {
            lore.add("&8...");
            draft.getMessageLines().stream().limit(2).forEach(line -> lore.add("&f" + line));
        }
        lore.add("&6&m------------------------------");
        lore.add("&8Supports legacy colors like &a, &b");
        lore.add("&8Supports hex colors like &#55FFFF");
        return lore;
    }

    private List<String> buildSaveLore(AnnouncementDraft draft) {
        List<String> errors = draft.validate();
        if (errors.isEmpty()) {
            return List.of("&7All fields are valid.", "&8Click to save.");
        }

        List<String> lore = new ArrayList<>();
        lore.add("&cSome fields are still invalid:");
        errors.forEach(error -> lore.add("&f- " + error));
        return lore;
    }

    private List<String> buildListLore(Announcement announcement, MenuType mode) {
        List<String> lore = new ArrayList<>();
        lore.add("&7ID: &f" + announcement.getId().toString().substring(0, 8));
        lore.add("&7Title: &f" + displayValue(announcement.getTitle()));
        lore.add("&7Status: " + (announcement.isEnabled() ? "&aEnabled" : "&cDisabled"));
        lore.add("&7Interval: &f" + announcement.getIntervalSeconds() + "s");
        lore.add("&7Target: &f" + announcement.getTargetSummary());
        if (!announcement.getMessageLines().isEmpty()) {
            lore.add("&7Preview: &f" + announcement.getMessageLines().get(0));
        }
        lore.add(mode == MenuType.EDIT_LIST ? "&8Click to edit" : "&8Click to delete");
        return lore;
    }

    private Announcement getAnnouncementForSlot(int page, int slot) {
        List<Announcement> announcements = announcementService.getAnnouncements();
        int index = page * LIST_PAGE_SIZE + slot;
        if (index < 0 || index >= announcements.size()) {
            return null;
        }
        return announcements.get(index);
    }

    private List<String> parseMessageLines(String input) {
        List<String> lines = new ArrayList<>();
        for (String part : input.split("\\|")) {
            if (!part.isBlank()) {
                lines.add(part.trim());
            }
        }
        return lines;
    }

    private Set<String> parseTargets(String input) {
        Set<String> targets = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            if (!part.isBlank()) {
                targets.add(part.trim().toLowerCase(Locale.ROOT));
            }
        }
        return targets;
    }

    private boolean requirePermission(Player player, String permission) {
        if (player.hasPermission("announcementgui.admin") || player.hasPermission(permission)) {
            return true;
        }
        player.sendMessage(color("&cYou do not have permission: &f" + permission));
        return false;
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "Not set" : value;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(this::color).toList());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private String color(String value) {
        return ColorUtil.colorize(value);
    }

    private record MenuView(MenuType type, int page, UUID targetAnnouncementId) {
    }

    private record EditorSession(UUID announcementId, AnnouncementDraft draft) {
    }

    private enum MenuType {
        MAIN,
        EDITOR,
        EDIT_LIST,
        DELETE_LIST,
        DELETE_CONFIRM
    }

    private enum PromptField {
        NAME,
        TITLE,
        DESCRIPTION,
        MESSAGE,
        INTERVAL,
        TARGETS
    }

    private final class ChatInputPrompt extends StringPrompt {

        private final PromptField promptField;
        private final String instruction;

        private ChatInputPrompt(PromptField promptField, String instruction) {
            this.promptField = promptField;
            this.instruction = instruction;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return color(instruction);
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (context.getForWhom() instanceof Player player) {
                plugin.getServer().getScheduler().runTask(plugin, () -> handlePrompt(player, promptField, input.trim()));
            }
            return Prompt.END_OF_CONVERSATION;
        }
    }
}
