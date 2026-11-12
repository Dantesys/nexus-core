package org.dantesys.nexus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.blocks.ControladorForja;
import org.dantesys.nexus.blocks.blockentity.ControladorForjaBE;
import org.jspecify.annotations.Nullable;

public class ControladorForjaRenderer implements
        BlockEntityRenderer<ControladorForjaBE, ControladorForjaRenderer.State> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public ControladorForjaRenderer(BlockEntityRendererProvider.Context context) {
        blockModelResolver = context.blockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ControladorForjaBE controller, State state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(controller, state, partialTicks, cameraPosition, breakProgress);
        for (BlockModelRenderState model : state.models) model.clear();

        if (!controller.isShowingPreview() || controller.getLevel() == null
                || controller.getBlockState().getValue(ControladorForja.FORMED)) {
            return;
        }

        BlockPos origin = controller.getBlockPos();
        Direction facing = controller.getBlockState().getValue(ControladorForja.FACING);
        BlockPos[] positions = ControladorForja.positions(origin, facing.getClockWise(), facing.getOpposite());
        Block[] expected = ControladorForja.expectedBlocks();
        for (int i = 0; i < positions.length; i++) {
            if (controller.getLevel().getBlockState(positions[i]).is(expected[i])) continue;
            blockModelResolver.update(state.models[i], expected[i].defaultBlockState(), DISPLAY_CONTEXT);
            state.offsets[i] = positions[i].subtract(origin);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.models.length; i++) {
            if (state.models[i].isEmpty()) continue;
            BlockPos offset = state.offsets[i];
            poseStack.pushPose();
            poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
            state.models[i].submit(
                    poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public static class State extends BlockEntityRenderState {
        private final BlockModelRenderState[] models = {
                new BlockModelRenderState(), new BlockModelRenderState(), new BlockModelRenderState(),
                new BlockModelRenderState(), new BlockModelRenderState(), new BlockModelRenderState(),
                new BlockModelRenderState()
        };
        private final BlockPos[] offsets = {
                BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO,
                BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO
        };
    }
}
