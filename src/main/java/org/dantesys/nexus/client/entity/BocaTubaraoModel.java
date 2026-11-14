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

/** Modelo da boca de tubarao (blockbench/habilidades/boca_tubarao.bbmodel), com a animacao "morder". */
public class BocaTubaraoModel extends EntityModel<BocaTubaraoRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Nexus.MODID, "boca_tubarao"), "main");
    private final ModelPart boca, tras, frente;

    public BocaTubaraoModel(ModelPart root) {
        super(root);
        this.boca = root.getChild("boca");
        this.tras = boca.getChild("mandibula_tras");
        this.frente = boca.getChild("mandibula_frente");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition boca = root.addOrReplaceChild("boca", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition tras = boca.addOrReplaceChild("mandibula_tras", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12.0F, -18.0F, -5.0F, 24.0F, 18.0F, 4.0F)
                .texOffs(56, 0).addBox(-10.0F, -22.0F, -5.0F, 20.0F, 4.0F, 5.0F)
                .texOffs(0, 22).addBox(-6.0F, -24.0F, -4.0F, 12.0F, 2.0F, 4.0F)
                .texOffs(32, 22).addBox(-11.0F, -16.0F, -1.0F, 22.0F, 15.0F, 1.0F)
                .texOffs(78, 22).addBox(9.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F)
                .texOffs(85, 22).addBox(5.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F)
                .texOffs(92, 22).addBox(1.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F)
                .texOffs(99, 22).addBox(-3.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F)
                .texOffs(106, 22).addBox(-7.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F)
                .texOffs(113, 22).addBox(-11.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F)
                .texOffs(120, 22).addBox(12.0F, -15.0F, -4.0F, 0.5F, 2.0F, 2.0F)
                .texOffs(0, 38).addBox(-12.5F, -15.0F, -4.0F, 0.5F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -1.0F));
        tras.addOrReplaceChild("barbatana_r1", CubeListBuilder.create().texOffs(5, 38).addBox(-1.0F, -5.0F, -3.0F, 2.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -18.0F, -5.0F, 0.3927F, 0.0F, 0.0F));
        boca.addOrReplaceChild("mandibula_frente", CubeListBuilder.create()
                .texOffs(17, 38).addBox(-12.0F, -16.0F, 1.0F, 24.0F, 16.0F, 4.0F)
                .texOffs(73, 38).addBox(-9.0F, -18.0F, 1.0F, 18.0F, 2.0F, 3.0F)
                .texOffs(0, 58).addBox(-11.0F, -14.0F, 0.0F, 22.0F, 13.0F, 1.0F)
                .texOffs(46, 58).addBox(7.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F)
                .texOffs(53, 58).addBox(3.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F)
                .texOffs(60, 58).addBox(-1.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F)
                .texOffs(67, 58).addBox(-5.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F)
                .texOffs(74, 58).addBox(-9.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F), PartPose.offset(0.0F, 0.0F, 1.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Interpolacao linear entre keyframes [tempo, valor]. */
    static float keyframes(float t, float[][] frames) {
        if (t <= frames[0][0]) return frames[0][1];
        for (int i = 1; i < frames.length; i++) {
            if (t <= frames[i][0]) {
                float span = frames[i][0] - frames[i - 1][0];
                float f = span <= 0 ? 1 : (t - frames[i - 1][0]) / span;
                return Mth.lerp(f, frames[i - 1][1], frames[i][1]);
            }
        }
        return frames[frames.length - 1][1];
    }

    private static final float[][] RISE = {{0F, -24F}, {0.3F, 0F}, {0.9F, 0F}, {1.2F, -24F}};
    private static final float[][] JAW = {{0F, 40F}, {0.5F, 40F}, {0.62F, 0F}, {1.2F, 0F}};

    @Override
    public void setupAnim(BocaTubaraoRenderer.State state) {
        super.setupAnim(state);
        float t = state.time;
        boca.y -= keyframes(t, RISE);                                   // posicao do Blockbench: y para cima
        tras.xRot += keyframes(t, JAW) * Mth.DEG_TO_RAD;                // rotacao x do Blockbench e invertida no Java
        frente.xRot -= keyframes(t, JAW) * Mth.DEG_TO_RAD;
    }
}
