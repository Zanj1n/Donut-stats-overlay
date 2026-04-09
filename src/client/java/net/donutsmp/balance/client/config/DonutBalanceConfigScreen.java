package net.donutsmp.balance.client.config;

import net.donutsmp.balance.client.StatsCache;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DonutBalanceConfigScreen {
    private DonutBalanceConfigScreen() {
    }

    public static Screen create(Screen parent) {
        DonutBalanceConfig c = DonutBalanceConfig.get();
        c.normalizeRegistry();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("donutsmp_balance.config.title"))
                .setSavingRunnable(() -> {
                    c.save();
                    StatsCache.clear();
                });
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("donutsmp_balance.config.category.general"));

        general.addEntry(eb.startStrField(Component.translatable("donutsmp_balance.config.api_key"), c.apiKey)
                .setDefaultValue("")
                .setTooltip(Component.translatable("donutsmp_balance.config.api_key.tooltip"))
                .setSaveConsumer(v -> c.apiKey = v == null ? "" : v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.enabled"), c.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> c.enabled = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_self"), c.showSelf)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showSelf = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.allow_any_server"), c.allowAnyServer)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("donutsmp_balance.config.allow_any_server.tooltip"))
                .setSaveConsumer(v -> c.allowAnyServer = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.abbreviate_money"), c.abbreviateMoney)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("donutsmp_balance.config.abbreviate_money.tooltip"))
                .setSaveConsumer(v -> c.abbreviateMoney = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_money"), c.showMoney)
                .setDefaultValue(true)
                .setSaveConsumer(v -> c.showMoney = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_kills"), c.showKills)
                .setDefaultValue(true)
                .setSaveConsumer(v -> c.showKills = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_deaths"), c.showDeaths)
                .setDefaultValue(true)
                .setSaveConsumer(v -> c.showDeaths = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_broken_blocks"), c.showBrokenBlocks)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showBrokenBlocks = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_mobs_killed"), c.showMobsKilled)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showMobsKilled = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_money_made_from_sell"), c.showMoneyMadeFromSell)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showMoneyMadeFromSell = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_money_spent_on_shop"), c.showMoneySpentOnShop)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showMoneySpentOnShop = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_placed_blocks"), c.showPlacedBlocks)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showPlacedBlocks = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_playtime"), c.showPlaytime)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showPlaytime = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_shards"), c.showShards)
                .setDefaultValue(false)
                .setSaveConsumer(v -> c.showShards = v)
                .build());
        general.addEntry(eb.startBooleanToggle(Component.translatable("donutsmp_balance.config.show_icons"), c.showIcons)
                .setDefaultValue(true)
                .setSaveConsumer(v -> c.showIcons = v)
                .build());

        general.addEntry(eb.startFloatField(
                        Component.translatable("donutsmp_balance.config.nametag_clearance"),
                        (float) c.nametagClearance)
                .setDefaultValue(1.05f)
                .setMin(0.0f)
                .setMax(15.0f)
                .setSaveConsumer(v -> c.nametagClearance = v)
                .build());
        general.addEntry(eb.startFloatField(
                        Component.translatable("donutsmp_balance.config.nametag_lift_per_line"),
                        (float) c.nametagLiftPerLine)
                .setDefaultValue(0.07f)
                .setMin(0.0f)
                .setMax(0.30f)
                .setSaveConsumer(v -> c.nametagLiftPerLine = v)
                .build());

        general.addEntry(eb.startIntField(Component.translatable("donutsmp_balance.config.cache_seconds"), c.cacheSeconds)
                .setDefaultValue(60)
                .setMin(5)
                .setMax(600)
                .setSaveConsumer(v -> c.cacheSeconds = v)
                .build());

        ConfigCategory registry = builder.getOrCreateCategory(Component.translatable("donutsmp_balance.config.category.registry"));
        for (int i = 0; i < DonutBalanceConfig.KEY_REGISTRY_SLOTS; i++) {
            final int slot = i;
            KeyRegistryEntry row = c.keyRegistry.get(slot);
            int label = slot + 1;
            registry.addEntry(eb.startStrField(Component.translatable("donutsmp_balance.config.registry.username", label), row.username)
                    .setDefaultValue("")
                    .setSaveConsumer(v -> c.keyRegistry.get(slot).username = v == null ? "" : v)
                    .build());
            registry.addEntry(eb.startStrField(Component.translatable("donutsmp_balance.config.registry.api_key", label), row.apiKey)
                    .setDefaultValue("")
                    .setTooltip(Component.translatable("donutsmp_balance.config.registry.api_key.tooltip"))
                    .setSaveConsumer(v -> c.keyRegistry.get(slot).apiKey = v == null ? "" : v)
                    .build());
        }

        return builder.build();
    }
}
