package de.invseesummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

/** Zeilen (Items, Rüstung) und das Listen-Panel – gemeinsam für InvSee-Anzeige und HUD */
public final class SummaryPanel {
    public static final int PAD = 4;
    public static final int ROW_H = 18;
    public static final int TITLE_H = 13;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ProtType[] PROT_TYPES = {
        new ProtType(Enchantments.PROTECTION, "Prot", 0xFF6FB7FF),
        new ProtType(Enchantments.BLAST_PROTECTION, "Blast", 0xFFFF9F43),
        new ProtType(Enchantments.FIRE_PROTECTION, "Fire", 0xFFFF5E3A),
        new ProtType(Enchantments.PROJECTILE_PROTECTION, "Proj", 0xFFC9A0FF),
    };
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private record ProtType(ResourceKey<Enchantment> key, String label, int color) {}

    /** eine Zeile: Symbol, Name (optional), Wert */
    public record Row(String key, ItemStack icon, Component name, int nameColor, String value, int valueColor, boolean alwaysName) {
        public boolean showsName(boolean names) {
            return names || alwaysName;
        }
    }

    private SummaryPanel() {}

    public static Item itemOf(String id) {
        Identifier i = Identifier.tryParse(id);
        if (i == null || !BuiltInRegistries.ITEM.containsKey(i)) return null;
        return BuiltInRegistries.ITEM.getValue(i);
    }

    public static List<Row> itemRows(List<String> ids, Map<String, Integer> counts) {
        List<Row> rows = new ArrayList<>();
        for (String id : ids) {
            Item item = itemOf(id);
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);
            int n = counts.getOrDefault(id, 0);
            rows.add(new Row(id, stack, stack.getHoverName(), n > 0 ? Ui.GREEN : Ui.TEXT_DIM, "×" + n, n > 0 ? Ui.TEXT : Ui.RED, false));
        }
        return rows;
    }

    public static List<Row> armorRows(List<ItemStack> armor) {
        List<Row> rows = new ArrayList<>();
        for (ItemStack s : armor) {
            int max = s.getMaxDamage();
            int left = max - s.getDamageValue();
            int color = durabilityColor((float) left / max);
            Component prot = protLabel(s);
            rows.add(new Row("armor:" + BuiltInRegistries.ITEM.getKey(s.getItem()), s, prot, Ui.TEXT, left + "/" + max, color, true));
        }
        return rows;
    }

    /** z. B. „Prot IV + Blast II“ in den Farben der Schutzart */
    public static Component protLabel(ItemStack s) {
        ItemEnchantments ench = s.getEnchantments();
        MutableComponent out = null;
        for (ProtType t : PROT_TYPES) {
            for (Holder<Enchantment> h : ench.keySet()) {
                if (!h.is(t.key())) continue;
                int lvl = ench.getLevel(h);
                String text = t.label() + " " + (lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl));
                MutableComponent part = Component.literal(text).withColor(t.color());
                out = out == null ? part : out.append(Component.literal(" + ").withColor(Ui.TEXT_DIM)).append(part);
            }
        }
        return out != null ? out : Component.literal("Kein Prot").withColor(Ui.TEXT_DIM);
    }

    public static int durabilityColor(float f) {
        if (f > 0.6f) return Ui.GREEN;
        if (f > 0.3f) return 0xFFE8C547;
        if (f > 0.15f) return 0xFFE8873A;
        return Ui.RED;
    }

    /** je Rüstungsplatz ein Teil – getragene (Slots ab 36) haben Vorrang vor Teilen im Inventar */
    public static List<ItemStack> findArmor(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack found = null;
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack s = stacks.get(i);
                if (!isArmorFor(s, slot)) continue;
                if (i >= 36) {
                    found = s;
                    break;
                }
                if (found == null) found = s;
            }
            if (found != null) out.add(found.copy());
        }
        return out;
    }

    private static boolean isArmorFor(ItemStack s, EquipmentSlot slot) {
        if (s.isEmpty() || !s.isDamageableItem()) return false;
        Equippable e = s.get(DataComponents.EQUIPPABLE);
        return e != null && e.slot() == slot;
    }

    public static int width(Font font, String title, String right, List<Row> rows, boolean names) {
        int value = 0, name = 0;
        for (Row r : rows) {
            value = Math.max(value, font.width(r.value));
            if (r.showsName(names)) name = Math.max(name, font.width(r.name));
        }
        int body = 20 + (name > 0 ? name + 12 : 0) + value;
        int head = font.width(title) + (right != null ? font.width(right) + 8 : 0);
        return Math.max(body, head) + 8;
    }

    public static int height(List<Row> rows) {
        return 17 + Math.max(1, rows.size()) * ROW_H + 2;
    }

    public static void drawList(GuiGraphicsExtractor g, Font font, String title, String right, List<Row> rows, int x, int y, int w, int h, boolean names, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        drawListUnscaled(g, font, title, right, rows, w, h, names);
        g.pose().popMatrix();
    }

    private static void drawListUnscaled(GuiGraphicsExtractor g, Font font, String title, String right, List<Row> rows, int w, int h, boolean names) {
        int x = 0, y = 0;
        int bg = background(), border = border();
        Ui.box(g, x, y, w, h, bg, border);
        g.text(font, title, x + PAD, y + PAD, Ui.TEXT, true);
        if (right != null) g.text(font, right, x + w - PAD - font.width(right), y + PAD, Ui.TEXT_DIM, true);
        g.fill(x + 1, y + TITLE_H + 1, x + w - 1, y + TITLE_H + 2, border);
        int ry = y + TITLE_H + PAD;
        if (rows.isEmpty()) g.text(font, "Keine Items gewählt", x + PAD, ry + 5, Ui.TEXT_DIM, true);
        for (Row r : rows) {
            g.item(r.icon, x + PAD, ry);
            if (r.showsName(names)) g.text(font, r.name, x + PAD + 20, ry + 4, r.nameColor, true);
            g.text(font, r.value, x + w - PAD - font.width(r.value), ry + 4, r.valueColor, true);
            ry += ROW_H;
        }
    }

    public static int background() {
        return Ui.withAlpha(0x14161C, Config.get().background * 255 / 100);
    }

    public static int border() {
        return Ui.withAlpha(0x2E333F, Math.max(Config.get().background, 25) * 255 / 100);
    }
}
