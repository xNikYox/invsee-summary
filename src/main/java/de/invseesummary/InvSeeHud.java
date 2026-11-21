package de.invseesummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.item.ItemStack;

/** HUD außerhalb des Inventars: letztes /invsee (Name, Items, Rüstung, wie lange her) – im Chat verschiebbar */
public final class InvSeeHud {
    private static String name;
    private static Map<String, Integer> counts = new HashMap<>();
    private static List<ItemStack> armor = new ArrayList<>();
    private static long time;
    private static int lastX;
    private static int lastY;
    private static int lastW;
    private static int lastH;
    private static boolean dragging;
    private static double dragMouseX;
    private static double dragMouseY;
    private static int dragStartX;
    private static int dragStartY;

    private InvSeeHud() {}

    public static void update(String player, Map<String, Integer> itemCounts, List<ItemStack> armorItems) {
        name = player;
        counts = itemCounts;
        armor = armorItems;
        time = System.currentTimeMillis();
    }

    public static void clear() {
        name = null;
        counts = new HashMap<>();
        armor = new ArrayList<>();
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Config cfg = Config.get();
        if (!cfg.hud || name == null || Compat.hudHidden(mc)) {
            lastW = 0;
            return;
        }
        Font font = mc.font;
        List<SummaryPanel.Row> rows = SummaryPanel.itemRows(cfg.effectiveHudItems(), counts);
        if (cfg.showArmor) rows.addAll(SummaryPanel.armorRows(armor));
        String ago = ago(System.currentTimeMillis() - time);
        float scale = cfg.hudScale / 100.0f;
        int w = SummaryPanel.width(font, name, ago, rows, cfg.itemNames);
        int h = SummaryPanel.height(rows);
        int sw = Math.round(w * scale), sh = Math.round(h * scale);
        int x = Math.max(0, Math.min(cfg.hudX, g.guiWidth() - sw));
        int y = Math.max(0, Math.min(cfg.hudY, g.guiHeight() - sh));
        SummaryPanel.drawList(g, font, name, ago, rows, x, y, w, h, cfg.itemNames, scale);
        lastX = x;
        lastY = y;
        lastW = sw;
        lastH = sh;
    }

    private static String ago(long ms) {
        long s = ms / 1000;
        if (s < 60) return "vor " + s + "s";
        if (s < 3600) return "vor " + s / 60 + "m";
        return "vor " + s / 3600 + "h";
    }

    /** im offenen Chat kann das HUD mit der linken Maustaste verschoben werden */
    public static void attachToChat(ChatScreen chat) {
        ScreenMouseEvents.allowMouseClick(chat).register((s, click) -> {
            if (click.button() != 0 || lastW == 0 || !Ui.inside(click.x(), click.y(), lastX, lastY, lastW, lastH)) return true;
            dragging = true;
            dragMouseX = click.x();
            dragMouseY = click.y();
            dragStartX = lastX;
            dragStartY = lastY;
            return false;
        });
        ScreenEvents.afterExtract(chat).register((s, g, mouseX, mouseY, delta) -> {
            if (!dragging) return;
            Config cfg = Config.get();
            cfg.hudX = dragStartX + (int) Math.round(mouseX - dragMouseX);
            cfg.hudY = dragStartY + (int) Math.round(mouseY - dragMouseY);
        });
        ScreenMouseEvents.allowMouseRelease(chat).register((s, click) -> {
            if (!dragging) return true;
            dragging = false;
            Config.save();
            return false;
        });
        ScreenEvents.remove(chat).register(s -> {
            if (dragging) {
                dragging = false;
                Config.save();
            }
        });
    }
}
