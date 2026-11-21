package de.invseesummary;

import de.invseesummary.mixin.HandledScreenAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Zusammenfassung neben einem geöffneten Inventar (InvSee oder – optional – jeder Kiste), verschiebbar per Maus */
public class SummaryOverlay {
    private static final Map<AbstractContainerScreen<?>, SummaryOverlay> ACTIVE = new WeakHashMap<>();
    private static final int PAD = 4;
    private static final int GAP = 6;
    private final AbstractContainerScreen<?> screen;
    private final String title;
    private final boolean invsee;
    private final List<HitBox> hitBoxes = new ArrayList<>();
    private boolean names;
    private String dragKey;
    private double dragMouseX;
    private double dragMouseY;
    private int dragStartX;
    private int dragStartY;

    private record HitBox(String key, int x, int y, int w, int h) {}

    private SummaryOverlay(AbstractContainerScreen<?> screen, String title, boolean invsee) {
        this.screen = screen;
        this.title = title;
        this.invsee = invsee;
    }

    public static SummaryOverlay previous(AbstractContainerScreen<?> screen) {
        return ACTIVE.get(screen);
    }

    public static void attach(AbstractContainerScreen<?> screen, String title, boolean invsee) {
        SummaryOverlay o = ACTIVE.computeIfAbsent(screen, s -> new SummaryOverlay(s, title, invsee));
        ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, delta) -> o.render(g, mouseX, mouseY));
        ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> click.button() != 0 || !o.startDrag(click.x(), click.y()));
        ScreenMouseEvents.allowMouseRelease(screen).register((s, click) -> !o.stopDrag());
        ScreenEvents.remove(screen).register(s -> o.stopDrag());
    }

    public String title() {
        return title;
    }

    public boolean isInvsee() {
        return invsee;
    }

    /** nur die Slots des fremden Inventars (nicht das eigene) */
    private List<ItemStack> containerStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof Inventory) continue;
            out.add(slot.getItem());
        }
        return out;
    }

    private static Map<String, Integer> countAll(List<ItemStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack s : stacks) {
            if (s.isEmpty()) continue;
            counts.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
        }
        return counts;
    }

    public void captureForHud() {
        if (!invsee) return;
        List<ItemStack> stacks = containerStacks();
        InvSeeHud.update(title, countAll(stacks), SummaryPanel.findArmor(stacks));
    }

    private void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Config cfg = Config.get();
        List<ItemStack> stacks = containerStacks();
        Map<String, Integer> counts = countAll(stacks);
        List<ItemStack> armor = SummaryPanel.findArmor(stacks);
        if (invsee) InvSeeHud.update(title, counts, armor);
        if (!cfg.summary) return;
        Font font = Minecraft.getInstance().font;
        List<SummaryPanel.Row> rows = SummaryPanel.itemRows(cfg.items, counts);
        if (cfg.showArmor) rows.addAll(SummaryPanel.armorRows(armor));
        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int left = acc.invseesummary$getX();
        int right = left + acc.invseesummary$getBackgroundWidth();
        int top = acc.invseesummary$getY();
        float scale = cfg.invseeScale / 100.0f;
        int room = Math.max(left, screen.width - right) - GAP;
        // Namen nur, wenn genug Platz ist
        names = cfg.itemNames && neededWidth(font, rows, true) * scale <= room;
        int needed = Math.round(neededWidth(font, rows, names) * scale);
        boolean rightSide = left - GAP - needed < 0 && screen.width - right - GAP >= needed;
        int anchorX = rightSide ? right + GAP + needed : left - GAP;
        if (dragKey != null) updateDrag(mouseX, mouseY);
        hitBoxes.clear();
        if (cfg.mode == Config.Mode.LIST) {
            int w = neededWidth(font, rows, names);
            int h = SummaryPanel.height(rows);
            int sw = Math.round(w * scale), sh = Math.round(h * scale);
            int x = clampX(anchorX - sw + cfg.listOffsetX, sw);
            int y = clampY(top + cfg.listOffsetY, sh);
            SummaryPanel.drawList(g, font, title, null, rows, x, y, w, h, names, scale);
            hitBoxes.add(new HitBox("list", x, y, sw, sh));
        } else {
            int bg = SummaryPanel.background(), border = SummaryPanel.border();
            int y0 = top;
            for (SummaryPanel.Row row : rows) {
                int w = separateWidth(font, row, names);
                int h = 22;
                int sw = Math.round(w * scale), sh = Math.round(h * scale);
                int[] off = cfg.separateOffsets.getOrDefault(row.key(), new int[] {0, 0});
                int x = clampX(anchorX - sw + off[0], sw);
                int y = clampY(y0 + off[1], sh);
                g.pose().pushMatrix();
                g.pose().translate(x, y);
                g.pose().scale(scale, scale);
                Ui.box(g, 0, 0, w, h, bg, border);
                g.item(row.icon(), 4, 3);
                int tx = 24;
                if (row.showsName(names)) {
                    g.text(font, row.name(), tx, 7, row.nameColor(), true);
                    tx += font.width(row.name()) + 8;
                }
                g.text(font, row.value(), tx, 7, row.valueColor(), true);
                g.pose().popMatrix();
                hitBoxes.add(new HitBox(row.key(), x, y, sw, sh));
                y0 += sh + 2;
            }
        }
    }

    private int neededWidth(Font font, List<SummaryPanel.Row> rows, boolean names) {
        if (Config.get().mode == Config.Mode.LIST) return SummaryPanel.width(font, title, null, rows, names);
        int w = 0;
        for (SummaryPanel.Row r : rows) w = Math.max(w, separateWidth(font, r, names));
        return w;
    }

    private static int separateWidth(Font font, SummaryPanel.Row row, boolean names) {
        return 24 + (row.showsName(names) ? font.width(row.name()) + 8 : 0) + font.width(row.value()) + 4;
    }

    private int clampX(int x, int w) {
        return Math.max(0, Math.min(x, screen.width - w));
    }

    private int clampY(int y, int h) {
        return Math.max(0, Math.min(y, screen.height - h));
    }

    private boolean startDrag(double mx, double my) {
        for (int i = hitBoxes.size() - 1; i >= 0; i--) {
            HitBox b = hitBoxes.get(i);
            if (!Ui.inside(mx, my, b.x, b.y, b.w, b.h)) continue;
            dragKey = b.key;
            dragMouseX = mx;
            dragMouseY = my;
            int[] off = currentOffset(b.key);
            dragStartX = off[0];
            dragStartY = off[1];
            return true;
        }
        return false;
    }

    private void updateDrag(double mx, double my) {
        int ox = dragStartX + (int) Math.round(mx - dragMouseX);
        int oy = dragStartY + (int) Math.round(my - dragMouseY);
        Config cfg = Config.get();
        if (dragKey.equals("list")) {
            cfg.listOffsetX = ox;
            cfg.listOffsetY = oy;
        } else {
            cfg.separateOffsets.put(dragKey, new int[] {ox, oy});
        }
    }

    private int[] currentOffset(String key) {
        Config cfg = Config.get();
        if (key.equals("list")) return new int[] {cfg.listOffsetX, cfg.listOffsetY};
        return cfg.separateOffsets.getOrDefault(key, new int[] {0, 0});
    }

    private boolean stopDrag() {
        if (dragKey == null) return false;
        dragKey = null;
        Config.save();
        return true;
    }
}
