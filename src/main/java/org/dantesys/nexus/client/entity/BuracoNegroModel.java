package org.dantesys.nexus.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.dantesys.nexus.Nexus;

/** Modelo do buraco negro (blockbench/habilidades/buraco_negro.bbmodel), com a animacao "girar". */
public class BuracoNegroModel extends EntityModel<BuracoNegroRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Nexus.MODID, "buraco_negro"), "main");
    private final ModelPart nucleo, disco;

    public BuracoNegroModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucentEmissive);
        ModelPart buraco = root.getChild("buraco_negro");
        this.nucleo = buraco.getChild("nucleo");
        this.disco = buraco.getChild("disco");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition buraco = root.addOrReplaceChild("buraco_negro", CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, 0.0F));
        buraco.addOrReplaceChild("nucleo", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(24, 0).addBox(-4.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                .texOffs(0, 12).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 8.0F)
                .texOffs(24, 12).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(0, 24).addBox(-4.5F, -4.5F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.ZERO);
        PartDefinition disco = buraco.addOrReplaceChild("disco", CubeListBuilder.create()
                .texOffs(36, 24).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
        int[][] uv = {{32, 46}, {16, 46}, {0, 46}, {48, 42}, {32, 42}, {16, 42}, {0, 42}};
        float[] yaw = {-5.4978F, -4.7124F, -3.927F, -3.1416F, -2.3562F, -1.5708F, -0.7854F};
        for (int i = 0; i < yaw.length; i++) {
            disco.addOrReplaceChild("disco_" + (8 - i) + "_r1", CubeListBuilder.create()
                    .texOffs(uv[i][0], uv[i][1]).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, yaw[i], 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BuracoNegroRenderer.State state) {
        super.setupAnim(state);
        float t = (state.ageInTicks / 20.0F) % 2.0F;
        disco.yRot -= (t / 2.0F) * Mth.TWO_PI;                      // 0 -> 360 graus em 2s
        float pulse = 1.0F + 0.08F * (t <= 1.0F ? t : 2.0F - t);   // 1 -> 1.08 -> 1
        nucleo.xScale = pulse;
        nucleo.yScale = pulse;
        nucleo.zScale = pulse;
    }
}
