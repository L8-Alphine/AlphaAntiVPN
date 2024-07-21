package org.alphine.alphaAntiVPN;

import org.alphine.alphaAntiVPN.commands.AntiVPNCommand;
import org.alphine.alphaAntiVPN.events.PlayerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public final class AlphaAntiVPN extends JavaPlugin {

    private VPNCheckService vpnCheckService;
    private PlayerConnectionListener playerConnectionListener;
    private FirewallService firewallService;
    private boolean antiVpnEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        antiVpnEnabled = config.getBoolean("anti-vpn.enabled");

        Logger logger = getLogger();
        firewallService = new FirewallService(logger);
        vpnCheckService = new VPNCheckService(logger, config, firewallService);
        playerConnectionListener = new PlayerConnectionListener(this, vpnCheckService, firewallService);
        getServer().getPluginManager().registerEvents(playerConnectionListener, this);

        // Register command and set its executor
        PluginCommand command = getCommand("antivpn");
        if (command != null) {
            AntiVPNCommand antiVPNCommand = new AntiVPNCommand(this);
            command.setExecutor(antiVPNCommand);
            command.setTabCompleter(antiVPNCommand);
        } else {
            logger.severe("Command 'antivpn' not found in plugin.yml");
        }

        logger.info("AlphaAntiVPN has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AlphaAntiVPN has been disabled.");
    }

    public boolean isAntiVpnEnabled() {
        return antiVpnEnabled;
    }

    public void setAntiVpnEnabled(boolean antiVpnEnabled) {
        this.antiVpnEnabled = antiVpnEnabled;
        getConfig().set("anti-vpn.enabled", antiVpnEnabled);
        saveConfig();
    }

    public VPNCheckService getVpnCheckService() {
        return vpnCheckService;
    }

    public FirewallService getFirewallService() {
        return firewallService;
    }

    public String getPlayerJoinDate(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.hasPlayedBefore()) {
            long firstPlayed = player.getFirstPlayed();
            return new Date(firstPlayed).toString();
        } else {
            return "Unknown";
        }
    }
}
