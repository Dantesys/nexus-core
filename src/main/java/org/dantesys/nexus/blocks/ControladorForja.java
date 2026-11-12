package org.dantesys.nexus.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.dantesys.nexus.blocks.blockentity.ControladorForjaBE;
import org.dantesys.nexus.blocks.blockentity.ModBlockEntities;
import org.dantesys.nexus.items.ModItems;
import org.jspecify.annotations.Nullable;

public class ControladorForja extends BaseEntityBlock {
    public static final MapCodec<ControladorForja> CODEC = simpleCodec(ControladorForja::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ControladorForja(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControladorForjaBE(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) level.setBlock(pos, state.setValue(FACING, placer.getDirection().getOpposite()), 2);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(FORMED)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof ControladorForjaBE forge) {
                if (!forge.insertLavaBucket(player, stack)) {
                    player.openMenu(forge, pos);
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!stack.is(ModItems.NUCLEO_FORJA.get())) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (state.getValue(FORMED)) return InteractionResult.FAIL;

        Direction right = state.getValue(FACING).getClockWise();
        Direction back = state.getValue(FACING).getOpposite();
        BlockPos[] positions = positions(pos, right, back);
        Block[] expected = expectedBlocks();

        boolean complete = true;
        for (int i = 0; i < positions.length; i++) {
            if (!level.getBlockState(positions[i]).is(expected[i])) {
                complete = false;
            }
        }
        if (!complete) {
            if (level.getBlockEntity(pos) instanceof ControladorForjaBE controller) {
                controller.setShowingPreview(true);
            }
            player.sendSystemMessage(Component.translatable("message.nexus.controlador_forja.incompleta"));
            return InteractionResult.FAIL;
        }

        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        for (BlockPos componentPos : positions) {
            level.setBlock(componentPos, ModBlocks.FORJA_COLLISION.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(pos) instanceof ControladorForjaBE controller) {
            controller.setShowingPreview(false);
        }
        level.setBlock(pos, state.setValue(FORMED, true), 3);
        player.sendSystemMessage(Component.translatable("message.nexus.controlador_forja.completa"));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(FORMED)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ControladorForjaBE forge) {
            player.openMenu(forge, pos);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(
                type, ModBlockEntities.CONTROLADOR_FORJA_BE.get(), ControladorForjaBE::serverTick);
    }

    public static BlockPos[] positions(BlockPos controller, Direction right, Direction back) {
        BlockPos upper = controller.above();
        return new BlockPos[] {
                controller.relative(right), controller.relative(back), controller.relative(back).relative(right),
                upper, upper.relative(right), upper.relative(back), upper.relative(back).relative(right)
        };
    }

    public static Block[] expectedBlocks() {
        return new Block[] {
                ModBlocks.STEEL_BLOCK.get(), ModBlocks.STEEL_BLOCK.get(), ModBlocks.TANQUE_FORJA.get(),
                ModBlocks.INFUSOR_ARCANO_BASICO.get(), ModBlocks.COLETOR_ARCANO_BASICO.get(),
                ModBlocks.COLETOR_ARCANO_BASICO.get(), ModBlocks.TANQUE_FORJA.get()
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? formedShape(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.block();
    }

    private static VoxelShape formedShape(Direction facing) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        VoxelShape shape = Shapes.block();
        BlockPos origin = BlockPos.ZERO;
        for (BlockPos component : positions(origin, right, back)) {
            int x = component.getX();
            int y = component.getY();
            int z = component.getZ();
            shape = Shapes.or(shape, Block.box(x * 16.0, y * 16.0, z * 16.0,
                    (x + 1) * 16.0, (y + 1) * 16.0, (z + 1) * 16.0));
        }
        return shape.optimize();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(FORMED)) {
            Direction facing = state.getValue(FACING);
            for (BlockPos componentPos : positions(pos, facing.getClockWise(), facing.getOpposite())) {
                if (level.getBlockState(componentPos).is(ModBlocks.FORJA_COLLISION.get())) {
                    level.setBlock(componentPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
