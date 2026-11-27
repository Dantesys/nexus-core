package org.dantesys.nexus.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.dantesys.nexus.relic.RelicMinions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mini Wither da Necrix: sem barra de boss. */
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @Inject(method = "startSeenByPlayer", at = @At("HEAD"), cancellable = true)
    private void nexus$noBossBar(ServerPlayer player, CallbackInfo ci) {
        if (RelicMinions.isMinion((WitherBoss) (Object) this)) ci.cancel();
    }
}
