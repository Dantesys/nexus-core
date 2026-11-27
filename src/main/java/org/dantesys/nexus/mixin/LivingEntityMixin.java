package org.dantesys.nexus.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.dantesys.nexus.effects.Subespaco;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Subespaco da Rifty: dentro de um bloco solido da para subir pulando (como andaime); agachado, desce. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void nexus$subespaco(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isSpectator()) return;
        // Morpix: DNA de aranha escala paredes.
        if (self instanceof net.minecraft.world.entity.player.Player player && self.horizontalCollision
                && player.getData(org.dantesys.nexus.data.ModAttachmentType.MORPH_POWERS).contains("minecraft:spider")) {
            cir.setReturnValue(true);
            return;
        }
        if (!Subespaco.active(self)) return;
        if (self.isShiftKeyDown()) {
            cir.setReturnValue(false);
            return;
        }
        BlockState state = self.getInBlockState();
        if (!state.isAir() && !state.getCollisionShape(self.level(), self.blockPosition()).isEmpty()) cir.setReturnValue(true);
    }
}
