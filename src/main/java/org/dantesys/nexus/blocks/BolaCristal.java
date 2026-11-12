package org.dantesys.nexus.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.dantesys.nexus.blocks.blockentity.BolaCristalBE;
import org.dantesys.nexus.advancement.ManaAdvancements;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;
import org.jspecify.annotations.Nullable;


public class BolaCristal extends BaseEntityBlock {

    public BolaCristal(Properties properties) {
        super(properties);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BolaCristalBE(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return box(0, 0, 0, 16, 22, 16);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        initializeMagicStatus(player, level.getRandom());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BolaCristalBE bolaCristal && bolaCristal.insertHeldItem(player, stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        initializeMagicStatus(player, level.getRandom());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BolaCristalBE bolaCristal)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            bolaCristal.returnItems(player);
        } else {
            player.openMenu(bolaCristal, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Sorteia os 4 status de mana (1 a 1000) na primeira vez. Não faz nada se já foram criados. */
    public static void initializeMagicStatus(Player player, RandomSource random) {
        MagicStatus stats = player.getData(ModAttachmentType.MAGIC_STATS);
        if (stats.isCreated()) {
            return;
        }
        // Mínimo 1: com 0 o jogador ficava sem mana e sem regeneração para sempre.
        stats.setMaxMana(random.nextInt(1, MagicStatus.MAX_STATUS + 1));
        stats.setRegeneracao(random.nextInt(1, MagicStatus.MAX_STATUS + 1));
        stats.setAfinidade(random.nextInt(1, MagicStatus.MAX_STATUS + 1));
        stats.setRessonancia(random.nextInt(1, MagicStatus.MAX_STATUS + 1));
        stats.setMana(stats.getMaxMana());
        stats.setCreated(true);
        player.syncData(ModAttachmentType.MAGIC_STATS);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ManaAdvancements.check(serverPlayer, stats);
        }
    }
}