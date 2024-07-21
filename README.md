# AlphaAntiVPN

AlphaAntiVPN is a Minecraft plugin designed to detect and block VPN and proxy connections. It leverages a set of IP lists and machine learning to identify suspicious connections and block them using firewall rules.

## Features

- Blocks VPN and proxy connections based on pre-defined IP lists
- Integrates with firewall to block and unblock IPs
- Tracks player connections and monitors for suspicious activity
- Easily configurable via configuration files
- Open-source and customizable

## Installation

1. **Download the Plugin**

   You can download the latest version of AlphaAntiVPN from the [Releases](https://github.com/L8-Alphine/AlphaAntiVPN/releases) page.

2. **Install the Plugin**

   Place the downloaded `.jar` file into the `plugins` directory of your Minecraft server.

3. **Start the Server**

   Start your Minecraft server to generate the default configuration files.

4. **Configure the Plugin**

   Edit the configuration file located at `plugins/AlphaAntiVPN/config.yml` to suit your needs.

## Configuration

Here is a sample configuration file:

```yaml
anti-vpn:
  enabled: true
  kick-message: "VPNs and proxies are not allowed."
  bypass-permission: "alphaAntiVPN.bypass"
  notify-actionbar: true
  notify-permission: "alphaAntiVPN.notify"
  whitelisted-ips:
    - "127.0.0.1"
  blacklisted-ips: []
