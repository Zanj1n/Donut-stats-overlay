package net.donutsmp.balance.client.api;

import com.google.gson.JsonParser;
import net.donutsmp.balance.client.config.DonutBalanceConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class DonutSmpApi {
    private static final String BASE = "https://api.donutsmp.net";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private DonutSmpApi() {
    }

    public static CompletableFuture<PlayerStats> fetchStats(String username) {
        String key = DonutBalanceConfig.get().apiKey == null ? "" : DonutBalanceConfig.get().apiKey.trim();
        if (key.isEmpty()) {
            return CompletableFuture.completedFuture(PlayerStats.EMPTY);
        }
        String safe = username == null ? "" : username.trim();
        if (safe.isEmpty()) {
            return CompletableFuture.completedFuture(PlayerStats.EMPTY);
        }
        var candidates = usernameCandidates(safe);
        if (candidates.isEmpty()) {
            return CompletableFuture.completedFuture(PlayerStats.EMPTY);
        }
        return fetchSequential(candidates.toArray(String[]::new), 0, key);
    }

    private static CompletableFuture<PlayerStats> fetchSequential(String[] candidates, int index, String key) {
        if (index >= candidates.length) {
            return CompletableFuture.completedFuture(PlayerStats.EMPTY);
        }
        return fetchSingle(candidates[index], key).thenCompose(stats -> {
            if (stats == null) {
                return fetchSequential(candidates, index + 1, key);
            }
            if (stats.apiUnauthorized()) {
                return CompletableFuture.completedFuture(stats);
            }
            if (stats.hasDisplayableData()) {
                return CompletableFuture.completedFuture(stats);
            }
            // Avoid retry storms on gateway/network/server failures.
            if (!stats.fetchError().isBlank() && !isLikelyUsernameVariantIssue(stats.fetchError())) {
                return CompletableFuture.completedFuture(stats);
            }
            return fetchSequential(candidates, index + 1, key).thenApply(later -> {
                if (later.hasDisplayableData() || later.apiUnauthorized()) {
                    return later;
                }
                if (!later.fetchError().isBlank()) {
                    return later;
                }
                if (!stats.fetchError().isBlank()) {
                    return stats;
                }
                return PlayerStats.EMPTY;
            });
        });
    }

    private static CompletableFuture<PlayerStats> fetchSingle(String username, String key) {
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20");
        String uri = BASE + "/v1/stats/" + encoded;
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "DonutSMP-Balance/1.0 (Fabric; +https://github.com/)")
                .header("Authorization", "Bearer " + key)
                .GET()
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    String body = response.body() == null ? "" : response.body();
                    if (response.statusCode() == 401) {
                        return PlayerStats.unauthorized();
                    }
                    if (response.statusCode() != 200) {
                        return PlayerStats.error("HTTP " + response.statusCode() + " " + summarizeBody(body));
                    }
                    try {
                        var el = JsonParser.parseString(body);
                        if (el == null || !el.isJsonObject()) {
                            return PlayerStats.error("Not JSON: " + summarizeBody(body));
                        }
                        var root = el.getAsJsonObject();
                        if (isUnauthorizedBody(root)) {
                            return PlayerStats.unauthorized();
                        }
                        var stats = PlayerStats.fromApiJson(root);
                        if (stats != null && stats.hasDisplayableData()) {
                            return stats;
                        }
                        if (stats != null && !stats.hasDisplayableData()) {
                            return PlayerStats.error("Empty stats for " + username + " (" + summarizeBody(body) + ")");
                        }
                        return PlayerStats.error("Unknown JSON shape (" + summarizeBody(body) + ")");
                    } catch (Exception e) {
                        return PlayerStats.error(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                })
                .exceptionally(ex -> PlayerStats.error(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
    }

    private static String summarizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String t = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (t.length() > 80) {
            return t.substring(0, 77) + "...";
        }
        return t;
    }

    private static Set<String> usernameCandidates(String raw) {
        java.util.LinkedHashSet<String> c = new java.util.LinkedHashSet<>();
        c.add(raw);
        String lower = raw.toLowerCase(Locale.ROOT);
        if (!lower.equals(raw)) {
            c.add(lower);
        }
        c.removeIf(s -> s.isBlank() || s.length() > 32);
        return c;
    }

    private static boolean isLikelyUsernameVariantIssue(String error) {
        if (error == null) {
            return false;
        }
        String e = error.toLowerCase(Locale.ROOT);
        return e.contains("empty stats") || e.contains("unknown json shape");
    }

    private static boolean isUnauthorizedBody(com.google.gson.JsonObject root) {
        try {
            if (root.has("status") && root.get("status").isJsonPrimitive()) {
                var p = root.get("status").getAsJsonPrimitive();
                if (p.isNumber() && p.getAsInt() == 401) {
                    return true;
                }
                if (p.isString() && "401".equals(p.getAsString().trim())) {
                    return true;
                }
            }
            if (root.has("reason") && root.get("reason").isJsonPrimitive()) {
                String reason = root.get("reason").getAsString();
                if (reason != null && reason.toLowerCase(Locale.ROOT).contains("unauthorized")) {
                    return true;
                }
            }
            if (root.has("message") && root.get("message").isJsonPrimitive()) {
                String message = root.get("message").getAsString();
                if (message != null && message.toLowerCase(Locale.ROOT).contains("generate an api key")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static String normalizeMcUsername(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
