package org.dantesys.nexus.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.items.ModItems;

import java.util.Optional;

public class ModModelProvider extends ModelProvider {
    private static final TextureSlot MOLDE_TEXTURE = TextureSlot.create("0");

    public static final ModelTemplate ESMERALDA_TEMPLATE = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":esmeralda_elemental")
            ),
            Optional.empty(),
            TextureSlot.LAYER0
    );
    public static final ModelTemplate FRAGMENTO_TEMPLATE = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":fragmento_elemental")
            ),
            Optional.empty(),
            TextureSlot.LAYER0
    );
    public static final ModelTemplate MOLDE_COMUM = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_comum")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );
    public static final ModelTemplate MOLDE_INCOMUM = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_incomum")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );
    public static final ModelTemplate MOLDE_RARO = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_raro")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );
    public static final ModelTemplate MOLDE_EPICO = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_epico")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );
    public static final ModelTemplate MOLDE_MITICO = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_mitico")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );
    public static final ModelTemplate MOLDE_LENDARIO = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_lendario")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );
    public static final ModelTemplate MOLDE_UNICO = new ModelTemplate(
            Optional.of(
                    ModelLocationUtils.decorateItemModelLocation(Nexus.MODID+":molde_unico")
            ),
            Optional.empty(),
            MOLDE_TEXTURE,
            TextureSlot.PARTICLE
    );

    public ModModelProvider(PackOutput output) {
        super(output, Nexus.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ModItems.STEEL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.declareCustomModelItem(ModItems.NUCLEO.get());
        itemModels.declareCustomModelItem(ModItems.NUCLEO_FORJA.get());
        generateMatriz(itemModels, ModItems.MATRIZ_PRIMORDIAL.get(), "molde_base");
        generateMatriz(itemModels, ModItems.MATRIZ_SINTETICA.get(), "molde_base_sintetico");
        itemModels.declareCustomModelItem(ModItems.ARCANO_EVOLUIDOR_INCOMUM.get());
        itemModels.declareCustomModelItem(ModItems.ARCANO_EVOLUIDOR_RARO.get());
        itemModels.declareCustomModelItem(ModItems.ARCANO_EVOLUIDOR_EPICO.get());
        itemModels.declareCustomModelItem(ModItems.ARCANO_EVOLUIDOR_MITICO.get());
        itemModels.declareCustomModelItem(ModItems.ARCANO_EVOLUIDOR_LENDARIO.get());
        itemModels.declareCustomModelItem(ModItems.ARCANO_EVOLUIDOR_UNICO.get());

        itemModels.generateFlatItem(ModItems.ESMERALDA_IGNEA.get(), ESMERALDA_TEMPLATE);
        itemModels.generateFlatItem(ModItems.ESMERALDA_VIDA.get(), ESMERALDA_TEMPLATE);
        itemModels.generateFlatItem(ModItems.ESMERALDA_SOMBRIA.get(), ESMERALDA_TEMPLATE);
        itemModels.generateFlatItem(ModItems.ESMERALDA_LUZ.get(), ESMERALDA_TEMPLATE);
        itemModels.generateFlatItem(ModItems.ESMERALDA_METAL.get(), ESMERALDA_TEMPLATE);
        itemModels.generateFlatItem(ModItems.ESMERALDA_TEMPESTADE.get(), ESMERALDA_TEMPLATE);
        itemModels.generateFlatItem(ModItems.FRAGMENTO_IGNEA.get(), FRAGMENTO_TEMPLATE);
        itemModels.generateFlatItem(ModItems.FRAGMENTO_VIDA.get(), FRAGMENTO_TEMPLATE);
        itemModels.generateFlatItem(ModItems.FRAGMENTO_SOMBRIA.get(), FRAGMENTO_TEMPLATE);
        itemModels.generateFlatItem(ModItems.FRAGMENTO_LUZ.get(), FRAGMENTO_TEMPLATE);
        itemModels.generateFlatItem(ModItems.FRAGMENTO_METAL.get(), FRAGMENTO_TEMPLATE);
        itemModels.generateFlatItem(ModItems.FRAGMENTO_TEMPESTADE.get(), FRAGMENTO_TEMPLATE);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_S.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_A.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_B.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_C.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_D.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_E.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CRISTAL_MANA_F.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_SWORD.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        itemModels.generateFlatItem(ModItems.STEEL_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_AXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_SHOVEL.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_HOE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateSpear(ModItems.STEEL_SPEAR.get());
        itemModels.generateFlatItem(ModItems.STEEL_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STEEL_BOOTS.get(), ModelTemplates.FLAT_ITEM);
        //BLOCOS
        blockModels.createNonTemplateModelBlock(ModBlocks.BOLA_CRISTAL.get());
        blockModels.createNonTemplateModelBlock(ModBlocks.COLETOR_ARCANO_BASICO.get());
        blockModels.createNonTemplateModelBlock(ModBlocks.INFUSOR_ARCANO_BASICO.get());
        blockModels.createTrivialCube(ModBlocks.STEEL_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.TANQUE_FORJA.get());
        generateForgeController(blockModels);
        blockModels.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                ModBlocks.FORJA_COLLISION.get(),
                BlockModelGenerators.variant(new Variant(
                        Identifier.fromNamespaceAndPath("minecraft", "block/air")))
        ));
        blockModels.woodProvider(ModBlocks.IGNEA_LOG.get()).log(ModBlocks.IGNEA_LOG.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_IGNEA_LOG.get()).log(ModBlocks.STRIPPED_IGNEA_LOG.get());
        blockModels.woodProvider(ModBlocks.LUZ_LOG.get()).log(ModBlocks.LUZ_LOG.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_LUZ_LOG.get()).log(ModBlocks.STRIPPED_LUZ_LOG.get());
        blockModels.woodProvider(ModBlocks.METAL_LOG.get()).log(ModBlocks.METAL_LOG.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_METAL_LOG.get()).log(ModBlocks.STRIPPED_METAL_LOG.get());
        blockModels.woodProvider(ModBlocks.SOMBRIA_LOG.get()).log(ModBlocks.SOMBRIA_LOG.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_SOMBRIA_LOG.get()).log(ModBlocks.STRIPPED_SOMBRIA_LOG.get());
        blockModels.woodProvider(ModBlocks.TEMPESTADE_LOG.get()).log(ModBlocks.TEMPESTADE_LOG.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_TEMPESTADE_LOG.get()).log(ModBlocks.STRIPPED_TEMPESTADE_LOG.get());
        blockModels.woodProvider(ModBlocks.VIDA_LOG.get()).log(ModBlocks.VIDA_LOG.get());
        blockModels.woodProvider(ModBlocks.STRIPPED_VIDA_LOG.get()).log(ModBlocks.STRIPPED_VIDA_LOG.get());
        blockModels.createTrivialCube(ModBlocks.IGNEA_LEAVES.get());
        blockModels.createTrivialCube(ModBlocks.LUZ_LEAVES.get());
        blockModels.createTrivialCube(ModBlocks.METAL_LEAVES.get());
        blockModels.createTrivialCube(ModBlocks.SOMBRIA_LEAVES.get());
        blockModels.createTrivialCube(ModBlocks.TEMPESTADE_LEAVES.get());
        blockModels.createTrivialCube(ModBlocks.VIDA_LEAVES.get());
        blockModels.createCrossBlockWithDefaultItem(ModBlocks.IGNEA_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createCrossBlockWithDefaultItem(ModBlocks.LUZ_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createCrossBlockWithDefaultItem(ModBlocks.METAL_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createCrossBlockWithDefaultItem(ModBlocks.SOMBRIA_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createCrossBlockWithDefaultItem(ModBlocks.TEMPESTADE_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.createCrossBlockWithDefaultItem(ModBlocks.VIDA_SAPLING.get(), BlockModelGenerators.PlantType.NOT_TINTED);
        blockModels.family(ModBlocks.IGNEA_PLANKS.get())
                .stairs(ModBlocks.IGNEA_STAIRS.get())
                .slab(ModBlocks.IGNEA_SLAB.get())
                .pressurePlate(ModBlocks.IGNEA_PRESSURE_PLATE.get())
                .button(ModBlocks.IGNEA_BUTTON.get())
                .fence(ModBlocks.IGNEA_FENCE.get())
                .fenceGate(ModBlocks.IGNEA_FENCE_GATE.get())
                .door(ModBlocks.IGNEA_DOOR.get())
                .trapdoor(ModBlocks.IGNEA_TRAPDOOR.get());

        blockModels.family(ModBlocks.LUZ_PLANKS.get())
                .stairs(ModBlocks.LUZ_STAIRS.get())
                .slab(ModBlocks.LUZ_SLAB.get())
                .pressurePlate(ModBlocks.LUZ_PRESSURE_PLATE.get())
                .button(ModBlocks.LUZ_BUTTON.get())
                .fence(ModBlocks.LUZ_FENCE.get())
                .fenceGate(ModBlocks.LUZ_FENCE_GATE.get())
                .door(ModBlocks.LUZ_DOOR.get())
                .trapdoor(ModBlocks.LUZ_TRAPDOOR.get());

        blockModels.family(ModBlocks.METAL_PLANKS.get())
                .stairs(ModBlocks.METAL_STAIRS.get())
                .slab(ModBlocks.METAL_SLAB.get())
                .pressurePlate(ModBlocks.METAL_PRESSURE_PLATE.get())
                .button(ModBlocks.METAL_BUTTON.get())
                .fence(ModBlocks.METAL_FENCE.get())
                .fenceGate(ModBlocks.METAL_FENCE_GATE.get())
                .door(ModBlocks.METAL_DOOR.get())
                .trapdoor(ModBlocks.METAL_TRAPDOOR.get());

        blockModels.family(ModBlocks.SOMBRIA_PLANKS.get())
                .stairs(ModBlocks.SOMBRIA_STAIRS.get())
                .slab(ModBlocks.SOMBRIA_SLAB.get())
                .pressurePlate(ModBlocks.SOMBRIA_PRESSURE_PLATE.get())
                .button(ModBlocks.SOMBRIA_BUTTON.get())
                .fence(ModBlocks.SOMBRIA_FENCE.get())
                .fenceGate(ModBlocks.SOMBRIA_FENCE_GATE.get())
                .door(ModBlocks.SOMBRIA_DOOR.get())
                .trapdoor(ModBlocks.SOMBRIA_TRAPDOOR.get());

        blockModels.family(ModBlocks.TEMPESTADE_PLANKS.get())
                .stairs(ModBlocks.TEMPESTADE_STAIRS.get())
                .slab(ModBlocks.TEMPESTADE_SLAB.get())
                .pressurePlate(ModBlocks.TEMPESTADE_PRESSURE_PLATE.get())
                .button(ModBlocks.TEMPESTADE_BUTTON.get())
                .fence(ModBlocks.TEMPESTADE_FENCE.get())
                .fenceGate(ModBlocks.TEMPESTADE_FENCE_GATE.get())
                .door(ModBlocks.TEMPESTADE_DOOR.get())
                .trapdoor(ModBlocks.TEMPESTADE_TRAPDOOR.get());

        blockModels.family(ModBlocks.VIDA_PLANKS.get())
                .stairs(ModBlocks.VIDA_STAIRS.get())
                .slab(ModBlocks.VIDA_SLAB.get())
                .pressurePlate(ModBlocks.VIDA_PRESSURE_PLATE.get())
                .button(ModBlocks.VIDA_BUTTON.get())
                .fence(ModBlocks.VIDA_FENCE.get())
                .fenceGate(ModBlocks.VIDA_FENCE_GATE.get())
                .door(ModBlocks.VIDA_DOOR.get())
                .trapdoor(ModBlocks.VIDA_TRAPDOOR.get());
    }

    private void generateForgeController(BlockModelGenerators blockModels) {
        Identifier controllerModel = Identifier.fromNamespaceAndPath(Nexus.MODID, "block/controlador_forja");
        Identifier formedModel = Identifier.fromNamespaceAndPath(Nexus.MODID, "block/forja_full");

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(ModBlocks.CONTROLADOR_FORJA.get())
                        .with(PropertyDispatch.initial(org.dantesys.nexus.blocks.ControladorForja.FORMED)
                                .select(false, BlockModelGenerators.variant(new Variant(controllerModel)))
                                .select(true, BlockModelGenerators.variant(new Variant(formedModel))))
                        .with(PropertyDispatch.modify(org.dantesys.nexus.blocks.ControladorForja.FACING)
                                .select(net.minecraft.core.Direction.NORTH, BlockModelGenerators.NOP)
                                .select(net.minecraft.core.Direction.EAST, BlockModelGenerators.Y_ROT_90)
                                .select(net.minecraft.core.Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(net.minecraft.core.Direction.WEST, BlockModelGenerators.Y_ROT_270))
        );
    }

    private static void generateMatriz(ItemModelGenerators itemModels, Item item, String textureName) {
        Material texture = new Material(
                Identifier.fromNamespaceAndPath(
                        Nexus.MODID,
                        "item/" + textureName
                )
        );

        TextureMapping textures = new TextureMapping()
                .put(MOLDE_TEXTURE, texture)
                .put(TextureSlot.PARTICLE, texture);

        Identifier modelId = MOLDE_COMUM.create(ModelLocationUtils.getModelLocation(item), textures, itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(modelId));
    }

}
