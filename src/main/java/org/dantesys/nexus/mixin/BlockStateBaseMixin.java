package org.dantesys.nexus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.dantesys.nexus.effects.Subespaco;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Subespaco da Rifty: para quem esta nele, todo bloco quebravel se comporta como andaime. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void nexus$subespaco(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext entityContext)) return;
        if (!(entityContext.getEntity() instanceof LivingEntity living) || !Subespaco.active(living)) return;
        BlockState state = (BlockState) (Object) this;
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return; // bedrock e inquebraveis continuam solidos
        boolean standOnTop = entityContext.isAbove(Shapes.block(), pos, true) && !entityContext.isDescending();
        cir.setReturnValue(standOnTop ? Shapes.block() : Shapes.empty());
    }
}
