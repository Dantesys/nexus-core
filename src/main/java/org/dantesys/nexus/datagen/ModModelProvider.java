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
    // A Rupty usa uma definição de item feita à mão (src/main/resources/assets/nexus/items/rupty.json).
    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.item.Item>> getKnownItems() {
        return super.getKnownItems().filter(h -> !h.unwrapKey().map(k -> k.identifier().getPath().equals("rupty")).orElse(false));
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
