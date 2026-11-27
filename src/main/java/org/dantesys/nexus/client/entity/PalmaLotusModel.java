package org.dantesys.nexus.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;

/** Modelo da palma dourada (blockbench/habilidades/palma_lotus.bbmodel). */
public class PalmaLotusModel extends EntityModel<EntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Nexus.MODID, "palma_lotus"), "main");

    public PalmaLotusModel(ModelPart root) { super(root); }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition palma = root.addOrReplaceChild("palma", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.0F, -4.0F, -12.0F, 24.0F, 4.0F, 24.0F)
                .texOffs(96, 0).addBox(-8.0F, -3.5F, 12.0F, 16.0F, 3.5F, 10.0F)
                .texOffs(53, 28).addBox(-3.0F, -5.0F, -3.0F, 6.0F, 1.0F, 6.0F)
                .texOffs(77, 28).addBox(-1.5F, -4.8F, 2.5F, 3.0F, 0.6F, 5.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        float[] petals = {-5.236F, -4.1888F, -3.1416F, -2.0944F, -1.0472F};
        int[] u = {157, 141, 125, 109, 93};
        for (int i = 0; i < petals.length; i++) {
            palma.addOrReplaceChild("petala_" + (6 - i) + "_r1", CubeListBuilder.create().texOffs(u[i], 28).addBox(-1.5F, -0.3F, 2.5F, 3.0F, 0.6F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.0F, petals[i], 0.0F));
        }
        palma.addOrReplaceChild("polegar_r1", CubeListBuilder.create().texOffs(27, 28).addBox(-8.0F, -1.5F, -3.0F, 8.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(-12.0F, -2.0F, -3.0F, 0.0F, -0.3927F, 0.0F));
        palma.addOrReplaceChild("dedos", CubeListBuilder.create()
                .texOffs(148, 0).addBox(6.5F, -1.6F, -10.0F, 4.5F, 3.2F, 10.0F)
                .texOffs(177, 0).addBox(0.5F, -1.6F, -13.0F, 4.5F, 3.2F, 13.0F)
                .texOffs(212, 0).addBox(-5.5F, -1.6F, -12.0F, 4.5F, 3.2F, 12.0F)
                .texOffs(0, 28).addBox(-11.5F, -1.6F, -9.0F, 4.5F, 3.2F, 9.0F), PartPose.offset(0.0F, -2.0F, -12.0F));
        return LayerDefinition.create(mesh, 256, 128);
    }
}
