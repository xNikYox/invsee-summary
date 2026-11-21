package de.invseesummary.mixin;

import de.invseesummary.AutoClose;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ein gleich wieder schließendes /invsee (Taste „wiederholen“) wird gar nicht erst gezeichnet */
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void invseesummary$hideAutoClosed(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (AutoClose.isHidden((Screen) (Object) this)) ci.cancel();
    }
}
