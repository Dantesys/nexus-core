package org.dantesys.nexus.mixin;

import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.dantesys.nexus.relic.OcultyEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Espiao Imaterial da Oculty: o modo espectador temporario nao pode usar o teleporte do espectador. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(method = "handleTeleportToEntityPacket", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexus$blockSpyTeleport(ServerboundTeleportToEntityPacket packet, CallbackInfo ci) {
        var player = ((ServerGamePacketListenerImpl) (Object) this).player;
        if (player != null && OcultyEffects.spying(player)) ci.cancel();
    }
}
