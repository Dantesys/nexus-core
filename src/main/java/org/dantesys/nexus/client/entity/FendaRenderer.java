package org.dantesys.nexus.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.entity.FendaDimensional;

/** Desenha as fendas da Rifty como planos com a textura, sempre com brilho total. */
public class FendaRenderer extends EntityRenderer<FendaDimensional, FendaRenderer.State> {
    private static final Identifier LINEAR = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/fenda_dimensional.png");
    private static final Identifier RADIAL = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/fenda_radial.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public FendaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(FendaDimensional entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.kind = entity.kind();
        state.length = entity.length();
        state.progress = Mth.clamp((entity.tickCount + partialTicks) / entity.life(), 0.0F, 1.0F);
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();
    }

    @Override
    protected boolean affectedByCulling(FendaDimensional entity) {
        return entity.kind() == FendaDimensional.RADIAL;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float p = state.progress;
        int alpha = (int) (255 * (1.0F - p * p));
        int color = ARGB.color(alpha, 255, 255, 255);
        poseStack.pushPose();
        if (state.kind == FendaDimensional.RADIAL) {
            float half = state.length * (0.6F + 0.6F * p) / 2.0F;
            poseStack.rotate(camera.orientation);
            poseStack.rotateDegrees(Axis.ZP, state.ageInTicks * 4.0F);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(RADIAL),
                    (pose, buffer) -> quad(pose, buffer, -half, -half, half, half, 0, color, false));
        } else {
            // A rachadura abre rapido e vai se fechando.
            float open = p < 0.15F ? p / 0.15F : 1.0F - (p - 0.15F) / 0.85F;
            float half = 0.3F * open;
            float length = state.length;
            poseStack.rotateDegrees(Axis.YP, -state.yRot);
            poseStack.rotateDegrees(Axis.XP, state.xRot);
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(LINEAR), (pose, buffer) -> {
                quadAlongZ(pose, buffer, half, length, color, true);   // plano vertical
                quadAlongZ(pose, buffer, half, length, color, false);  // plano horizontal
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float x1, float y1, float z, int color, boolean unused) {
        vertex(pose, buffer, x0, y0, z, 0, 1, color);
        vertex(pose, buffer, x1, y0, z, 1, 1, color);
        vertex(pose, buffer, x1, y1, z, 1, 0, color);
        vertex(pose, buffer, x0, y1, z, 0, 0, color);
    }

    /** Faixa de 0 a length no eixo Z, com largura 2*half no eixo Y (vertical) ou X (horizontal). */
    private static void quadAlongZ(PoseStack.Pose pose, VertexConsumer buffer, float half, float length, int color, boolean vertical) {
        float ax = vertical ? 0 : half, ay = vertical ? half : 0;
        vertex(pose, buffer, -ax, -ay, 0, 0, 1, color);
        vertex(pose, buffer, -ax, -ay, length, 1, 1, color);
        vertex(pose, buffer, ax, ay, length, 1, 0, color);
        vertex(pose, buffer, ax, ay, 0, 0, 0, color);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int color) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static class State extends EntityRenderState {
        public int kind;
        public float length = 1.0F;
        public float progress;
        public float yRot, xRot;
    }
}
