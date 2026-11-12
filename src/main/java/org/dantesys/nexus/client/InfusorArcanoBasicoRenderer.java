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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.blocks.blockentity.InfusorArcanoBasicoBE;
import org.jspecify.annotations.Nullable;

public class InfusorArcanoBasicoRenderer implements
        BlockEntityRenderer<InfusorArcanoBasicoBE, InfusorArcanoBasicoRenderer.State> {
    private static final float[][] PILLARS = {
            {0.15625F, 0.15625F},
            {0.84375F, 0.15625F},
            {0.15625F, 0.84375F},
            {0.84375F, 0.84375F}
    };
    private static final float PILLAR_ITEM_Y = 1.22F;
    private static final float CENTER_ITEM_Y = 1.10F;

    private final ItemModelResolver itemModelResolver;

    public InfusorArcanoBasicoRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InfusorArcanoBasicoBE infusor, State state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(infusor, state, partialTicks, cameraPosition, breakProgress);
        state.progress = infusor.getManaCost() > 0
                ? Mth.clamp(infusor.getProgress() / (float) infusor.getManaCost(), 0.0F, 1.0F)
                : 0.0F;
        state.age = infusor.getLevel() == null ? 0.0F : infusor.getLevel().getGameTime() + partialTicks;

        for (int slot = 0; slot < state.items.length; slot++) {
            itemModelResolver.updateForTopItem(
                    state.items[slot], infusor.getItem(slot), ItemDisplayContext.FIXED,
                    infusor.getLevel(), null, (int) (infusor.getBlockPos().asLong() + slot));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        float easedProgress = (float) Mth.smoothstep(state.progress);
        for (int slot = 0; slot < InfusorArcanoBasicoBE.INPUT_SLOT_COUNT; slot++) {
            if (state.items[slot].isEmpty()) {
                continue;
            }

            float x = Mth.lerp(easedProgress, PILLARS[slot][0], 0.5F);
            float z = Mth.lerp(easedProgress, PILLARS[slot][1], 0.5F);
            float bob = Mth.sin(state.age * 0.12F + slot * 1.57F) * 0.045F;
            float y = Mth.lerp(easedProgress, PILLAR_ITEM_Y, CENTER_ITEM_Y) + bob;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 2.5F + slot * 90.0F));
            poseStack.scale(0.35F, 0.35F, 0.35F);
            state.items[slot].submit(
                    poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        ItemStackRenderState result = state.items[InfusorArcanoBasicoBE.RESULT_SLOT];
        if (!result.isEmpty()) {
            float bob = Mth.sin(state.age * 0.12F) * 0.045F;
            poseStack.pushPose();
            poseStack.translate(0.5F, CENTER_ITEM_Y + bob, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 2.5F));
            poseStack.scale(0.42F, 0.42F, 0.42F);
            result.submit(
                    poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public static class State extends BlockEntityRenderState {
        private final ItemStackRenderState[] items = {
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState(),
                new ItemStackRenderState()
        };
        private float progress;
        private float age;
    }
}
