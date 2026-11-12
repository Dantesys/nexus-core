package org.dantesys.nexus.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.dantesys.nexus.blocks.blockentity.ColetorArcanoBasicoBE;
import org.dantesys.nexus.blocks.blockentity.ModBlockEntities;
import org.jspecify.annotations.Nullable;

public class ColetorArcanoBasico extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 12, 12);

    public ColetorArcanoBasico(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColetorArcanoBasicoBE(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ColetorArcanoBasicoBE coletor) {
            if (player.isShiftKeyDown()) {
                BlockPos target = coletor.cycleConfiguredTarget(player) ? coletor.getConfiguredTarget() : null;
                if (target == null) {
                    player.sendSystemMessage(Component.translatable("message.nexus.coletor.alvo_automatico"));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            "message.nexus.coletor.alvo_definido",
                            target.getX(), target.getY(), target.getZ()));
                }
                return InteractionResult.SUCCESS;
            }

            BlockPos target = coletor.getConfiguredTarget();
            player.sendSystemMessage(Component.translatable(
                    "message.nexus.coletor.status", coletor.getMana(), ColetorArcanoBasicoBE.CAPACITY));
            player.sendSystemMessage(target == null
                    ? Component.translatable("message.nexus.coletor.alvo_atual_automatico", coletor.getRange())
                    : Component.translatable("message.nexus.coletor.alvo_atual",
                    target.getX(), target.getY(), target.getZ()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(
                type, ModBlockEntities.COLETOR_ARCANO_BASICO_BE.get(), ColetorArcanoBasicoBE::serverTick);
    }
}
