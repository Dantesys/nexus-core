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
import org.dantesys.nexus.magic.ElementalStatus;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    public static void initializeMagicStatus(Player player, RandomSource random) {
        MagicStatus stats = player.getData(ModAttachmentType.MAGIC_STATS);
        if (stats.isCreated()) {
            return;
        }
        stats.setMaxMana(random.nextInt(1001));
        stats.setRegeneracao(random.nextInt(1001));
        stats.setMana(stats.getMaxMana());
        desbloquearAleatorios(stats, quantidadeAleatoria(random), random);
        stats.setCreated(true);
        player.syncData(ModAttachmentType.MAGIC_STATS);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ManaAdvancements.check(serverPlayer, stats);
        }
    }

    private static int quantidadeAleatoria(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 10) return 0;
        if (roll < 30) return 1;
        if (roll < 50) return 2;
        if (roll < 80) return 3;
        if (roll < 85) return 4;
        if (roll < 90) return 5;
        return Elementos.values().length - 1;
    }

    private static void desbloquearAleatorios(MagicStatus stats, int quantidade, RandomSource random) {
        List<Elementos> elementos = new ArrayList<>(Arrays.asList(Elementos.values()));
        elementos.remove(Elementos.NAO_ELEMENTAL);
        Collections.shuffle(elementos, new Random(random.nextLong()));
        ElementalStatus status = stats.get(Elementos.NAO_ELEMENTAL);
        status.setUnlocked(true);
        status.setAfinidade(1000);
        status.setRessonancia(1000);
        for (int i = 0; i < quantidade && i < elementos.size(); i++) {
            status = stats.get(elementos.get(i));
            status.setUnlocked(true);
            status.setAfinidade(random.nextInt(1001));
            status.setRessonancia(random.nextInt(1001));
        }
    }
}
