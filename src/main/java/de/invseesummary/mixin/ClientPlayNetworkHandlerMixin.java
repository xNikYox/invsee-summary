package de.invseesummary.mixin;

import de.invseesummary.AutoClose;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inventar-Inhalt angekommen → ein unsichtbar geöffnetes /invsee kann wieder schließen */
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void invseesummary$closeWhenFilled(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        AutoClose.onInventoryReceived(packet.containerId());
    }
}
