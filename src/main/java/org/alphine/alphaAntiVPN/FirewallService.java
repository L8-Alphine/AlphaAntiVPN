package org.alphine.alphaAntiVPN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirewallService {

    private static final int MAX_CONNECTIONS = 10;
    private static final long TIME_FRAME_MS = 60000; // 1 minute

    private final Set<String> blockedIPs = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, ConnectionInfo> connectionAttempts = Collections.synchronizedMap(new HashMap<>());
    private final Logger logger;

    public FirewallService(Logger logger) {
        this.logger = logger;
    }

    public void blockIP(String ip) {
        blockedIPs.add(ip);
        if (hasRootAccess() && isDedicatedOrVPS()) {
            if (executeIptablesCommand("-A INPUT -s " + ip + " -j DROP")) {
                logger.info("[Firewall] " + ip + " was successfully blocked using iptables.");
            } else {
                logger.warning("[Firewall] Failed to block " + ip + " using iptables.");
            }
        }
        logger.info("[Firewall] " + ip + " is using a VPN connection and is currently being firewalled. Successful Block");
    }

    public void unblockIP(String ip) {
        blockedIPs.remove(ip);
        if (hasRootAccess() && isDedicatedOrVPS()) {
            if (executeIptablesCommand("-D INPUT -s " + ip + " -j DROP")) {
                logger.info("[Firewall] " + ip + " was successfully unblocked using iptables.");
            } else {
                logger.warning("[Firewall] Failed to unblock " + ip + " using iptables.");
            }
        }
    }

    public boolean isBlocked(String ip) {
        return blockedIPs.contains(ip);
    }

    public Set<String> getBlockedIPs() {
        return new HashSet<>(blockedIPs);
    }

    public boolean checkAndTrackConnection(String ip) {
        long currentTime = System.currentTimeMillis();
        connectionAttempts.putIfAbsent(ip, new ConnectionInfo());
        ConnectionInfo connectionInfo = connectionAttempts.get(ip);

        synchronized (connectionInfo) {
            connectionInfo.removeOldConnections(currentTime - TIME_FRAME_MS);
            connectionInfo.addConnectionAttempt(currentTime);

            if (connectionInfo.getConnectionCount() > MAX_CONNECTIONS) {
                blockIP(ip);
                logger.info("[Firewall] " + ip + " was blocked for exceeding connection limit.");
                return false; // Indicate that the connection should be blocked
            }
        }
        return true; // Indicate that the connection is allowed
    }

    private boolean hasRootAccess() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            logger.info("Windows OS detected. Skipping root access check.");
            return false; // Assume no root access on Windows for simplicity
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("id", "-u");
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                return "0".equals(output); // Root user ID is 0
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Not on VPS that has root access. Ignore this message.", e);
            return false;
        }
    }

    private boolean isDedicatedOrVPS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            logger.info("Windows OS detected. Skipping VPS check.");
            return false; // Assume no VPS on Windows for simplicity
        }
        // This is a basic check. You may need more sophisticated checks based on your environment.
        String[] commands = {"/bin/sh", "-c", "cat /proc/1/sched | grep -oE 'init|systemd'"}; // Checks if the init process is systemd or init
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                return output != null && (output.contains("init") || output.contains("systemd"));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to determine if the server is a dedicated host or VPS.", e);
            return false;
        }
    }

    private boolean executeIptablesCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            logger.info("Windows OS detected. Skipping iptables command execution.");
            return false; // No iptables on Windows
        }
        String[] commands = {"/bin/sh", "-c", "iptables " + command};
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to execute iptables command: " + command, e);
            return false;
        }
    }

    private static class ConnectionInfo {
        private final Set<Long> connectionTimes = new HashSet<>();

        void addConnectionAttempt(long time) {
            connectionTimes.add(time);
        }

        void removeOldConnections(long cutoff) {
            connectionTimes.removeIf(time -> time < cutoff);
        }

        int getConnectionCount() {
            return connectionTimes.size();
        }
    }
}
