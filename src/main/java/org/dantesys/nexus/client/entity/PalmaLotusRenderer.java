package org.dantesys.nexus.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.entity.PalmaLotus;

/** Palma desce do alto (4 ticks), fica no chao e afunda no fim. */
public class PalmaLotusRenderer extends EntityRenderer<PalmaLotus, PalmaLotusRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/palma_lotus.png");
    private final PalmaLotusModel model;

    public PalmaLotusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PalmaLotusModel(context.bakeLayer(PalmaLotusModel.LAYER));
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(PalmaLotus entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.time = entity.tickCount + partialTicks;
        state.yRot = entity.getYRot();
        state.size = entity.size();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = state.time, lift;
        if (t < PalmaLotus.SLAM_TICK) lift = 6.0F * (1.0F - t / PalmaLotus.SLAM_TICK) * (1.0F - t / PalmaLotus.SLAM_TICK);
        else if (t < PalmaLotus.LIFE - 4) lift = 0;
        else lift = -(t - (PalmaLotus.LIFE - 4)) * 0.08F * state.size;
        poseStack.pushPose();
        poseStack.translate(0.0F, lift, 0.0F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        poseStack.scale(-state.size, -state.size, state.size);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override protected boolean affectedByCulling(PalmaLotus entity) { return false; }

    public static class State extends EntityRenderState {
        public float time, yRot, size = 4.0F;
    }
}
