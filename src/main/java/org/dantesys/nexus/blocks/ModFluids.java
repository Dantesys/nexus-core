package org.dantesys.nexus.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.dantesys.nexus.Nexus;

/** Fluidos do mod. Lava roxa (Draconix): sopro do dragao jogado na lava; nao causa dano nem queima. */
public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Nexus.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Nexus.MODID);

    public static final DeferredHolder<FluidType, FluidType> LAVA_ROXA_TYPE = TYPES.register("lava_roxa", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.nexus.lava_roxa")
            .canSwim(false).canDrown(false).canExtinguish(false).canConvertToSource(false).supportsBoating(false).canHydrate(false)
            .pathType(PathType.LAVA).adjacentPathType(null)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
            .lightLevel(12).density(3000).viscosity(6000).temperature(300).motionScale(0.0023333333333333335D)
            .rarity(Rarity.EPIC)));

    public static final DeferredHolder<Fluid, FlowingFluid> LAVA_ROXA = FLUIDS.register("lava_roxa", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, FlowingFluid> LAVA_ROXA_FLOWING = FLUIDS.register("lava_roxa_flowing", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(LAVA_ROXA_TYPE, LAVA_ROXA, LAVA_ROXA_FLOWING)
                .block(ModBlocks.LAVA_ROXA).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100f);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
