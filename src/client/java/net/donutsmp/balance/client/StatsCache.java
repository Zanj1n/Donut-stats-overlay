package net.donutsmp.balance.client;

import net.donutsmp.balance.client.api.DonutSmpApi;
import net.donutsmp.balance.client.api.PlayerStats;
import net.donutsmp.balance.client.config.DonutBalanceConfig;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsCache {
    private record Entry(PlayerStats stats, long fetchedAtMs, boolean pending, long ttlMillis) {
    }

    private static final long FAILED_FETCH_TTL_MS = 15_000L;

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<PlayerStats>> IN_FLIGHT = new ConcurrentHashMap<>();

    private StatsCache() {
    }

    public static void clear() {
        CACHE.clear();
        IN_FLIGHT.clear();
    }

    public static PlayerStats getOrPlaceholder(String mcUsername) {
        String key = DonutSmpApi.normalizeMcUsername(mcUsername);
        if (key.isEmpty()) {
            return PlayerStats.EMPTY;
        }
        long now = System.currentTimeMillis();
        Entry e = CACHE.get(key);
        if (e != null && !e.pending() && now - e.fetchedAtMs() < e.ttlMillis()) {
            return e.stats();
        }
        ensureRequested(key, mcUsername.trim());
        e = CACHE.get(key);
        if (e != null && e.pending()) {
            return PlayerStats.EMPTY;
        }
        if (e != null) {
            return e.stats();
        }
        return PlayerStats.EMPTY;
    }

    public static PlayerStats getCachedOrPlaceholder(String mcUsername) {
        String key = DonutSmpApi.normalizeMcUsername(mcUsername);
        if (key.isEmpty()) {
            return PlayerStats.EMPTY;
        }
        Entry e = CACHE.get(key);
        if (e == null || e.pending()) {
            return PlayerStats.EMPTY;
        }
        return e.stats();
    }

    public static void request(String mcUsername) {
        String key = DonutSmpApi.normalizeMcUsername(mcUsername);
        if (key.isEmpty()) {
            return;
        }
        ensureRequested(key, mcUsername.trim());
    }

    public static boolean isPending(String mcUsername) {
        String key = DonutSmpApi.normalizeMcUsername(mcUsername);
        if (key.isEmpty()) {
            return false;
        }
        Entry e = CACHE.get(key);
        return e != null && e.pending();
    }

    private static void ensureRequested(String normKey, String rawForApi) {
        long now = System.currentTimeMillis();
        Entry e = CACHE.get(normKey);
        if (e != null && !e.pending() && now - e.fetchedAtMs() < e.ttlMillis()) {
            return;
        }
        if (IN_FLIGHT.containsKey(normKey)) {
            return;
        }
        CACHE.put(normKey, new Entry(PlayerStats.EMPTY, now, true, 0L));
        CompletableFuture<PlayerStats> fut = DonutSmpApi.fetchStats(rawForApi)
                .whenComplete((stats, err) -> {
                    IN_FLIGHT.remove(normKey);
                    PlayerStats resolved = stats == null ? PlayerStats.EMPTY : stats;
                    long okTtl = Math.max(5_000L, (long) Math.max(5, DonutBalanceConfig.get().cacheSeconds) * 1000L);
                    long ttl = resolved.hasDisplayableData() ? okTtl : Math.min(okTtl, FAILED_FETCH_TTL_MS);
                    // Back off longer on 5xx/gateway failures to avoid hammering API/rate limits.
                    if (!resolved.fetchError().isBlank() && resolved.fetchError().startsWith("HTTP 5")) {
                        ttl = Math.max(ttl, 60_000L);
                    }
                    CACHE.put(normKey, new Entry(resolved, System.currentTimeMillis(), false, ttl));
                });
        IN_FLIGHT.put(normKey, fut);
    }
}
