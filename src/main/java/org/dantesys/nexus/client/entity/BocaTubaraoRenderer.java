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
import org.dantesys.nexus.entity.BocaTubarao;

public class BocaTubaraoRenderer extends EntityRenderer<BocaTubarao, BocaTubaraoRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/boca_tubarao.png");
    private static final float SCALE = 1.5F;
    private final BocaTubaraoModel model;

    public BocaTubaraoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BocaTubaraoModel(context.bakeLayer(BocaTubaraoModel.LAYER));
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(BocaTubarao entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.time = entity.animationTime(partialTicks);
        state.yRot = entity.getYRot();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        poseStack.scale(-SCALE, -SCALE, SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float time;
        public float yRot;
    }
}
