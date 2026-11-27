package org.dantesys.nexus.client;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.relic.SteelyEffects;

/** Formas visuais do jogador: Steely "Eu Sou o Iron Golem" desenha o jogador como golem de meio tamanho. */
@EventBusSubscriber(modid = Nexus.MODID, value = Dist.CLIENT)
public final class PlayerForms {
    private PlayerForms() {}

    private static final Identifier GOLEM_TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/steely_golem.png");
    private static EntityModelSet bakedFrom;
    private static IronGolemModel golemModel;
    private static final IronGolemRenderState GOLEM_STATE = new IronGolemRenderState();

    private static IronGolemModel golem() {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (golemModel == null || bakedFrom != models) {
            golemModel = new IronGolemModel(models.bakeLayer(ModelLayers.IRON_GOLEM));
            bakedFrom = models;
        }
        return golemModel;
    }

    @SubscribeEvent
    public static void render(RenderPlayerEvent.Pre<?> event) {
        AvatarRenderState state = event.getRenderState();
        Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(state.id);
        if (entity == null || !SteelyEffects.GOLEM_FORM.equals(entity.getData(ModAttachmentType.FORMA))) return;
        event.setCanceled(true);
        if (state.isInvisible) return;

        IronGolemRenderState g = GOLEM_STATE;
        g.walkAnimationPos = state.walkAnimationPos;
        g.walkAnimationSpeed = state.walkAnimationSpeed;
        g.yRot = state.yRot;
        g.xRot = state.xRot;
        g.bodyRot = state.bodyRot;
        g.ageInTicks = state.ageInTicks;
        g.lightCoords = state.lightCoords;
        g.attackTicksRemaining = state.swingAnimation > 0 ? 10.0F * (1.0F - state.swingAnimation) : 0.0F;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float scale = state.scale * (2.0F / 3.0F); // jogador a 75% -> golem a 50%
        pose.scale(scale, scale, scale);
        pose.rotateDegrees(Axis.YP, 180.0F - state.bodyRot);
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        event.getSubmitNodeCollector().submitModel(golem(), g, pose, GOLEM_TEXTURE, state.lightCoords, overlay, state.outlineColor);
        pose.popPose();
    }
}
