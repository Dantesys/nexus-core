package org.dantesys.nexus.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.gui.menu.BolaCristalMenu;

public class BolaCristalScreen extends AbstractContainerScreen<BolaCristalMenu> {
    private static final float STATUS_TEXT_SCALE = 1.35F;
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/bola_cristal_gui.png");

    public BolaCristalScreen(BolaCristalMenu menu, Inventory inventory, Component title) {
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

        Identifier statusTexture = Identifier.fromNamespaceAndPath(Nexus.MODID, menu.getBgStats());
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                statusTexture,
                leftPos + 7,
                topPos + 13,
                0.0F,
                0.0F,
                161,
                58,
                161,
                58
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.isElementoBloqueado()) {
            return;
        }

        int color = menu.getTextColor();
        drawCentered(graphics, 11, 50, menu.getStatus(0), color);
        drawCentered(graphics, 43, 50, menu.getStatus(1), color);
        drawCentered(graphics, 105, 50, menu.getStatus(2), color);
        drawCentered(graphics, 138, 50, menu.getStatus(3), color);
    }

    private void drawCentered(GuiGraphicsExtractor graphics, int x, int y, Component text, int color) {
        float scaledWidth = font.width(text) * STATUS_TEXT_SCALE;
        float centeredX = x + (28 - scaledWidth) / 2.0F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(centeredX, y - 2);
        graphics.pose().scale(STATUS_TEXT_SCALE, STATUS_TEXT_SCALE);
        graphics.text(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }
}
