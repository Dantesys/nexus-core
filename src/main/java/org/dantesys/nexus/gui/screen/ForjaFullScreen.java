package org.dantesys.nexus.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.blockentity.ControladorForjaBE;
import org.dantesys.nexus.gui.menu.ForjaFullMenu;

public class ForjaFullScreen extends AbstractContainerScreen<ForjaFullMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/forja_full_gui.png");
    private static final Identifier LAVA_BAR =
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/lava_bar.png");
    private static final Identifier MANA_BAR =
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/mana_bar.png");
    private static final Identifier PROGRESS_BAR =
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/forjando.png");

    public ForjaFullScreen(ForjaFullMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = -1000;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos,
                0, 0, imageWidth, imageHeight, 256, 256);
        renderLava(graphics);
        renderMana(graphics);
        renderProgress(graphics);
    }

    private void renderLava(GuiGraphicsExtractor graphics) {
        int height = scaled(menu.getLava(), ControladorForjaBE.LAVA_CAPACITY, 33);
        if (height > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, LAVA_BAR,
                    leftPos + 11, topPos + 36 + (33 - height),
                    0, 33 - height, 10, height, 10, 33);
        }
    }

    private void renderMana(GuiGraphicsExtractor graphics) {
        int width = scaled(menu.getMana(), ControladorForjaBE.MANA_CAPACITY, 160);
        if (width > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MANA_BAR,
                    leftPos + 8, topPos + 74, 0, 0, width, 6, 160, 6);
        }
    }

    private void renderProgress(GuiGraphicsExtractor graphics) {
        int width = scaled(menu.getProgress(), menu.getMaxProgress(), 57);
        if (width > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR,
                    leftPos + 89, topPos + 38, 0, 0, width, 8, 57, 8);
        }
    }

    private static int scaled(int value, int maximum, int pixels) {
        if (value <= 0 || maximum <= 0) return 0;
        return Math.clamp(value * pixels / maximum, 0, pixels);
    }
}
