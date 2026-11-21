package de.invseesummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Einstellungen: Anzeige-Schalter, Modus, Größen, gezählte Items (InvSee/HUD) und Item-Auswahl mit Suche */
public class ConfigScreen extends Screen {
    private static final int SLOT = 20;
    private final List<Button> buttons = new ArrayList<>();
    private final List<Item> allItems = new ArrayList<>();
    private List<Item> filtered = new ArrayList<>();
    private EditBox search;
    private int page;
    private final List<Slider> sliders = new ArrayList<>();
    private Slider draggingSlider;
    private int px, py, pw, ph;
    private int countedY, addLabelY;
    private int gridX, gridY, cols, rows;
    private boolean editingHud;

    private record Button(int x, int y, int w, int h, Component label, BooleanSupplier active, Runnable action, boolean toggle, BooleanSupplier visible) {
        Button(int x, int y, int w, int h, Component label, BooleanSupplier active, Runnable action, boolean toggle) {
            this(x, y, w, h, label, active, action, toggle, () -> true);
        }
    }

    private record Slider(int x, int y, int w, String label, IntSupplier get, IntConsumer set, int min, int max, int step) {
        void setFromMouse(double mx) {
            double f = Math.max(0.0, Math.min(1.0, (mx - x - 2.0) / (w - 4)));
            int v = (int) Math.round((min + f * (max - min)) / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }

        float fraction() {
            return (get.getAsInt() - min) / (float) (max - min);
        }
    }

