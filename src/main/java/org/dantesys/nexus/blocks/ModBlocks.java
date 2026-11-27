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

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nexus.MODID);

    public static final DeferredBlock<BolaCristal> BOLA_CRISTAL = registerBlock("bola_cristal",
            properties -> new BolaCristal(properties.strength(3.0F, 1000f)
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


    /** Draconix: lava roxa (sem item). */
    public static final DeferredBlock<LiquidBlock> LAVA_ROXA = BLOCKS.registerBlock("lava_roxa",
            properties -> new LiquidBlock(ModFluids.LAVA_ROXA.get(), properties.mapColor(net.minecraft.world.level.material.MapColor.COLOR_PURPLE)
                    .replaceable().noCollision().strength(100.0F).lightLevel(state -> 12)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).noLootTable().liquid().sound(SoundType.EMPTY)));

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
