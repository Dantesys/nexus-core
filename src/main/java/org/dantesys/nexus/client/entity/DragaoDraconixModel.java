package org.dantesys.nexus.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.dantesys.nexus.Nexus;

/** Modelo do dragao da Draconix (blockbench/habilidades/dragao_draconix.bbmodel). */
public class DragaoDraconixModel extends EntityModel<DragaoDraconixRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Nexus.MODID, "dragao_draconix"), "main");
    private final ModelPart pescoco, cabeca, mandibula, cauda, cauda2, cauda3, pernaFe, pernaFd, pernaTe, pernaTd, asaEsq, asaDir;

    public DragaoDraconixModel(ModelPart root) {
        super(root);
        ModelPart corpo = root.getChild("corpo");
        this.pescoco = corpo.getChild("pescoco");
        this.cabeca = pescoco.getChild("cabeca");
        this.mandibula = cabeca.getChild("mandibula");
        this.cauda = corpo.getChild("cauda");
        this.cauda2 = cauda.getChild("cauda2");
        this.cauda3 = cauda2.getChild("cauda3");
        this.pernaFe = corpo.getChild("perna_fe");
        this.pernaFd = corpo.getChild("perna_fd");
        this.pernaTe = corpo.getChild("perna_te");
        this.pernaTd = corpo.getChild("perna_td");
        this.asaEsq = corpo.getChild("asa_esq");
        this.asaDir = corpo.getChild("asa_dir");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition corpo = root.addOrReplaceChild("corpo", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -6.0F, -10.0F, 12.0F, 10.0F, 20.0F)
                .texOffs(64, 0).addBox(-5.0F, 3.5F, -9.0F, 10.0F, 1.0F, 18.0F)
                .texOffs(120, 0).addBox(-0.6F, -8.0F, -7.0F, 1.2F, 2.0F, 1.6F)
                .texOffs(126, 0).addBox(-0.6F, -8.0F, -2.0F, 1.2F, 2.0F, 1.6F)
                .texOffs(132, 0).addBox(-0.6F, -8.0F, 3.0F, 1.2F, 2.0F, 1.6F)
                .texOffs(138, 0).addBox(-0.6F, -8.0F, 8.0F, 1.2F, 2.0F, 1.6F), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition pescoco = corpo.addOrReplaceChild("pescoco", CubeListBuilder.create()
                .texOffs(144, 0).addBox(-3.0F, -3.0F, -9.0F, 6.0F, 6.0F, 9.0F)
                .texOffs(174, 0).addBox(-0.5F, -4.6F, -6.0F, 1.0F, 1.6F, 1.4F), PartPose.offset(0.0F, -4.0F, -9.0F));
        PartDefinition cabeca = pescoco.addOrReplaceChild("cabeca", CubeListBuilder.create()
                .texOffs(179, 0).addBox(-4.0F, -4.0F, -9.0F, 8.0F, 6.5F, 9.0F)
                .texOffs(213, 0).addBox(-3.0F, -2.0F, -15.0F, 6.0F, 3.5F, 6.0F)
                .texOffs(237, 0).addBox(1.2F, -2.2F, -14.6F, 1.0F, 0.6F, 1.0F)
                .texOffs(241, 0).addBox(-2.2F, -2.2F, -14.6F, 1.0F, 0.6F, 1.0F)
                .texOffs(245, 0).addBox(3.9F, -2.7F, -7.0F, 0.4F, 1.2F, 2.0F)
                .texOffs(250, 0).addBox(-4.3F, -2.7F, -7.0F, 0.4F, 1.2F, 2.0F), PartPose.offset(0.0F, -2.0F, -8.0F));
        cabeca.addOrReplaceChild("chifre_dir_r1", CubeListBuilder.create()
                .texOffs(7, 30).addBox(-0.8F, -5.4F, -0.8F, 1.6F, 5.6F, 1.6F)
                .texOffs(0, 30).addBox(4.8F, -5.4F, -0.8F, 1.6F, 5.6F, 1.6F), PartPose.offsetAndRotation(-2.8F, -3.6F, -1.8F, -0.6109F, 0.0F, 0.0F));
        cabeca.addOrReplaceChild("mandibula", CubeListBuilder.create()
                .texOffs(14, 30).addBox(-2.8F, -1.0F, -6.5F, 5.6F, 2.5F, 6.5F)
                .texOffs(39, 30).addBox(1.6F, -2.0F, -6.0F, 0.8F, 1.0F, 0.8F)
                .texOffs(43, 30).addBox(-2.4F, -2.0F, -6.0F, 0.8F, 1.0F, 0.8F), PartPose.offset(0.0F, 2.5F, -8.0F));
        PartDefinition cauda = corpo.addOrReplaceChild("cauda", CubeListBuilder.create()
                .texOffs(47, 30).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 5.0F, 10.0F), PartPose.offset(0.0F, -1.0F, 10.0F));
        PartDefinition cauda2 = cauda.addOrReplaceChild("cauda2", CubeListBuilder.create()
                .texOffs(79, 30).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.5F, 10.0F), PartPose.offset(0.0F, 1.0F, 10.0F));
        cauda2.addOrReplaceChild("cauda3", CubeListBuilder.create()
                .texOffs(107, 30).addBox(-1.2F, -1.4F, 0.0F, 2.4F, 2.2F, 8.0F)
                .texOffs(128, 30).addBox(-2.4F, -0.8F, 7.0F, 4.8F, 1.2F, 4.0F), PartPose.offset(0.0F, 1.0F, 10.0F));
        corpo.addOrReplaceChild("perna_fe", CubeListBuilder.create()
                .texOffs(146, 30).addBox(-1.5F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F)
                .texOffs(162, 30).addBox(-1.8F, 9.0F, -3.5F, 4.6F, 2.0F, 5.5F)
                .texOffs(183, 30).addBox(1.5F, 10.2F, -4.5F, 0.7F, 0.8F, 1.0F)
                .texOffs(187, 30).addBox(0.1923F, 10.2F, -4.5F, 0.7F, 0.8F, 1.0F)
                .texOffs(191, 30).addBox(-1.1154F, 10.2F, -4.5F, 0.7F, 0.8F, 1.0F), PartPose.offset(5.0F, 3.0F, -6.0F));
        corpo.addOrReplaceChild("perna_fd", CubeListBuilder.create()
                .texOffs(195, 30).addBox(-2.5F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F)
                .texOffs(211, 30).addBox(-2.8F, 9.0F, -3.5F, 4.6F, 2.0F, 5.5F)
                .texOffs(232, 30).addBox(0.5F, 10.2F, -4.5F, 0.7F, 0.8F, 1.0F)
                .texOffs(236, 30).addBox(-0.8077F, 10.2F, -4.5F, 0.7F, 0.8F, 1.0F)
                .texOffs(240, 30).addBox(-2.1154F, 10.2F, -4.5F, 0.7F, 0.8F, 1.0F), PartPose.offset(-5.0F, 3.0F, -6.0F));
        corpo.addOrReplaceChild("perna_te", CubeListBuilder.create()
                .texOffs(0, 45).addBox(-2.0F, -1.0F, -2.5F, 5.0F, 10.0F, 5.0F)
                .texOffs(20, 45).addBox(-2.3F, 9.0F, -4.0F, 5.6F, 2.0F, 6.5F)
                .texOffs(45, 45).addBox(2.0F, 10.2F, -5.0F, 0.7F, 0.8F, 1.0F)
                .texOffs(49, 45).addBox(0.3077F, 10.2F, -5.0F, 0.7F, 0.8F, 1.0F)
                .texOffs(53, 45).addBox(-1.3846F, 10.2F, -5.0F, 0.7F, 0.8F, 1.0F), PartPose.offset(5.0F, 3.0F, 6.0F));
        corpo.addOrReplaceChild("perna_td", CubeListBuilder.create()
                .texOffs(57, 45).addBox(-3.0F, -1.0F, -2.5F, 5.0F, 10.0F, 5.0F)
                .texOffs(77, 45).addBox(-3.3F, 9.0F, -4.0F, 5.6F, 2.0F, 6.5F)
                .texOffs(102, 45).addBox(1.0F, 10.2F, -5.0F, 0.7F, 0.8F, 1.0F)
                .texOffs(106, 45).addBox(-0.6923F, 10.2F, -5.0F, 0.7F, 0.8F, 1.0F)
                .texOffs(110, 45).addBox(-2.3846F, 10.2F, -5.0F, 0.7F, 0.8F, 1.0F), PartPose.offset(-5.0F, 3.0F, 6.0F));
        corpo.addOrReplaceChild("asa_esq", CubeListBuilder.create()
                .texOffs(114, 45).addBox(0.0F, -1.0F, -1.5F, 18.0F, 2.0F, 2.0F)
                .texOffs(154, 45).addBox(0.0F, -0.4F, 0.5F, 18.0F, 0.8F, 11.5F)
                .texOffs(213, 45).addBox(16.0F, -0.8F, 0.5F, 2.0F, 1.6F, 12.5F), PartPose.offset(6.0F, -6.0F, -3.0F));
        corpo.addOrReplaceChild("asa_dir", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-18.0F, -1.0F, -1.5F, 18.0F, 2.0F, 2.0F)
                .texOffs(40, 60).addBox(-18.0F, -0.4F, 0.5F, 18.0F, 0.8F, 11.5F)
                .texOffs(99, 60).addBox(-18.0F, -0.8F, 0.5F, 2.0F, 1.6F, 12.5F), PartPose.offset(-6.0F, -6.0F, -3.0F));
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(DragaoDraconixRenderer.State state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        float flap = Mth.sin(t * 0.35F) * 0.65F;
        asaEsq.zRot = flap;
        asaDir.zRot = -flap;
        pescoco.xRot = Mth.clamp(state.xRot, -45, 45) * Mth.DEG_TO_RAD * 0.4F;
        pescoco.yRot = Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD * 0.5F;
        cabeca.xRot = Mth.clamp(state.xRot, -45, 45) * Mth.DEG_TO_RAD * 0.4F;
        mandibula.xRot = state.mouthOpen * 0.7F;
        float sway = Mth.sin(t * 0.12F) * 0.12F;
        cauda.yRot = sway;
        cauda2.yRot = sway * 1.3F;
        cauda3.yRot = sway * 1.6F;
        cauda.xRot = 0.1F + Mth.sin(t * 0.35F + 1.0F) * 0.05F;
        // voando: pernas recolhidas para tras, com balanco leve
        float tuck = 0.7F, swing = Mth.sin(t * 0.2F) * 0.08F;
        pernaFe.xRot = tuck + swing;
        pernaFd.xRot = tuck - swing;
        pernaTe.xRot = tuck * 1.2F - swing;
        pernaTd.xRot = tuck * 1.2F + swing;
    }
}
