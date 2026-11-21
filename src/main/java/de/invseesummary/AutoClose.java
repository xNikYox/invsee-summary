package de.invseesummary;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * „Letztes /invsee wiederholen“ per Taste: das Inventar wird unsichtbar geöffnet, sobald der Inhalt da ist für das
 * HUD erfasst und sofort wieder geschlossen (spätestens nach 10 Ticks).
 */
public final class AutoClose {
    private static final int MAX_TICKS = 10;
    private static AbstractContainerScreen<?> screen;
    private static int ticks;

    private AutoClose() {}

    public static void track(AbstractContainerScreen<?> s) {
        screen = s;
        ticks = 0;
    }

    public static boolean isHidden(Screen s) {
        return s != null && s == screen;
    }

    public static void onInventoryReceived(int containerId) {
        if (screen != null && screen.getMenu().containerId == containerId) close(Minecraft.getInstance());
    }

    public static void tick(Minecraft mc) {
        if (screen == null) return;
        if (mc.screen != screen || mc.player == null) {
            screen = null;
            return;
        }
        ticks++;
        if ((ticks >= 1 && hasContent()) || ticks >= MAX_TICKS) close(mc);
    }

    private static void close(Minecraft mc) {
        if (screen == null || mc.player == null || mc.screen != screen) {
            screen = null;
            return;
        }
        SummaryOverlay o = SummaryOverlay.previous(screen);
        if (o != null) o.captureForHud();
        screen = null;
        mc.player.closeContainer();
    }

    private static boolean hasContent() {
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory) && slot.hasItem()) return true;
        }
        return false;
    }
}
