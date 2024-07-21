package org.alphine.alphaAntiVPN.events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.alphine.alphaAntiVPN.AlphaAntiVPN;
import org.alphine.alphaAntiVPN.FirewallService;
import org.alphine.alphaAntiVPN.VPNCheckService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerConnectionListener implements Listener {

    private final AlphaAntiVPN plugin;
    private final VPNCheckService vpnCheckService;
    private final FirewallService firewallService;

    public PlayerConnectionListener(AlphaAntiVPN plugin, VPNCheckService vpnCheckService, FirewallService firewallService) {
        this.plugin = plugin;
        this.vpnCheckService = vpnCheckService;
        this.firewallService = firewallService;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.isAntiVpnEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String ip = event.getAddress().getHostAddress();

        if (player.hasPermission(plugin.getConfig().getString("anti-vpn.bypass-permission"))) {
            return;
        }

        if (!firewallService.checkAndTrackConnection(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("You are connecting too fast from a single IP. Please try again later.", NamedTextColor.RED));
            return;
        }

        if (vpnCheckService.isVPN(ip) || firewallService.isBlocked(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(plugin.getConfig().getString("anti-vpn.kick-message")));
            firewallService.blockIP(ip); // Log and block the IP
            notifyAdmins(player);
            return;
        }

        vpnCheckService.trackPlayerIP(player.getName(), ip);  // Track the player's IP on login
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();

        if (player.hasPermission(plugin.getConfig().getString("anti-vpn.bypass-permission"))) {
            return;
        }

        vpnCheckService.trackPlayerIP(player.getName(), ip);  // Track the player's IP on join
    }

    private void notifyAdmins(Player player) {
        if (!plugin.getConfig().getBoolean("anti-vpn.notify-actionbar")) {
            return;
        }

        Component message = Component.text("Blocked VPN/Proxy connection from: ", NamedTextColor.RED)
                .append(Component.text(player.getName(), NamedTextColor.WHITE));
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(plugin.getConfig().getString("anti-vpn.notify-permission"))) {
                onlinePlayer.sendActionBar(message);
            }
        }
    }
}
