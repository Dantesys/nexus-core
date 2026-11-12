package org.dantesys.nexus.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.worldgen.ModTreeGrowes;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nexus.MODID);

    public static final DeferredBlock<BolaCristal> BOLA_CRISTAL = registerBlock("bola_cristal",
            properties -> new BolaCristal(properties.explosionResistance(1000f)
                    .requiresCorrectToolForDrops().dynamicShape().noOcclusion()));

    public static final DeferredBlock<ColetorArcanoBasico> COLETOR_ARCANO_BASICO = registerBlock("coletor_basico",
            properties -> new ColetorArcanoBasico(properties.strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).dynamicShape().noOcclusion()));

    public static final DeferredBlock<InfusorArcanoBasico> INFUSOR_ARCANO_BASICO = registerBlock("infusor_basico",
            properties -> new InfusorArcanoBasico(properties.strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).dynamicShape().noOcclusion()));

    public static final DeferredBlock<TanqueForja> TANQUE_FORJA = registerBlock("tanque_forja",
            properties -> new TanqueForja(properties.strength(4.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final DeferredBlock<ControladorForja> CONTROLADOR_FORJA = registerBlock("controlador_forja",
            properties -> new ControladorForja(properties.strength(4.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<ForjaCollisionBlock> FORJA_COLLISION = BLOCKS.registerBlock(
            "forja_collision",
            properties -> new ForjaCollisionBlock(properties.noLootTable()
                    .strength(-1.0F, 3600000.0F).noOcclusion()));


    public static final DeferredBlock<Block> STEEL_BLOCK = registerBlock("steel_block",
            properties -> new Block(properties.strength(4f)
                    .requiresCorrectToolForDrops().sound(SoundType.IRON)));

    public static final DeferredBlock<RotatedPillarBlock> IGNEA_LOG = registerBlock("ignea_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> IGNEA_PLANKS = registerBlock("ignea_planks",
            properties -> new Block(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<DoorBlock> IGNEA_DOOR = registerBlock("ignea_door",
            properties -> new DoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> IGNEA_TRAPDOOR = registerBlock("ignea_trapdoor",
            properties -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<ButtonBlock> IGNEA_BUTTON = registerBlock("ignea_button",
            properties -> new ButtonBlock(BlockSetType.OAK,20, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<PressurePlateBlock> IGNEA_PRESSURE_PLATE = registerBlock("ignea_pressure_plate",
            properties -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<StairBlock> IGNEA_STAIRS = registerBlock("ignea_stairs",
            properties -> new StairBlock(Blocks.OAK_STAIRS.defaultBlockState(), properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SlabBlock> IGNEA_SLAB = registerBlock("ignea_slab",
            properties -> new SlabBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> IGNEA_FENCE = registerBlock("ignea_fence",
            properties -> new FenceBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceGateBlock> IGNEA_FENCE_GATE = registerBlock("ignea_fence_gate",
            properties -> new FenceGateBlock(WoodType.OAK, properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_IGNEA_LOG = registerBlock("stripped_ignea_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> LUZ_LOG = registerBlock("luz_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD).lightLevel(_ -> 7)));
    public static final DeferredBlock<Block> LUZ_PLANKS = registerBlock("luz_planks",
            properties -> new Block(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD).lightLevel(_ -> 7)));
    public static final DeferredBlock<DoorBlock> LUZ_DOOR = registerBlock("luz_door",
            properties -> new DoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).lightLevel(_ -> 7).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> LUZ_TRAPDOOR = registerBlock("luz_trapdoor",
            properties -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).lightLevel(_ -> 7).noOcclusion()));
    public static final DeferredBlock<ButtonBlock> LUZ_BUTTON = registerBlock("luz_button",
            properties -> new ButtonBlock(BlockSetType.OAK, 20, properties.strength(0.5F).lightLevel(_ -> 7).sound(SoundType.WOOD)));
    public static final DeferredBlock<PressurePlateBlock> LUZ_PRESSURE_PLATE = registerBlock("luz_pressure_plate",
            properties -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(0.5F).lightLevel(_ -> 7).sound(SoundType.WOOD)));
    public static final DeferredBlock<StairBlock> LUZ_STAIRS = registerBlock("luz_stairs",
            properties -> new StairBlock(Blocks.OAK_STAIRS.defaultBlockState(), properties.strength(2.0F, 3.0F).lightLevel(_ -> 7).sound(SoundType.WOOD)));
    public static final DeferredBlock<SlabBlock> LUZ_SLAB = registerBlock("luz_slab",
            properties -> new SlabBlock(properties.strength(2.0F, 3.0F).lightLevel(_ -> 7).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> LUZ_FENCE = registerBlock("luz_fence",
            properties -> new FenceBlock(properties.strength(2.0F, 3.0F).lightLevel(_ -> 7).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceGateBlock> LUZ_FENCE_GATE = registerBlock("luz_fence_gate",
            properties -> new FenceGateBlock(WoodType.OAK, properties.strength(2.0F, 3.0F).lightLevel(_ -> 7).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_LUZ_LOG = registerBlock("stripped_luz_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).lightLevel(_ -> 7).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> METAL_LOG = registerBlock("metal_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> METAL_PLANKS = registerBlock("metal_planks",
            properties -> new Block(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<DoorBlock> METAL_DOOR = registerBlock("metal_door",
            properties -> new DoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> METAL_TRAPDOOR = registerBlock("metal_trapdoor",
            properties -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<ButtonBlock> METAL_BUTTON = registerBlock("metal_button",
            properties -> new ButtonBlock(BlockSetType.OAK, 20, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<PressurePlateBlock> METAL_PRESSURE_PLATE = registerBlock("metal_pressure_plate",
            properties -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<StairBlock> METAL_STAIRS = registerBlock("metal_stairs",
            properties -> new StairBlock(Blocks.OAK_STAIRS.defaultBlockState(), properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SlabBlock> METAL_SLAB = registerBlock("metal_slab",
            properties -> new SlabBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> METAL_FENCE = registerBlock("metal_fence",
            properties -> new FenceBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceGateBlock> METAL_FENCE_GATE = registerBlock("metal_fence_gate",
            properties -> new FenceGateBlock(WoodType.OAK, properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_METAL_LOG = registerBlock("stripped_metal_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> SOMBRIA_LOG = registerBlock("sombria_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> SOMBRIA_PLANKS = registerBlock("sombria_planks",
            properties -> new Block(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<DoorBlock> SOMBRIA_DOOR = registerBlock("sombria_door",
            properties -> new DoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> SOMBRIA_TRAPDOOR = registerBlock("sombria_trapdoor",
            properties -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<ButtonBlock> SOMBRIA_BUTTON = registerBlock("sombria_button",
            properties -> new ButtonBlock(BlockSetType.OAK, 20, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<PressurePlateBlock> SOMBRIA_PRESSURE_PLATE = registerBlock("sombria_pressure_plate",
            properties -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<StairBlock> SOMBRIA_STAIRS = registerBlock("sombria_stairs",
            properties -> new StairBlock(Blocks.OAK_STAIRS.defaultBlockState(), properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SlabBlock> SOMBRIA_SLAB = registerBlock("sombria_slab",
            properties -> new SlabBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> SOMBRIA_FENCE = registerBlock("sombria_fence",
            properties -> new FenceBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceGateBlock> SOMBRIA_FENCE_GATE = registerBlock("sombria_fence_gate",
            properties -> new FenceGateBlock(WoodType.OAK, properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SOMBRIA_LOG = registerBlock("stripped_sombria_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> TEMPESTADE_LOG = registerBlock("tempestade_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> TEMPESTADE_PLANKS = registerBlock("tempestade_planks",
            properties -> new Block(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<DoorBlock> TEMPESTADE_DOOR = registerBlock("tempestade_door",
            properties -> new DoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> TEMPESTADE_TRAPDOOR = registerBlock("tempestade_trapdoor",
            properties -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<ButtonBlock> TEMPESTADE_BUTTON = registerBlock("tempestade_button",
            properties -> new ButtonBlock(BlockSetType.OAK, 20, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<PressurePlateBlock> TEMPESTADE_PRESSURE_PLATE = registerBlock("tempestade_pressure_plate",
            properties -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<StairBlock> TEMPESTADE_STAIRS = registerBlock("tempestade_stairs",
            properties -> new StairBlock(Blocks.OAK_STAIRS.defaultBlockState(), properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SlabBlock> TEMPESTADE_SLAB = registerBlock("tempestade_slab",
            properties -> new SlabBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> TEMPESTADE_FENCE = registerBlock("tempestade_fence",
            properties -> new FenceBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceGateBlock> TEMPESTADE_FENCE_GATE = registerBlock("tempestade_fence_gate",
            properties -> new FenceGateBlock(WoodType.OAK, properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_TEMPESTADE_LOG = registerBlock("stripped_tempestade_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<RotatedPillarBlock> VIDA_LOG = registerBlock("vida_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> VIDA_PLANKS = registerBlock("vida_planks",
            properties -> new Block(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<DoorBlock> VIDA_DOOR = registerBlock("vida_door",
            properties -> new DoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> VIDA_TRAPDOOR = registerBlock("vida_trapdoor",
            properties -> new TrapDoorBlock(BlockSetType.OAK, properties.strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<ButtonBlock> VIDA_BUTTON = registerBlock("vida_button",
            properties -> new ButtonBlock(BlockSetType.OAK, 20, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<PressurePlateBlock> VIDA_PRESSURE_PLATE = registerBlock("vida_pressure_plate",
            properties -> new PressurePlateBlock(BlockSetType.OAK, properties.strength(0.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<StairBlock> VIDA_STAIRS = registerBlock("vida_stairs",
            properties -> new StairBlock(Blocks.OAK_STAIRS.defaultBlockState(), properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SlabBlock> VIDA_SLAB = registerBlock("vida_slab",
            properties -> new SlabBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> VIDA_FENCE = registerBlock("vida_fence",
            properties -> new FenceBlock(properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceGateBlock> VIDA_FENCE_GATE = registerBlock("vida_fence_gate",
            properties -> new FenceGateBlock(WoodType.OAK, properties.strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_VIDA_LOG = registerBlock("stripped_vida_log",
            properties -> new RotatedPillarBlock(properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<NexusLeavesBlock> IGNEA_LEAVES = registerBlock("ignea_leaves", NexusLeavesBlock::new);
    public static final DeferredBlock<NexusLeavesBlock> LUZ_LEAVES = registerBlock("luz_leaves",
            properties -> new NexusLeavesBlock(properties.lightLevel(_ -> 7)));
    public static final DeferredBlock<NexusLeavesBlock> METAL_LEAVES = registerBlock("metal_leaves", NexusLeavesBlock::new);
    public static final DeferredBlock<NexusLeavesBlock> SOMBRIA_LEAVES = registerBlock("sombria_leaves", NexusLeavesBlock::new);
    public static final DeferredBlock<NexusLeavesBlock> TEMPESTADE_LEAVES = registerBlock("tempestade_leaves", NexusLeavesBlock::new);
    public static final DeferredBlock<NexusLeavesBlock> VIDA_LEAVES = registerBlock("vida_leaves", NexusLeavesBlock::new);
    public static final DeferredBlock<SaplingBlock> IGNEA_SAPLING = registerBlock("ignea_sapling",
            properties -> new SaplingBlock(ModTreeGrowes.IGNEA,properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SaplingBlock> LUZ_SAPLING = registerBlock("luz_sapling",
            properties -> new SaplingBlock(ModTreeGrowes.LUZ,properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SaplingBlock> METAL_SAPLING = registerBlock("metal_sapling",
            properties -> new SaplingBlock(ModTreeGrowes.METAL,properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SaplingBlock> SOMBRIA_SAPLING = registerBlock("sombria_sapling",
            properties -> new SaplingBlock(ModTreeGrowes.SOMBRIA,properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SaplingBlock> TEMPESTADE_SAPLING = registerBlock("tempestade_sapling",
            properties -> new SaplingBlock(ModTreeGrowes.TEMPESTADE,properties.strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<SaplingBlock> VIDA_SAPLING = registerBlock("vida_sapling",
            properties -> new SaplingBlock(ModTreeGrowes.VIDA,properties.strength(2.0F).sound(SoundType.WOOD)));

    public static class NexusLeavesBlock extends LeavesBlock {
        public static final MapCodec<NexusLeavesBlock> CODEC = simpleCodec(NexusLeavesBlock::new);

        public NexusLeavesBlock(BlockBehaviour.Properties properties) {
            super(0.01F, properties
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
        }

        @Override
        public MapCodec<? extends LeavesBlock> codec() {
            return CODEC;
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
            ColorParticleOption particle = ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, level.getClientLeafTintColor(pos));
            ParticleUtils.spawnParticleBelow(level, pos, random, particle);
        }
    }


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name, properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
