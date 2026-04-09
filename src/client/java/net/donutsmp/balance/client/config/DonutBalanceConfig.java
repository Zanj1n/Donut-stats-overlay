package net.donutsmp.balance.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DonutBalanceConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("donutsmp-balance.json");
    public static final int KEY_REGISTRY_SLOTS = 12;

    public String apiKey = "";
    public boolean enabled = true;
    public boolean showSelf = false;
    /** When true, show the overlay on any multiplayer server (not only DonutSMP hostnames). */
    public boolean allowAnyServer = false;
    /** Abbreviate numeric money values like 1,000 -> 1K and 1,000,000 -> 1M. */
    public boolean abbreviateMoney = true;
    public boolean showMoney = true;
    public boolean showKills = true;
    public boolean showDeaths = true;
    public boolean showBrokenBlocks = false;
    public boolean showMobsKilled = false;
    public boolean showMoneyMadeFromSell = false;
    public boolean showMoneySpentOnShop = false;
    public boolean showPlacedBlocks = false;
    public boolean showPlaytime = false;
    public boolean showShards = false;
    public boolean showIcons = true;
    /** Vertical offset above vanilla nametag anchor. */
    public double nametagClearance = 1.05;
    /** Extra vertical lift applied per overlay line. */
    public double nametagLiftPerLine = 0.07;
    public int cacheSeconds = 60;

    /**
     * Personal notebook: which Minecraft username each key is tied to.
     * Not sent anywhere; not used for API calls unless you copy a key into {@link #apiKey} above.
     */
    public List<KeyRegistryEntry> keyRegistry = new ArrayList<>();

    private static DonutBalanceConfig instance;

    public static DonutBalanceConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        instance = new DonutBalanceConfig();
        if (!Files.isRegularFile(PATH)) {
            instance.normalizeRegistry();
            return;
        }
        try {
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            DonutBalanceConfig loaded = GSON.fromJson(json, DonutBalanceConfig.class);
            if (loaded != null) {
                instance = loaded;
            }
        } catch (IOException ignored) {
        }
        instance.normalizeRegistry();
    }

    public void normalizeRegistry() {
        if (keyRegistry == null) {
            keyRegistry = new ArrayList<>();
        }
        while (keyRegistry.size() < KEY_REGISTRY_SLOTS) {
            keyRegistry.add(new KeyRegistryEntry());
        }
        while (keyRegistry.size() > KEY_REGISTRY_SLOTS) {
            keyRegistry.remove(keyRegistry.size() - 1);
        }
        for (int i = 0; i < keyRegistry.size(); i++) {
            if (keyRegistry.get(i) == null) {
                keyRegistry.set(i, new KeyRegistryEntry());
            }
            KeyRegistryEntry e = keyRegistry.get(i);
            if (e.username == null) {
                e.username = "";
            }
            if (e.apiKey == null) {
                e.apiKey = "";
            }
        }
    }

    public void save() {
        normalizeRegistry();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
