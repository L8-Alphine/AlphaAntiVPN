package org.alphine.alphaAntiVPN.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.alphine.alphaAntiVPN.AlphaAntiVPN;
import org.alphine.alphaAntiVPN.VPNCheckService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class AntiVPNCommand implements CommandExecutor, TabCompleter {

    private final AlphaAntiVPN plugin;

    public AntiVPNCommand(AlphaAntiVPN plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /antivpn <enable|disable|status|whitelist|blacklist|check> [args...]", NamedTextColor.RED));
            return true;
        }

        VPNCheckService vpnCheckService = plugin.getVpnCheckService();
        FileConfiguration config = plugin.getConfig();

        switch (args[0].toLowerCase()) {
            case "enable":
                plugin.setAntiVpnEnabled(true);
                sender.sendMessage(Component.text("Anti-VPN has been enabled.", NamedTextColor.GREEN));
                break;

            case "disable":
                plugin.setAntiVpnEnabled(false);
                sender.sendMessage(Component.text("Anti-VPN has been disabled.", NamedTextColor.RED));
                break;

            case "status":
                sender.sendMessage(Component.text("Anti-VPN is currently " +
                        (plugin.isAntiVpnEnabled() ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED)) + ".", NamedTextColor.YELLOW));
                break;

            case "whitelist":
                handleWhitelistCommand(sender, args, vpnCheckService, config);
                break;

            case "blacklist":
                handleBlacklistCommand(sender, args, vpnCheckService, config);
                break;

            case "check":
                if (args.length != 2) {
                    sender.sendMessage(Component.text("Usage: /antivpn check <ip|username>", NamedTextColor.RED));
                    return true;
                }
                String input = args[1];
                handleCheckCommand(sender, input, vpnCheckService);
                break;

            default:
                sender.sendMessage(Component.text("Usage: /antivpn <enable|disable|status|whitelist|blacklist|check> [args...]", NamedTextColor.RED));
                break;
        }

        return true;
    }

    private void handleWhitelistCommand(CommandSender sender, String[] args, VPNCheckService vpnCheckService, FileConfiguration config) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /antivpn whitelist <add|remove> <ip>", NamedTextColor.RED));
            return;
        }

        String action = args[1];
        String ip = args[2];

        if (action.equalsIgnoreCase("add")) {
            vpnCheckService.addWhitelistedIP(ip);
            config.set("anti-vpn.whitelisted-ips", List.copyOf(vpnCheckService.getWhitelistedIPs()));
            plugin.saveConfig();
            sender.sendMessage(Component.text("IP " + ip + " has been added to the whitelist.", NamedTextColor.GREEN));
        } else if (action.equalsIgnoreCase("remove")) {
            vpnCheckService.removeWhitelistedIP(ip);
            config.set("anti-vpn.whitelisted-ips", List.copyOf(vpnCheckService.getWhitelistedIPs()));
            plugin.saveConfig();
            sender.sendMessage(Component.text("IP " + ip + " has been removed from the whitelist.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Usage: /antivpn whitelist <add|remove> <ip>", NamedTextColor.RED));
        }
    }

    private void handleBlacklistCommand(CommandSender sender, String[] args, VPNCheckService vpnCheckService, FileConfiguration config) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /antivpn blacklist <add|remove> <ip>", NamedTextColor.RED));
            return;
        }

        String action = args[1];
        String ip = args[2];

        if (action.equalsIgnoreCase("add")) {
            vpnCheckService.addBlacklistedIP(ip);
            config.set("anti-vpn.blacklisted-ips", List.copyOf(vpnCheckService.getBlacklistedIPs()));
            plugin.saveConfig();
            sender.sendMessage(Component.text("IP " + ip + " has been added to the blacklist.", NamedTextColor.GREEN));
        } else if (action.equalsIgnoreCase("remove")) {
            vpnCheckService.removeBlacklistedIP(ip);
            config.set("anti-vpn.blacklisted-ips", List.copyOf(vpnCheckService.getBlacklistedIPs()));
            plugin.saveConfig();
            sender.sendMessage(Component.text("IP " + ip + " has been removed from the blacklist.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Usage: /antivpn blacklist <add|remove> <ip>", NamedTextColor.RED));
        }
    }

    private void handleCheckCommand(CommandSender sender, String input, VPNCheckService vpnCheckService) {
        Player player = Bukkit.getPlayer(input);
        if (player != null) {
            // Input is a username
            displayPlayerData(sender, player, vpnCheckService);
        } else {
            // Input is assumed to be an IP address
            boolean isVpn = vpnCheckService.isVPN(input);
            String associatedPlayerName = getPlayerNameByIp(input);

            sender.sendMessage(Component.text("IP " + input + " is " + (isVpn ? Component.text("a VPN", NamedTextColor.RED) : Component.text("not a VPN", NamedTextColor.GREEN)) + ".", NamedTextColor.YELLOW));
            if (associatedPlayerName != null) {
                sender.sendMessage(Component.text("Associated player: " + associatedPlayerName, NamedTextColor.YELLOW));
            }
        }
    }

    private void displayPlayerData(CommandSender sender, Player player, VPNCheckService vpnCheckService) {
        String ip = player.getAddress().getAddress().getHostAddress();
        boolean isVpn = vpnCheckService.isVPN(ip);
        String geoLocation = vpnCheckService.getGeoLocation(ip);
        List<String> alts = vpnCheckService.getAlts(ip);
        String dateOfJoin = plugin.getPlayerJoinDate(player.getUniqueId());

        sender.sendMessage(Component.text("IGN: " + player.getName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("IP: " + ip, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Geo Location: " + geoLocation, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Alts: " + String.join(", ", alts), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("On VPN: ").append(isVpn ? Component.text("true", NamedTextColor.RED) : Component.text("false", NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Date of Join: " + dateOfJoin, NamedTextColor.YELLOW));
    }

    private String getPlayerNameByIp(String ip) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getAddress().getAddress().getHostAddress().equals(ip)) {
                return player.getName();
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enable", "disable", "status", "whitelist", "blacklist", "check");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("whitelist") || args[0].equalsIgnoreCase("blacklist"))) {
            return Arrays.asList("add", "remove");
        }
        return null;
    }
}
