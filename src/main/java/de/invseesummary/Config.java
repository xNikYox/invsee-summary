package de.invseesummary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Einstellungen (config/invseesummary.json) */
public class Config {
    public static final int MAX_ITEMS = 12;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("invseesummary.json");
    private static Config instance = new Config();

    public enum Mode { LIST, SEPARATE }

    public boolean summary = true;
    public boolean itemNames = true;
    public boolean everyChest = false;
    public Mode mode = Mode.LIST;
    public int background = 60;
    public List<String> items = new ArrayList<>(List.of("minecraft:totem_of_undying", "minecraft:end_crystal", "minecraft:enchanted_golden_apple"));
    public int invseeScale = 100;
    public int hudScale = 100;
    public boolean showArmor = true;
    public String lastInvseeCommand = null;
    public boolean hud = true;
    public boolean hudSameItems = false;
    public List<String> hudItems = new ArrayList<>(List.of("minecraft:totem_of_undying", "minecraft:end_crystal", "minecraft:enchanted_golden_apple"));
    public int hudX = 4;
    public int hudY = 4;
    public int listOffsetX = 0;
    public int listOffsetY = 0;
    public Map<String, int[]> separateOffsets = new HashMap<>();

    public static Config get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(FILE)) {
            try {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) {
                    if (c.items == null) c.items = new ArrayList<>();
                    if (c.hudItems == null) c.hudItems = new ArrayList<>(c.items);
                    if (c.separateOffsets == null) c.separateOffsets = new HashMap<>();
                    if (c.mode == null) c.mode = Mode.LIST;
                    if (c.invseeScale < 50) c.invseeScale = 100;
                    if (c.hudScale < 50) c.hudScale = 100;
                    instance = c;
                }
            } catch (Exception e) {
                InvSeeSummaryClient.LOGGER.warn("Konnte Config nicht laden, nutze Standardwerte", e);
            }
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            InvSeeSummaryClient.LOGGER.warn("Konnte Config nicht speichern", e);
        }
    }

    public List<String> effectiveHudItems() {
        return hudSameItems ? items : hudItems;
    }

    public void resetPositions() {
        listOffsetX = 0;
        listOffsetY = 0;
        separateOffsets.clear();
        hudX = 4;
        hudY = 4;
    }
}
