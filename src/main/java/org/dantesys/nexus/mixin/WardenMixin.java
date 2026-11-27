package org.dantesys.nexus.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.dantesys.nexus.relic.SculkyEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eco Vivo da Sculky: o Warden nao ataca nem se irrita com quem segura a reliquia. */
@Mixin(Warden.class)
public abstract class WardenMixin {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void nexus$ignoreHolder(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && SculkyEffects.echo(entity)) cir.setReturnValue(false);
    }

    @Inject(method = "increaseAngerAt(Lnet/minecraft/world/entity/Entity;IZ)V", at = @At("HEAD"), cancellable = true)
    private void nexus$noAnger(Entity entity, int amount, boolean playSound, CallbackInfo ci) {
        if (entity != null && SculkyEffects.echo(entity)) ci.cancel();
    }
}
