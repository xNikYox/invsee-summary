package de.invseesummary;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einstieg: erkennt /invsee-Befehle, hängt die Zusammenfassung an das nächste Inventar, Tasten, HUD, /invseesummary */
public class InvSeeSummaryClient implements ClientModInitializer {
    public static final String MOD_ID = "invseesummary";
    public static final Logger LOGGER = LoggerFactory.getLogger("InvSee Summary");
    private static final Pattern INVSEE_COMMAND = Pattern.compile("^(?:[\\w-]+:)?(?:invsee|openinv|inv|inventorysee|seeinv|isee)\\s+(\\S+).*$", Pattern.CASE_INSENSITIVE);
    private static final long INVSEE_WINDOW_MS = 5000;
    private static String pendingName;
    private static long pendingTime;
    private static boolean openConfigNextTick;
    private static boolean autoClosePending;
    private static KeyMapping openConfigKey;
    private static KeyMapping repeatInvseeKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.invseesummary.config", InputConstants.Type.KEYSYM, -1, category));
        repeatInvseeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.invseesummary.repeat_invsee", InputConstants.Type.KEYSYM, 73, category)); // I
        ClientSendMessageEvents.COMMAND.register(InvSeeSummaryClient::onCommand);
        registerCommands();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "invsee_hud"), InvSeeHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openConfigKey.consumeClick()) openConfigNextTick = true;
            while (repeatInvseeKey.consumeClick()) repeatLastInvsee(mc);
            if (openConfigNextTick) {
                openConfigNextTick = false;
                Compat.setScreen(mc, new ConfigScreen());
            }
            AutoClose.tick(mc);
        });
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (screen instanceof ChatScreen chat) {
                InvSeeHud.attachToChat(chat);
                return;
            }
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) return;
            Config cfg = Config.get();
            SummaryOverlay prev = SummaryOverlay.previous(container);
            if (prev != null) {
                // Bildschirm neu aufgebaut (z. B. Fenstergröße) – Anzeige wieder anhängen
                SummaryOverlay.attach(container, prev.title(), prev.isInvsee());
                return;
            }
            boolean fromInvsee = pendingName != null && System.currentTimeMillis() - pendingTime < INVSEE_WINDOW_MS;
            if (fromInvsee) {
                String name = pendingName;
                pendingName = null;
                if (cfg.summary || cfg.hud) SummaryOverlay.attach(container, name, true);
                if (autoClosePending) {
                    autoClosePending = false;
                    AutoClose.track(container);
                }
            } else if (cfg.everyChest && cfg.summary) {
                SummaryOverlay.attach(container, screen.getTitle().getString(), false);
            }
        });
    }

    /** jeder gesendete Befehl: /invsee <Name> merken (das nächste Inventar gehört dazu) */
    private static void onCommand(String command) {
        String cmd = command.trim();
        Matcher m = INVSEE_COMMAND.matcher(cmd);
        if (!m.matches()) return;
        autoClosePending = false;
        pendingName = m.group(1);
        pendingTime = System.currentTimeMillis();
        Config cfg = Config.get();
        if (!cmd.equals(cfg.lastInvseeCommand)) {
            cfg.lastInvseeCommand = cmd;
            Config.save();
        }
    }

    private static void repeatLastInvsee(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return;
        String cmd = Config.get().lastInvseeCommand;
        if (cmd == null) {
            Compat.actionBar(mc, Component.literal("Noch kein /invsee benutzt").withStyle(ChatFormatting.RED));
            return;
        }
        mc.getConnection().sendCommand(cmd);
        onCommand(cmd);
        autoClosePending = true;
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(ClientCommands.literal("invseesummary")
            .executes(c -> {
                openConfigNextTick = true;
                return 1;
            })
            .then(ClientCommands.literal("clear").executes(c -> {
                InvSeeHud.clear();
                c.getSource().sendFeedback(Component.literal("InvSee-HUD geleert.").withStyle(ChatFormatting.GRAY));
                return 1;
            }))));
    }

    public static Minecraft client() {
        return Minecraft.getInstance();
    }
}
