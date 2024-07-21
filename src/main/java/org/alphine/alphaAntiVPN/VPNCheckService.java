package org.alphine.alphaAntiVPN;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VPNCheckService {

    private static final String GEOLOCATION_API = "http://ip-api.com/line/";
    private static final String[] IP_LIST_URLS = {
            "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt",
            "https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list-raw.txt",
            "https://raw.githubusercontent.com/scriptzteam/ProtonVPN-VPN-IPs/main/exit_ips.txt",
            "https://raw.githubusercontent.com/mmpx12/proxy-list/master/ips-list.txt",
            "https://check.torproject.org/torbulkexitlist?ip=1.1.1.1",
            "https://cinsscore.com/list/ci-badguys.txt",
            "https://lists.blocklist.de/lists/all.txt",
            "https://blocklist.greensnow.co/greensnow.txt",
            "https://raw.githubusercontent.com/firehol/blocklist-ipsets/master/stopforumspam_7d.ipset",
            "https://raw.githubusercontent.com/jetkai/proxy-list/main/online-proxies/txt/proxies.txt",
            "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/socks4.txt",
            "https://www.brightdata.com/vpn-list",
            "https://www.smartproxy.com/proxy-list"
    };

    private final Set<String> blockedIPs = new HashSet<>();
    private final Set<String> whitelistedIPs = new HashSet<>();
    private final Set<String> blacklistedIPs = new HashSet<>();
    private final Map<String, Set<String>> ipToUsernames = new HashMap<>();
    private final Logger logger;
    private final FirewallService firewallService;

    public VPNCheckService(Logger logger, FileConfiguration config, FirewallService firewallService) {
        this.logger = logger;
        this.firewallService = firewallService;
        updateIPLists();
        loadWhitelistedIPs(config);
        loadBlacklistedIPs(config);
    }

    public boolean isVPN(String ip) {
        return blockedIPs.contains(ip) && !whitelistedIPs.contains(ip);
    }

    public void updateIPLists() {
        logger.info("Updating IP Lists...");
        blockedIPs.clear();
        for (String url : IP_LIST_URLS) {
            try {
                fetchIPs(url);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to fetch IPs from: " + url, e);
            }
        }
        logger.info("IP Lists updated. " + blockedIPs.size() + " IPs loaded.");
    }

    private void fetchIPs(String urlString) throws Exception {
        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String ip = inputLine.trim().split(":")[0];
                    if (!ip.isEmpty() && !ip.startsWith("#")) {
                        blockedIPs.add(ip);
                    }
                }
            }
        } else if (responseCode == 404) {
            logger.log(Level.WARNING, "URL not found: " + urlString + " Response code: " + responseCode);
        } else {
            logger.log(Level.WARNING, "Non-200 response code received from URL: " + urlString + " Response code: " + responseCode);
        }
    }

    public void addWhitelistedIP(String ip) {
        whitelistedIPs.add(ip);
        firewallService.unblockIP(ip);
    }

    public void removeWhitelistedIP(String ip) {
        whitelistedIPs.remove(ip);
        if (blockedIPs.contains(ip)) {
            firewallService.blockIP(ip);
        }
    }

    public Set<String> getWhitelistedIPs() {
        return new HashSet<>(whitelistedIPs);
    }

    public void addBlacklistedIP(String ip) {
        blacklistedIPs.add(ip);
        firewallService.blockIP(ip);
    }

    public void removeBlacklistedIP(String ip) {
        blacklistedIPs.remove(ip);
        firewallService.unblockIP(ip);
    }

    public Set<String> getBlacklistedIPs() {
        return new HashSet<>(blacklistedIPs);
    }

    private void loadWhitelistedIPs(FileConfiguration config) {
        whitelistedIPs.addAll(config.getStringList("anti-vpn.whitelisted-ips"));
        for (String ip : whitelistedIPs) {
            firewallService.unblockIP(ip);
        }
    }

    private void loadBlacklistedIPs(FileConfiguration config) {
        blacklistedIPs.addAll(config.getStringList("anti-vpn.blacklisted-ips"));
        for (String ip : blacklistedIPs) {
            firewallService.blockIP(ip);
        }
    }

    public String getGeoLocation(String ip) {
        try {
            URL url = new URI(GEOLOCATION_API + ip).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    return response.toString().trim();
                }
            } else {
                logger.log(Level.WARNING, "Non-200 response code received from GeoLocation API: " + responseCode);
                return "Unknown";
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to fetch geolocation for IP: " + ip, e);
            return "Unknown";
        }
    }

    public List<String> getAlts(String ip) {
        return new ArrayList<>(ipToUsernames.getOrDefault(ip, Collections.emptySet()));
    }

    public void trackPlayerIP(String username, String ip) {
        ipToUsernames.computeIfAbsent(ip, k -> new HashSet<>()).add(username);
    }
}
