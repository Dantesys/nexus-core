package org.dantesys.nexus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.blocks.blockentity.ColetorArcanoBasicoBE;
import org.dantesys.nexus.items.ModItems;
import org.jspecify.annotations.Nullable;

public class ColetorArcanoBasicoRenderer implements
        BlockEntityRenderer<ColetorArcanoBasicoBE, ColetorArcanoBasicoRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ColetorArcanoBasicoRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ColetorArcanoBasicoBE collector, State state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(collector, state, partialTicks, cameraPosition, breakProgress);
        state.angle = collector.getRenderAngle(partialTicks);
        itemModelResolver.updateForTopItem(
                state.core, new ItemStack(ModItems.NUCLEO.get()), ItemDisplayContext.FIXED,
                collector.getLevel(), null, (int) collector.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.02F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, state.angle);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        state.core.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        private final ItemStackRenderState core = new ItemStackRenderState();
        private float angle;
    }
}
