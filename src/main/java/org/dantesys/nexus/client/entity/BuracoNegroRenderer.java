package org.dantesys.nexus.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.entity.BuracoNegro;

public class BuracoNegroRenderer extends EntityRenderer<BuracoNegro, BuracoNegroRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/buraco_negro.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private final BuracoNegroModel model;

    public BuracoNegroRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BuracoNegroModel(context.bakeLayer(BuracoNegroModel.LAYER));
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(BuracoNegro entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.size = entity.size();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(-state.size, -state.size, state.size);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class State extends EntityRenderState {
        public float size = 1.5F;
    }
}