    public ConfigScreen() {
        super(Component.literal("InvSee Summary"));
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) allItems.add(item);
        }
        filtered = allItems;
    }

    private List<String> currentList() {
        Config cfg = Config.get();
        return editingHud ? cfg.effectiveHudItems() : cfg.items;
    }

    @Override
    protected void init() {
        pw = Math.min(360, width - 16);
        ph = Math.min(300, height - 8);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int inner = pw - 16;
        int x = px + 8;
        Config cfg = Config.get();
        buttons.clear();
        buttons.add(new Button(px + pw - 52, py + 6, 44, 16, Component.literal("Fertig"), () -> true, this::onClose, false));
        int y = py + 44;
        int bw = (inner - 12) / 4;
        buttons.add(new Button(x, y, bw, 16, Component.literal("Summary"), () -> cfg.summary, () -> cfg.summary = !cfg.summary, true));
        buttons.add(new Button(x + bw + 4, y, bw, 16, Component.literal("HUD"), () -> cfg.hud, () -> cfg.hud = !cfg.hud, true));
        buttons.add(new Button(x + (bw + 4) * 2, y, bw, 16, Component.literal("Namen"), () -> cfg.itemNames, () -> cfg.itemNames = !cfg.itemNames, true));
        buttons.add(new Button(x + (bw + 4) * 3, y, inner - (bw + 4) * 3, 16, Component.literal("Jede Kiste"), () -> cfg.everyChest, () -> cfg.everyChest = !cfg.everyChest, true));
        y += 20;
        int mw = 56, aw = 76;
        buttons.add(new Button(x, y, mw, 16, Component.literal("Liste"), () -> cfg.mode == Config.Mode.LIST, () -> cfg.mode = Config.Mode.LIST, false));
        buttons.add(new Button(x + mw + 4, y, mw, 16, Component.literal("Einzeln"), () -> cfg.mode == Config.Mode.SEPARATE, () -> cfg.mode = Config.Mode.SEPARATE, false));
        buttons.add(new Button(x + (mw + 4) * 2, y, aw, 16, Component.literal("Rüstung"), () -> cfg.showArmor, () -> cfg.showArmor = !cfg.showArmor, true));
        int rx = x + (mw + 4) * 2 + aw + 4;
        buttons.add(new Button(rx, y, x + inner - rx, 16, Component.literal("Positionen zurücksetzen"), () -> false, cfg::resetPositions, false));
        y += 20;
        sliders.clear();
        int sw = (inner - 8) / 3;
        sliders.add(new Slider(x, y, sw, "Hintergrund", () -> cfg.background, v -> cfg.background = v, 0, 100, 1));
        sliders.add(new Slider(x + sw + 4, y, sw, "Größe InvSee", () -> cfg.invseeScale, v -> cfg.invseeScale = v, 50, 200, 5));
        sliders.add(new Slider(x + (sw + 4) * 2, y, inner - (sw + 4) * 2, "Größe HUD", () -> cfg.hudScale, v -> cfg.hudScale = v, 50, 200, 5));
        countedY = y + 34;
        addLabelY = countedY + 26;
        int tabY = countedY - 16;
        int right = x + inner;
        buttons.add(new Button(right - 60, tabY, 60, 13, Component.literal("Außerhalb"), () -> editingHud, () -> editingHud = true, false));
        buttons.add(new Button(right - 124, tabY, 60, 13, Component.literal("Im InvSee"), () -> !editingHud, () -> editingHud = false, false));
        buttons.add(new Button(right - 230, tabY, 102, 13, Component.literal("= InvSee-Liste"), () -> cfg.hudSameItems, () -> {
            cfg.hudSameItems = !cfg.hudSameItems;
            if (!cfg.hudSameItems) cfg.hudItems = new ArrayList<>(cfg.items);
        }, true, () -> editingHud));
        int searchY = addLabelY + 12;
        search = new EditBox(font, x + 1, searchY + 1, inner - 46, 14, Component.literal("Suche"));
        search.setHint(Component.literal("Items suchen, z. B. perle oder apfel").withColor(Ui.TEXT_DIM));
        search.setResponder(this::applySearch);
        addRenderableWidget(search);
        buttons.add(new Button(x + inner - 40, searchY, 18, 16, Component.literal("<"), () -> false, () -> page = Math.max(0, page - 1), false));
        buttons.add(new Button(x + inner - 18, searchY, 18, 16, Component.literal(">"), () -> false, () -> page = Math.min(pageCount() - 1, page + 1), false));
        gridY = searchY + 21;
        cols = Math.max(1, inner / SLOT);
        rows = Math.max(1, (py + ph - 6 - gridY) / SLOT);
        gridX = x + (inner - cols * SLOT) / 2;
        page = Math.min(page, pageCount() - 1);
    }

    private void applySearch(String text) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            filtered = allItems;
        } else {
            filtered = new ArrayList<>();
            for (Item item : allItems) {
                String name = item.getName(new ItemStack(item)).getString().toLowerCase(Locale.ROOT);
                String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                if (name.contains(q) || path.contains(q)) filtered.add(item);
            }
        }
        page = 0;
    }

    private int perPage() {
        return cols * rows;
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + perPage() - 1) / perPage());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int left = px + 8;
        int right = px + pw - 8;
        Ui.box(g, px, py, pw, ph, Ui.PANEL, Ui.PANEL_BORDER);
        g.item(new ItemStack(Items.TOTEM_OF_UNDYING), px + 6, py + 7);
        g.text(font, "InvSee Summary", px + 26, py + 6, Ui.TEXT, false);
        g.text(font, "Was beim /invsee gezählt wird", px + 26, py + 16, Ui.TEXT_DIM, false);
        g.fill(px + 1, py + 28, px + pw - 1, py + 29, Ui.ACCENT);
        sectionLabel(g, "Anzeige", null, left, right, py + 33);
        for (Button b : buttons) {
            if (b.visible.getAsBoolean()) drawButton(g, b, mouseX, mouseY);
        }
        for (Slider s : sliders) {
            boolean hover = Ui.inside(mouseX, mouseY, s.x, s.y, s.w, 16) || draggingSlider == s;
            Ui.box(g, s.x, s.y, s.w, 16, hover ? Ui.BUTTON_HOVER : Ui.BUTTON, Ui.BUTTON_BORDER);
            int fw = Math.round((s.w - 2) * s.fraction());
            g.fill(s.x + 1, s.y + 1, s.x + 1 + fw, s.y + 15, Ui.ACTIVE);
            g.fill(s.x + fw - 1, s.y + 1, s.x + fw + 1, s.y + 15, Ui.ACTIVE_BORDER);
            String label = s.label + " " + s.get.getAsInt() + "%";
            g.text(font, label, s.x + (s.w - font.width(label)) / 2, s.y + 4, Ui.TEXT, true);
        }
        List<String> list = currentList();
        String header = (editingHud ? "Außerhalb (HUD)  " : "Im InvSee  ") + list.size() + "/12";
        g.text(font, header, left, countedY - 13, Ui.TEXT_DIM, false);
        Item hovered = null;
        for (int i = 0; i < list.size(); i++) {
            Item item = itemOf(list.get(i));
            if (item == null) continue;
            int bx = left + i * SLOT;
            boolean hover = Ui.inside(mouseX, mouseY, bx, countedY, 18, 18);
            Ui.box(g, bx, countedY, 18, 18, hover ? 0xFF4A2A2A : Ui.BUTTON, hover ? Ui.RED : Ui.ACCENT);
            g.item(new ItemStack(item), bx + 1, countedY + 1);
            if (hover) hovered = item;
        }
        if (list.isEmpty()) g.text(font, "Klicke unten auf Items, um sie hinzuzufügen", left, countedY + 5, Ui.TEXT_DIM, false);
        String pageText = "Seite " + (page + 1) + "/" + pageCount() + ", " + filtered.size() + " Items";
        sectionLabel(g, "Items hinzufügen", pageText, left, right, addLabelY);
        int start = page * perPage();
        for (int i = 0; i < perPage() && start + i < filtered.size(); i++) {
            Item item = filtered.get(start + i);
            int gx = gridX + i % cols * SLOT;
            int gy = gridY + i / cols * SLOT;
            boolean selected = list.contains(BuiltInRegistries.ITEM.getKey(item).toString());
            boolean hover = Ui.inside(mouseX, mouseY, gx, gy, 18, 18);
            Ui.box(g, gx, gy, 18, 18, hover ? Ui.BUTTON_HOVER : Ui.BUTTON, selected ? Ui.ACCENT : Ui.BUTTON_BORDER);
            g.item(new ItemStack(item), gx + 1, gy + 1);
            if (hover) hovered = item;
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (hovered != null) g.setTooltipForNextFrame(font, new ItemStack(hovered), mouseX, mouseY);
    }

    private void sectionLabel(GuiGraphicsExtractor g, String label, String rightText, int left, int right, int y) {
        g.text(font, label, left, y, Ui.TEXT_DIM, false);
        int lineStart = left + font.width(label) + 6;
        int lineEnd = right;
        if (rightText != null) {
            int rw = font.width(rightText);
            g.text(font, rightText, right - rw, y, Ui.TEXT_DIM, false);
            lineEnd = right - rw - 6;
        }
        if (lineEnd > lineStart) g.fill(lineStart, y + 4, lineEnd, y + 5, Ui.PANEL_BORDER);
    }

    private void drawButton(GuiGraphicsExtractor g, Button b, int mouseX, int mouseY) {
        boolean hover = Ui.inside(mouseX, mouseY, b.x, b.y, b.w, b.h);
        boolean on = b.active.getAsBoolean();
        if (b.toggle) {
            Ui.box(g, b.x, b.y, b.w, b.h, hover ? Ui.BUTTON_HOVER : Ui.BUTTON, Ui.BUTTON_BORDER);
            g.text(font, b.label, b.x + 6, b.y + 4, on ? Ui.TEXT : Ui.TEXT_DIM, false);
            int sx = b.x + b.w - 22, sy = b.y + 4;
            g.fill(sx, sy, sx + 16, sy + 8, on ? 0xFF4CC26A : 0xFF2A2E38);
            int kx = on ? sx + 9 : sx + 1;
            g.fill(kx, sy + 1, kx + 6, sy + 7, 0xFFF0F0F0);
        } else {
            int fill = on ? Ui.ACTIVE : hover ? Ui.BUTTON_HOVER : Ui.BUTTON;
            int border = on ? Ui.ACTIVE_BORDER : Ui.BUTTON_BORDER;
            Ui.box(g, b.x, b.y, b.w, b.h, fill, border);
            int lw = font.width(b.label);
            g.text(font, b.label, b.x + (b.w - lw) / 2, b.y + 4, on || hover ? Ui.TEXT : 0xFFC4C8D2, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            for (Button b : buttons) {
                if (b.visible.getAsBoolean() && Ui.inside(mx, my, b.x, b.y, b.w, b.h)) {
                    b.action.run();
                    Config.save();
                    search.setFocused(false);
                    return true;
                }
            }
            for (Slider s : sliders) {
                if (Ui.inside(mx, my, s.x, s.y, s.w, 16)) {
                    draggingSlider = s;
                    s.setFromMouse(mx);
                    return true;
                }
            }
            List<String> list = currentList();
            for (int i = 0; i < list.size(); i++) {
                if (Ui.inside(mx, my, px + 8 + i * SLOT, countedY, 18, 18)) {
                    list.remove(i);
                    Config.save();
                    return true;
                }
            }
            int start = page * perPage();
            for (int i = 0; i < perPage() && start + i < filtered.size(); i++) {
                int gx = gridX + i % cols * SLOT;
                int gy = gridY + i / cols * SLOT;
                if (!Ui.inside(mx, my, gx, gy, 18, 18)) continue;
                String id = BuiltInRegistries.ITEM.getKey(filtered.get(start + i)).toString();
                if (list.contains(id)) list.remove(id);
                else if (list.size() < Config.MAX_ITEMS) list.add(id);
                Config.save();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingSlider != null) {
            draggingSlider.setFromMouse(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider != null) {
            draggingSlider = null;
            Config.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (my >= gridY) {
            page = Math.max(0, Math.min(pageCount() - 1, page - (int) Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private static Item itemOf(String id) {
        Identifier i = Identifier.tryParse(id);
        if (i == null || !BuiltInRegistries.ITEM.containsKey(i)) return null;
        return BuiltInRegistries.ITEM.getValue(i);
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
