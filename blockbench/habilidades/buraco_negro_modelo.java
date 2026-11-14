// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class buraco_negro<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "buraco_negro"), "main");
	private final ModelPart buraco_negro;
	private final ModelPart nucleo;
	private final ModelPart disco;

	public buraco_negro(ModelPart root) {
		this.buraco_negro = root.getChild("buraco_negro");
		this.nucleo = this.buraco_negro.getChild("nucleo");
		this.disco = this.buraco_negro.getChild("disco");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition buraco_negro = partdefinition.addOrReplaceChild("buraco_negro", CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, 0.0F));

		PartDefinition nucleo = buraco_negro.addOrReplaceChild("nucleo", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(24, 0).addBox(-4.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 12).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(24, 12).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 24).addBox(-4.5F, -4.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition disco = buraco_negro.addOrReplaceChild("disco", CubeListBuilder.create().texOffs(36, 24).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3491F, 0.0F, 0.0F));

		PartDefinition disco_8_r1 = disco.addOrReplaceChild("disco_8_r1", CubeListBuilder.create().texOffs(32, 46).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.4978F, 0.0F));

		PartDefinition disco_7_r1 = disco.addOrReplaceChild("disco_7_r1", CubeListBuilder.create().texOffs(16, 46).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));

		PartDefinition disco_6_r1 = disco.addOrReplaceChild("disco_6_r1", CubeListBuilder.create().texOffs(0, 46).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.927F, 0.0F));

		PartDefinition disco_5_r1 = disco.addOrReplaceChild("disco_5_r1", CubeListBuilder.create().texOffs(48, 42).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));

		PartDefinition disco_4_r1 = disco.addOrReplaceChild("disco_4_r1", CubeListBuilder.create().texOffs(32, 42).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));

		PartDefinition disco_3_r1 = disco.addOrReplaceChild("disco_3_r1", CubeListBuilder.create().texOffs(16, 42).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition disco_2_r1 = disco.addOrReplaceChild("disco_2_r1", CubeListBuilder.create().texOffs(0, 42).addBox(-2.6F, -0.2F, 5.2F, 5.2F, 0.4F, 2.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		buraco_negro.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}