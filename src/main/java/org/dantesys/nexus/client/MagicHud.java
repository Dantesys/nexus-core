package org.dantesys.nexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;

public class MagicHud {
    private static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/manabar/base.png");
    private static final ManaTexture NAO_ELEMENTAL_TEXTURE = manaTexture("nao_elemental", 1043, 112);
    private static final ManaTexture TEMPESTADE_TEXTURE = manaTexture("tempestade", 1044, 104);
    private static final ManaTexture SOMBRIA_TEXTURE = manaTexture("sombria", 1051, 110);
    private static final ManaTexture IGNEA_TEXTURE = manaTexture("ignea", 1048, 109);
    private static final ManaTexture LUZ_TEXTURE = manaTexture("luz", 1044, 114);
    private static final ManaTexture VIDA_TEXTURE = manaTexture("vida", 1039, 109);
    private static final ManaTexture METAL_TEXTURE = manaTexture("metal", 1041, 94);
    private static final int TEXTURE_WIDTH = 1428;
    private static final int TEXTURE_HEIGHT = 275;
    private static final int WIDTH = 170;
    private static final int HEIGHT = 28;

    // Area do preenchimento dentro da base.
    private static final int INNER_X = 20;
    private static final int INNER_Y = 9;
    private static final int INNER_WIDTH = 130;
    private static final int INNER_HEIGHT = 14;

    public static void render(GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden()) {
            return;
        }

        MagicStatus status = minecraft.player.getData(ModAttachmentType.MAGIC_STATS);
        if (status.getMaxMana() <= 0) {
            return;
        }

        int x = (guiGraphics.guiWidth() - WIDTH) / 2;
        int y = guiGraphics.guiHeight() - 72;
        int fillWidth = Math.max(0, Math.min(INNER_WIDTH, status.getMana() * INNER_WIDTH / status.getMaxMana()));
        ManaTexture manaTexture = manaTexture(status.getSelectedMana());

        if (fillWidth > 0) {
            int sourceWidth = Math.max(1, manaTexture.width() * fillWidth / INNER_WIDTH);
            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    manaTexture.id(),
                    x + INNER_X,
                    y + INNER_Y,
                    0,
                    0,
                    fillWidth,
                    INNER_HEIGHT,
                    sourceWidth,
                    manaTexture.height(),
                    manaTexture.width(),
                    manaTexture.height()
            );
        }

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BASE_TEXTURE,
                x,
                y,
                0,
                0,
                WIDTH,
                HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    private static ManaTexture manaTexture(Elementos elemento) {
        return switch (elemento) {
            case TEMPESTADE -> TEMPESTADE_TEXTURE;
            case SOMBRA -> SOMBRIA_TEXTURE;
            case IGNEA -> IGNEA_TEXTURE;
            case LUZ -> LUZ_TEXTURE;
            case VIDA -> VIDA_TEXTURE;
            case METAL -> METAL_TEXTURE;
            case NAO_ELEMENTAL -> NAO_ELEMENTAL_TEXTURE;
        };
    }

    private static ManaTexture manaTexture(String name, int width, int height) {
        return new ManaTexture(
                Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/manabar/" + name + ".png"),
                width,
                height
        );
    }

    private record ManaTexture(Identifier id, int width, int height) {
    }
}
