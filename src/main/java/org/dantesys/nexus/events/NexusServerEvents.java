package org.dantesys.nexus.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.commands.NexusCommands;
import org.dantesys.nexus.recipe.ModRecipes;

public class NexusServerEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NexusCommands.register(event);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Nexus.LOGGER.info("Nexus server setup complete");
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.INFUSOR_TYPE.get());
        event.sendRecipes(ModRecipes.FORJA_TYPE.get());
    }
}
