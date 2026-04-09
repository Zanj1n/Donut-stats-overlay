package net.donutsmp.balance.client;

import net.donutsmp.balance.client.config.DonutBalanceConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Locale;
import java.util.Set;

public final class DonutSmpServerDetector {
    private static final Set<String> HOSTS = Set.of(
            "mc.donutsmp.net",
            "donutsmp.net",
            "lmc.donutsmp.net",
            "eu.donutsmp.net",
            "us.donutsmp.net",
            "au.donutsmp.net",
            "ca.donutsmp.net",
            "uk.donutsmp.net",
            "de.donutsmp.net",
            "fr.donutsmp.net",
            "es.donutsmp.net",
            "it.donutsmp.net",
            "nl.donutsmp.net",
            "pl.donutsmp.net",
            "br.donutsmp.net",
            "mx.donutsmp.net",
            "ar.donutsmp.net",
            "sa.donutsmp.net",
            "ae.donutsmp.net",
            "za.donutsmp.net"
    );

    private static volatile boolean onDonutSmp;

    private DonutSmpServerDetector() {
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                onDonutSmp = isDonutHost(connectionAddress(handler, client)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            onDonutSmp = false;
            StatsCache.clear();
        });
    }

    /**
     * Prefer the address the player typed in the server list (hostname) over the socket’s
     * resolved IP — otherwise we only see {@code /203.x.x.x:25565} and never match donutsmp.net.
     */
    private static String connectionAddress(ClientPacketListener handler, Minecraft client) {
        if (client != null) {
            var server = client.getCurrentServer();
            if (server != null && server.ip != null && !server.ip.isEmpty()) {
                return server.ip;
            }
        }
        if (handler != null && handler.getConnection() != null) {
            var addr = handler.getConnection().getRemoteAddress();
            if (addr != null) {
                return addr.toString();
            }
        }
        return "";
    }

    static boolean isDonutHost(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        String host = address.toLowerCase(Locale.ROOT).trim();
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int at = host.lastIndexOf('@');
        if (at >= 0 && at < host.length() - 1) {
            host = host.substring(at + 1);
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0) {
            String maybePort = host.substring(colon + 1);
            if (maybePort.chars().allMatch(Character::isDigit)) {
                host = host.substring(0, colon);
            }
        }
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            if (end > 1) {
                host = host.substring(1, end);
            }
        }
        if (HOSTS.contains(host)) {
            return true;
        }
        // Covers regional hosts and direct connections that still resolve to *.donutsmp.net
        return host.endsWith(".donutsmp.net") || host.equals("donutsmp.net");
    }

    public static boolean isOnDonutSmp() {
        return onDonutSmp;
    }

    /** True when connected to multiplayer and either DonutSMP was detected or config allows any server. */
    public static boolean shouldShowOverlay(DonutBalanceConfig cfg) {
        if (cfg != null && cfg.allowAnyServer) {
            Minecraft c = Minecraft.getInstance();
            return c != null && c.getConnection() != null;
        }
        return onDonutSmp;
    }
}
