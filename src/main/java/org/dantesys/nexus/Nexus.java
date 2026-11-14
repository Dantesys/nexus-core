package org.dantesys.nexus;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.blocks.blockentity.ModBlockEntities;
import org.dantesys.nexus.creativetab.ModCreativeTab;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.events.NexusPlayerEvents;
import org.dantesys.nexus.events.NexusServerEvents;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.gui.ModMenus;
import org.dantesys.nexus.network.ModNetwork;
import org.dantesys.nexus.recipe.ModRecipes;
import org.slf4j.Logger;

@Mod(Nexus.MODID)
public class Nexus {
    public static final String MODID = "nexus";
    public static final Logger LOGGER = LogUtils.getLogger();


    public Nexus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);

        ModCreativeTab.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModAttachmentType.register(modEventBus);
        ModEffects.register(modEventBus);
        org.dantesys.nexus.entity.ModEntities.register(modEventBus);
        ModRecipes.register(modEventBus);
        NeoForge.EVENT_BUS.register(NexusPlayerEvents.class);
        NeoForge.EVENT_BUS.register(NexusServerEvents.class);

        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

}
