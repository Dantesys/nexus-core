package org.dantesys.nexus.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.entity.DragaoDraconix;

public class DragaoDraconixRenderer extends MobRenderer<DragaoDraconix, DragaoDraconixRenderer.State, DragaoDraconixModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/dragao_draconix.png");
    public static final float SCALE = 1.0F;

    public DragaoDraconixRenderer(EntityRendererProvider.Context context) {
        super(context, new DragaoDraconixModel(context.bakeLayer(DragaoDraconixModel.LAYER)), 0.9F);
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(DragaoDraconix entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int breath = entity.breathTicks();
        state.mouthOpen = breath > 0 ? Mth.clamp(breath / 6.0F, 0.0F, 1.0F) : 0.0F;
    }

    @Override protected void scale(State state, PoseStack poseStack) { poseStack.scale(SCALE, SCALE, SCALE); }

    @Override public Identifier getTextureLocation(State state) { return TEXTURE; }

    public static class State extends LivingEntityRenderState {
        public float mouthOpen;
    }
}
