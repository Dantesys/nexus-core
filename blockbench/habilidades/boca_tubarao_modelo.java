// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class boca_tubarao<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "boca_tubarao"), "main");
	private final ModelPart boca;
	private final ModelPart mandibula_tras;
	private final ModelPart mandibula_frente;

	public boca_tubarao(ModelPart root) {
		this.boca = root.getChild("boca");
		this.mandibula_tras = this.boca.getChild("mandibula_tras");
		this.mandibula_frente = this.boca.getChild("mandibula_frente");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition boca = partdefinition.addOrReplaceChild("boca", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition mandibula_tras = boca.addOrReplaceChild("mandibula_tras", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -18.0F, -5.0F, 24.0F, 18.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(56, 0).addBox(-10.0F, -22.0F, -5.0F, 20.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 22).addBox(-6.0F, -24.0F, -4.0F, 12.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 22).addBox(-11.0F, -16.0F, -1.0F, 22.0F, 15.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(78, 22).addBox(9.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(85, 22).addBox(5.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(92, 22).addBox(1.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(99, 22).addBox(-3.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(106, 22).addBox(-7.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(113, 22).addBox(-11.0F, -17.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(120, 22).addBox(12.0F, -15.0F, -4.0F, 0.5F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 38).addBox(-12.5F, -15.0F, -4.0F, 0.5F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -1.0F));

		PartDefinition barbatana_r1 = mandibula_tras.addOrReplaceChild("barbatana_r1", CubeListBuilder.create().texOffs(5, 38).addBox(-1.0F, -5.0F, -3.0F, 2.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -18.0F, -5.0F, 0.3927F, 0.0F, 0.0F));

		PartDefinition mandibula_frente = boca.addOrReplaceChild("mandibula_frente", CubeListBuilder.create().texOffs(17, 38).addBox(-12.0F, -16.0F, 1.0F, 24.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(73, 38).addBox(-9.0F, -18.0F, 1.0F, 18.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 58).addBox(-11.0F, -14.0F, 0.0F, 22.0F, 13.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(46, 58).addBox(7.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(53, 58).addBox(3.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(60, 58).addBox(-1.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(67, 58).addBox(-5.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(74, 58).addBox(-9.0F, -15.0F, -1.0F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 1.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		boca.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}