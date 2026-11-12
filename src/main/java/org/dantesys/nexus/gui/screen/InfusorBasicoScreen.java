package org.dantesys.nexus.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.gui.menu.InfusorBasicoMenu;

public class InfusorBasicoScreen extends AbstractContainerScreen<InfusorBasicoMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/infusor_gui.png");

    public InfusorBasicoScreen(InfusorBasicoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256
        );
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        renderInputIndicators(graphics, x, y);
    }

    private void renderInputIndicators(GuiGraphicsExtractor graphics, int x, int y) {
        if (menu.hasInput(0)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, menu.getTextureFromSlot(0),
                    x + 61, y + 15, 0, 0, 27, 27, 54, 54);
        }
        if (menu.hasInput(1)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, menu.getTextureFromSlot(1),
                    x + 88, y + 15, 27, 0, 27, 27, 54, 54);
        }
        if (menu.hasInput(2)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, menu.getTextureFromSlot(2),
                    x + 61, y + 42, 0, 27, 27, 27, 54, 54);
        }
        if (menu.hasInput(3)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, menu.getTextureFromSlot(3),
                    x + 88, y + 42, 27, 27, 27, 27, 54, 54);
        }
    }
}
