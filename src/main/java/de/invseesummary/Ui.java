package de.invseesummary;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Farben und kleine Zeichenhilfen für alle Panels */
public final class Ui {
    public static final int PANEL = 0xF0161920;
    public static final int PANEL_BORDER = 0xFF2E333F;
    public static final int ACCENT = 0xFFE0A33A;
    public static final int BUTTON = 0xFF1D2028;
    public static final int BUTTON_HOVER = 0xFF262A34;
    public static final int BUTTON_BORDER = 0xFF3A4050;
    public static final int ACTIVE = 0xFF34406A;
    public static final int ACTIVE_BORDER = 0xFF6F82D6;
    public static final int TEXT = 0xFFE6E8EE;
    public static final int TEXT_DIM = 0xFF8C92A0;
    public static final int GREEN = 0xFF5FD35F;
    public static final int RED = 0xFFD65050;

    private Ui() {}

    /** gefüllter Kasten mit Rand */
    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.outline(x, y, w, h, border);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static int withAlpha(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 0xFFFFFF;
    }
}
