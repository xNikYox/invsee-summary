package de.invseesummary.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Position und Breite des Inventar-Fensters */
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("leftPos")
    int invseesummary$getX();

    @Accessor("topPos")
    int invseesummary$getY();

    @Accessor("imageWidth")
    int invseesummary$getBackgroundWidth();
}
