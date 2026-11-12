package org.dantesys.nexus.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.dantesys.nexus.blocks.blockentity.TanqueForjaBE;
import org.jspecify.annotations.Nullable;

public class TanqueForja extends BaseEntityBlock {
    public static final MapCodec<TanqueForja> CODEC = simpleCodec(TanqueForja::new);

    public TanqueForja(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TanqueForjaBE(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TanqueForjaBE tank)) return InteractionResult.PASS;
        if (stack.is(Items.LAVA_BUCKET)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!tank.addBucket()) return InteractionResult.FAIL;
            if (!player.hasInfiniteMaterials()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            player.sendSystemMessage(tank.status());
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(Items.BUCKET)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!tank.removeBucket()) return InteractionResult.FAIL;
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
                ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
                if (!player.addItem(lava)) player.drop(lava, false);
            }
            player.sendSystemMessage(tank.status());
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TanqueForjaBE tank) {
            player.sendSystemMessage(tank.status());
        }
        return InteractionResult.SUCCESS;
    }
}
