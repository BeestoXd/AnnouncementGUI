package com.bx.announcementGUI.command;

import com.bx.announcementGUI.AnnouncementGUI;
import com.bx.announcementGUI.model.Announcement;
import com.bx.announcementGUI.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AnnouncementCommand implements TabExecutor {

    private final AnnouncementGUI plugin;

    public AnnouncementCommand(AnnouncementGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (commandName.equals("announcementgui") || commandName.equals("agui")) {
            return openGui(sender);
        }

        if (args.length == 0) {
            return openGui(sender);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "open" -> openGui(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "broadcast" -> handleBroadcast(sender, args);
            default -> {
                sender.sendMessage(color("&cUnknown subcommand. Use /announcement <open|reload|list|broadcast>."));
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("open", "reload", "list", "broadcast"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("broadcast")) {
            List<String> suggestions = new ArrayList<>();
            for (Announcement announcement : plugin.getAnnouncementService().getAnnouncements()) {
                suggestions.add(announcement.getId().toString());
            }
            return filter(suggestions, args[1]);
        }
        return List.of();
    }

    private boolean openGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cCommand ini hanya bisa dipakai oleh player."));
            return true;
        }
        if (!hasPermission(player, "announcementgui.open")) {
            return true;
        }
        plugin.getGuiService().openMain(player);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "announcementgui.reload")) {
            return true;
        }
        plugin.reloadPluginState();
        sender.sendMessage(color("&aAnnouncementGUI reloaded successfully."));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!hasPermission(sender, "announcementgui.open")) {
            return true;
        }
        List<Announcement> announcements = plugin.getAnnouncementService().getAnnouncements();
        if (announcements.isEmpty()) {
            sender.sendMessage(color("&7There are no announcements yet."));
            return true;
        }
        sender.sendMessage(color("&eAnnouncement list:"));
        for (Announcement announcement : announcements) {
            sender.sendMessage(color("&8- &f" + announcement.getDisplayName()
                    + " &8[" + announcement.getId().toString().substring(0, 8) + "] "
                    + "&7target=&f" + announcement.getTargetSummary()
                    + " &7status=" + (announcement.isEnabled() ? "&aenabled" : "&cdisabled")));
        }
        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "announcementgui.broadcast")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUse /announcement broadcast <id>"));
            return true;
        }

        Optional<Announcement> announcement = plugin.getAnnouncementService().findByToken(args[1]);
        if (announcement.isEmpty()) {
            sender.sendMessage(color("&cAnnouncement not found."));
            return true;
        }

        plugin.forceBroadcast(announcement.get().getId());
        sender.sendMessage(color("&aForce broadcast sent for &f" + announcement.get().getDisplayName()));
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission("announcementgui.admin") || sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(color("&cYou do not have permission: &f" + permission));
        return false;
    }

    private List<String> filter(List<String> values, String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private String color(String value) {
        return ColorUtil.colorize(value);
    }
}
