package net.donutsmp.balance.client.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record PlayerStats(
        String money,
        String kills,
        String deaths,
        String brokenBlocks,
        String mobsKilled,
        String moneyMadeFromSell,
        String moneySpentOnShop,
        String placedBlocks,
        String playtime,
        String shards,
        boolean apiUnauthorized,
        String fetchError
) {
    public static final PlayerStats EMPTY = new PlayerStats("", "", "", "", "", "", "", "", "", "", false, "");

    public static PlayerStats unauthorized() {
        return new PlayerStats("", "", "", "", "", "", "", "", "", "", true, "");
    }

    public static PlayerStats error(String message) {
        String m = message == null ? "" : message.trim();
        if (m.length() > 120) {
            m = m.substring(0, 117) + "...";
        }
        return new PlayerStats("", "", "", "", "", "", "", "", "", "", false, m);
    }

    public boolean hasDisplayableData() {
        return apiUnauthorized
                || !money.isBlank()
                || !kills.isBlank()
                || !deaths.isBlank()
                || !brokenBlocks.isBlank()
                || !mobsKilled.isBlank()
                || !moneyMadeFromSell.isBlank()
                || !moneySpentOnShop.isBlank()
                || !placedBlocks.isBlank()
                || !playtime.isBlank()
                || !shards.isBlank();
    }

    public static PlayerStats fromApiJson(JsonObject root) {
        if (root == null) {
            return null;
        }
        JsonObject block = extractStatsBlock(root);
        if (block == null) {
            return null;
        }
        String money = readStat(block, "money", "balance", "eco", "coins", "cash");
        String kills = readStat(block, "kills", "player_kills", "kill");
        String deaths = readStat(block, "deaths", "death", "player_deaths");
        String brokenBlocks = readStat(block, "broken_blocks", "blocks_broken");
        String mobsKilled = readStat(block, "mobs_killed", "mobskilled");
        String moneyMadeFromSell = readStat(block, "money_made_from_sell", "sell", "sell_money");
        String moneySpentOnShop = readStat(block, "money_spent_on_shop", "shop", "shop_money");
        String placedBlocks = readStat(block, "placed_blocks", "blocks_placed");
        String playtime = readStat(block, "playtime", "time_played");
        String shards = readStat(block, "shards");
        return new PlayerStats(
                money,
                kills,
                deaths,
                brokenBlocks,
                mobsKilled,
                moneyMadeFromSell,
                moneySpentOnShop,
                placedBlocks,
                playtime,
                shards,
                false,
                ""
        );
    }

    private static JsonObject extractStatsBlock(JsonObject root) {
        if (root.has("result")) {
            JsonElement r = root.get("result");
            if (r.isJsonObject()) {
                return r.getAsJsonObject();
            }
            if (r.isJsonArray()) {
                JsonArray a = r.getAsJsonArray();
                if (!a.isEmpty() && a.get(0).isJsonObject()) {
                    return a.get(0).getAsJsonObject();
                }
            }
        }
        if (root.has("data") && root.get("data").isJsonObject()) {
            return root.getAsJsonObject("data");
        }
        if (hasAnyStatKey(root)) {
            return root;
        }
        return null;
    }

    private static boolean hasAnyStatKey(JsonObject o) {
        for (String k : o.keySet()) {
            String n = normalizeKey(k);
            if (n.contains("money") || n.contains("balance") || n.contains("kill") || n.contains("death")) {
                return true;
            }
        }
        return false;
    }

    private static String readStat(JsonObject o, String... candidateKeys) {
        for (String key : candidateKeys) {
            String direct = readValue(o, key);
            if (!direct.isBlank()) {
                return direct;
            }
        }
        for (var entry : o.entrySet()) {
            String rawKey = entry.getKey();
            String normRaw = normalizeKey(rawKey);
            for (String candidate : candidateKeys) {
                if (normRaw.equals(normalizeKey(candidate))) {
                    String value = readValue(o, rawKey);
                    if (!value.isBlank()) {
                        return value;
                    }
                }
            }
        }
        return "";
    }

    private static String readValue(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        JsonElement el = o.get(key);
        if (el.isJsonPrimitive()) {
            return readPrimitive(el);
        }
        if (el.isJsonObject()) {
            JsonObject child = el.getAsJsonObject();
            for (String inner : new String[]{"value", "amount", "formatted", "text", "raw"}) {
                if (child.has(inner)) {
                    String v = readValue(child, inner);
                    if (!v.isBlank()) {
                        return v;
                    }
                }
            }
        }
        return "";
    }

    private static String readPrimitive(JsonElement el) {
        try {
            var p = el.getAsJsonPrimitive();
            if (p.isString()) {
                return p.getAsString();
            }
            if (p.isNumber()) {
                return p.getAsNumber().toString();
            }
            if (p.isBoolean()) {
                return Boolean.toString(p.getAsBoolean());
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    }
}
