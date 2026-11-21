package de.invseesummary;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Stellen, an denen sich 26.1 und 26.2 unterscheiden – Fassung für 26.1.x */
final class Compat {
    private Compat() {}

    static Screen screen(Minecraft mc) {
        return mc.screen;
    }

    static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }

    /** F1: Oberfläche ausgeblendet */
    static boolean hudHidden(Minecraft mc) {
        return mc.options.hideGui;
    }

    /** Meldung über der Schnellleiste */
    static void actionBar(Minecraft mc, Component text) {
        mc.gui.setOverlayMessage(text, false);
    }
}
