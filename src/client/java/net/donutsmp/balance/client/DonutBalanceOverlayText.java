package net.donutsmp.balance.client;

import net.donutsmp.balance.client.api.PlayerStats;
import net.donutsmp.balance.client.config.DonutBalanceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DonutBalanceOverlayText {
    static final Style STYLE_MONEY = Style.EMPTY.withBold(true).withColor(ChatFormatting.GREEN);
    static final Style STYLE_KILLS = Style.EMPTY.withBold(true).withColor(ChatFormatting.RED);
    static final Style STYLE_DEATHS = Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(0xFF8800));
    static final Style STYLE_OTHER = Style.EMPTY.withBold(true).withColor(ChatFormatting.WHITE);

    private DonutBalanceOverlayText() {
    }

    public static List<Component> buildLines(String lookupName) {
        List<Component> lines = new ArrayList<>();
        DonutBalanceConfig cfg = DonutBalanceConfig.get();
        String apiKey = cfg.apiKey == null ? "" : cfg.apiKey.trim();
        if (apiKey.isEmpty()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.no_key"));
            return lines;
        }
        if (StatsCache.isPending(lookupName)) {
            lines.add(Component.translatable("donutsmp_balance.overlay.loading"));
            return lines;
        }
        PlayerStats st = StatsCache.getCachedOrPlaceholder(lookupName);
        if (st.apiUnauthorized()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.unauthorized"));
            return lines;
        }
        if (!st.fetchError().isBlank()) {
            String error = st.fetchError();
            if (error.startsWith("HTTP 502")) {
                lines.add(Component.translatable("donutsmp_balance.overlay.api_down"));
            } else {
                lines.add(Component.literal(error));
            }
            return lines;
        }
        if (cfg.showMoney && !st.money().isBlank()) {
            String money = formatMoney(st.money(), cfg.abbreviateMoney);
            lines.add(Component.translatable("donutsmp_balance.overlay.money", money).withStyle(STYLE_MONEY));
        }
        if (cfg.showKills && !st.kills().isBlank()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.kills", st.kills() + " ⚔").withStyle(STYLE_KILLS));
        }
        if (cfg.showDeaths && !st.deaths().isBlank()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.deaths", st.deaths() + " ☠").withStyle(STYLE_DEATHS));
        }
        if (cfg.showBrokenBlocks && !st.brokenBlocks().isBlank()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.broken_blocks", st.brokenBlocks()).withStyle(STYLE_OTHER));
        }
        if (cfg.showMobsKilled && !st.mobsKilled().isBlank()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.mobs_killed", st.mobsKilled()).withStyle(STYLE_OTHER));
        }
        if (cfg.showMoneyMadeFromSell && !st.moneyMadeFromSell().isBlank()) {
            String sellMoney = formatMoney(st.moneyMadeFromSell(), cfg.abbreviateMoney);
            lines.add(Component.translatable("donutsmp_balance.overlay.money_made_from_sell", sellMoney).withStyle(STYLE_OTHER));
        }
        if (cfg.showMoneySpentOnShop && !st.moneySpentOnShop().isBlank()) {
            String shopMoney = formatMoney(st.moneySpentOnShop(), cfg.abbreviateMoney);
            lines.add(Component.translatable("donutsmp_balance.overlay.money_spent_on_shop", shopMoney).withStyle(STYLE_OTHER));
        }
        if (cfg.showPlacedBlocks && !st.placedBlocks().isBlank()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.placed_blocks", st.placedBlocks()).withStyle(STYLE_OTHER));
        }
        if (cfg.showPlaytime && !st.playtime().isBlank()) {
            String icon = cfg.showIcons ? "🕒 " : "";
            lines.add(Component.translatable("donutsmp_balance.overlay.playtime", icon + formatPlaytime(st.playtime())).withStyle(STYLE_OTHER));
        }
        if (cfg.showShards && !st.shards().isBlank()) {
            String icon = cfg.showIcons ? "🔮 " : "";
            lines.add(Component.translatable("donutsmp_balance.overlay.shards", icon + st.shards()).withStyle(STYLE_OTHER));
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable("donutsmp_balance.overlay.no_stats"));
        }
        return lines;
    }

    static String formatMoney(String raw, boolean abbreviate) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        boolean hasDollar = s.endsWith("$");
        String core = hasDollar ? s.substring(0, s.length() - 1).trim() : s;
        String lower = core.toLowerCase(Locale.ROOT);
        if (lower.matches(".*[kmbt]$")) {
            return core + "$";
        }
        try {
            double value = Double.parseDouble(core.replace(",", ""));
            String out;
            if (abbreviate && Math.abs(value) >= 1000.0) {
                out = abbreviateNumber(value);
            } else if (Math.rint(value) == value) {
                out = String.format(Locale.US, "%,d", (long) value);
            } else {
                DecimalFormat df = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
                out = df.format(value);
            }
            return out + "$";
        } catch (NumberFormatException ignored) {
            return hasDollar ? s : s + "$";
        }
    }

    private static String abbreviateNumber(double value) {
        double abs = Math.abs(value);
        String suffix = "";
        double scaled = value;
        if (abs >= 1_000_000_000_000.0) {
            suffix = "T";
            scaled = value / 1_000_000_000_000.0;
        } else if (abs >= 1_000_000_000.0) {
            suffix = "B";
            scaled = value / 1_000_000_000.0;
        } else if (abs >= 1_000_000.0) {
            suffix = "M";
            scaled = value / 1_000_000.0;
        } else if (abs >= 1_000.0) {
            suffix = "K";
            scaled = value / 1_000.0;
        }
        DecimalFormat df = Math.abs(scaled) >= 10.0
                ? new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.US))
                : new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(scaled) + suffix;
    }

    static String formatPlaytime(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (!s.matches("[-+]?\\d[\\d,]*")) {
            return s;
        }
        try {
            long value = Long.parseLong(s.replace(",", ""));
            long seconds = normalizePlaytimeToSeconds(value);
            if (seconds < 0) {
                seconds = 0;
            }
            long minutes = seconds / 60;
            long remSeconds = seconds % 60;
            long hours = minutes / 60;
            long remMinutes = minutes % 60;
            long days = hours / 24;
            long remHours = hours % 24;
            long years = days / 365;
            long remDaysAfterYears = days % 365;

            if (years > 0) {
                return years + "y " + remDaysAfterYears + "d";
            }
            if (days > 0) {
                return days + "d " + remHours + "hr";
            }
            if (hours > 0) {
                return hours + "hr " + remMinutes + "m";
            }
            if (minutes > 0) {
                return minutes + "m " + remSeconds + "s";
            }
            return seconds + "s";
        } catch (NumberFormatException ignored) {
            return s;
        }
    }

    private static long normalizePlaytimeToSeconds(long raw) {
        long seconds = raw;

        long asMsSeconds = raw / 1000L;
        if (raw >= 86_400_000L && asMsSeconds > 0) {
            if (raw > 315_360_000L || asMsSeconds < raw / 10L) {
                seconds = asMsSeconds;
            }
        }

        if (seconds % 20L == 0L) {
            long asTickSeconds = seconds / 20L;
            if (asTickSeconds > 0 && asTickSeconds < seconds / 2L) {
                if (seconds > 31_536_000L * 2L) {
                    seconds = asTickSeconds;
                }
            }
        }

        return seconds;
    }
}
