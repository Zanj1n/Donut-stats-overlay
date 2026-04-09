package net.donutsmp.balance.client;

import net.donutsmp.balance.client.config.DonutBalanceConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class DonutSmpBalanceClient implements ClientModInitializer {
    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        DonutBalanceConfig.load();
        DonutSmpServerDetector.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter % 20 != 0) {
                return;
            }
            prefetchNearbyStats(client);
        });
    }

    private static void prefetchNearbyStats(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        DonutBalanceConfig cfg = DonutBalanceConfig.get();
        if (!cfg.enabled || !DonutSmpServerDetector.shouldShowOverlay(cfg)) {
            return;
        }
        String apiKey = cfg.apiKey == null ? "" : cfg.apiKey.trim();
        if (apiKey.isEmpty()) {
            return;
        }

        for (Player player : client.level.players()) {
            if (player == client.player && !cfg.showSelf) {
                continue;
            }
            if (client.player.distanceToSqr(player) > 64.0 * 64.0) {
                continue;
            }
            String lookupName = player.getGameProfile().getName();
            if (lookupName == null || lookupName.isEmpty()) {
                continue;
            }
            StatsCache.request(lookupName);
        }
    }
}
