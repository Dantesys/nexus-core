package org.dantesys.nexus;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.dantesys.nexus.client.MagicHud;
import org.dantesys.nexus.client.ColetorArcanoBasicoRenderer;
import org.dantesys.nexus.client.InfusorArcanoBasicoRenderer;
import org.dantesys.nexus.client.ControladorForjaRenderer;
import org.dantesys.nexus.blocks.blockentity.ModBlockEntities;
import org.dantesys.nexus.gui.screen.InfusorBasicoScreen;
import org.dantesys.nexus.network.ChangeSelectedManaPayload;
import org.dantesys.nexus.gui.ModMenus;
import org.dantesys.nexus.gui.screen.BolaCristalScreen;
import org.dantesys.nexus.gui.screen.ForjaFullScreen;
import org.dantesys.nexus.network.ToggleManaSkinPayload;
import org.dantesys.nexus.network.UseManaPayload;
import org.dantesys.nexus.network.ActivateRelicPayload;
import org.dantesys.nexus.items.ModItems;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Nexus.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Nexus.MODID, value = Dist.CLIENT)
public class NexusClient {
    private static final KeyMapping.Category NEXUS_CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "nexus")
    );
    private static final KeyMapping PREVIOUS_MANA = new KeyMapping(
            "key.nexus.previous_mana",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_LEFT,
            NEXUS_CATEGORY
    );
    private static final KeyMapping NEXT_MANA = new KeyMapping(
            "key.nexus.next_mana",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_RIGHT,
            NEXUS_CATEGORY
    );
    private static final KeyMapping TOGGLE_MANA_SKIN = new KeyMapping(
            "key.nexus.toggle_mana_skin",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_G,
            NEXUS_CATEGORY
    );
    private static final KeyMapping USE_MANA = new KeyMapping(
            "key.nexus.use_mana",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_R,
            NEXUS_CATEGORY
    );
    private static final KeyMapping ACTIVATE_RELIC = new KeyMapping(
            "key.nexus.activate_relic", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, NEXUS_CATEGORY);

    public NexusClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Nexus.LOGGER.info("HELLO FROM CLIENT SETUP");
        Nexus.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BOLA_CRISTAL_MENU.get(), BolaCristalScreen::new);
        event.register(ModMenus.INFUSOR_BASICO_MENU.get(), InfusorBasicoScreen::new);
        event.register(ModMenus.FORJA_FULL_MENU.get(), ForjaFullScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.COLETOR_ARCANO_BASICO_BE.get(), ColetorArcanoBasicoRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.INFUSOR_ARCANO_BASICO_BE.get(), InfusorArcanoBasicoRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.CONTROLADOR_FORJA_BE.get(), ControladorForjaRenderer::new);
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(NEXUS_CATEGORY);
        event.register(PREVIOUS_MANA);
        event.register(NEXT_MANA);
        event.register(TOGGLE_MANA_SKIN);
        event.register(USE_MANA);
        event.register(ACTIVATE_RELIC);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(Nexus.MODID, "magic_bar"),
                (guiGraphics, deltaTracker) -> MagicHud.render(guiGraphics)
        );
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.Key event) {
        while (PREVIOUS_MANA.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ChangeSelectedManaPayload(false));
        }

        while (NEXT_MANA.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ChangeSelectedManaPayload(true));
        }

        while (TOGGLE_MANA_SKIN.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ToggleManaSkinPayload());
        }
        while (ACTIVATE_RELIC.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ActivateRelicPayload());
        }

    }

    @SubscribeEvent
    static void onRelicScroll(InputEvent.MouseScrollingEvent event) {
        if (!canSelectRelic() || event.getScrollDeltaY() == 0) return;
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new org.dantesys.nexus.network.SelectRelicSkillPayload(event.getScrollDeltaY() < 0));
    }

    @SubscribeEvent
    static void onRelicClick(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != InputConstants.MOUSE_BUTTON_RIGHT
                || event.getAction() != InputConstants.PRESS || !canSelectRelic()) return;
        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new org.dantesys.nexus.network.SelectRelicSkillPayload(true));
    }

    private static boolean canSelectRelic() {
        var minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.gui.screen() == null
                && org.dantesys.nexus.client.RelicTooltip.shiftDown()
                && !org.dantesys.nexus.relic.RelicItem.controlledRelic(minecraft.player).isEmpty();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null && USE_MANA.isDown()) {
            ClientPacketDistributor.sendToServer(new UseManaPayload());
        }
    }
}
