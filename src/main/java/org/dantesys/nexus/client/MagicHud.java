package org.dantesys.nexus.client;

import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;

public class MagicHud {
    private static final Identifier BASE_TEXTURE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/manabar/base.png");
    private static final ManaTexture NAO_ELEMENTAL_TEXTURE = manaTexture("nao_elemental", 1043, 112);
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
        ManaTexture manaTexture = NAO_ELEMENTAL_TEXTURE;

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
        renderRelic(guiGraphics, minecraft, y);
    }

    // ---------- Brutix: barra de furia ----------

    private static final Identifier FURY_BASE = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/furia/base.png");
    private static final Identifier FURY_FILL = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/furia/preenchimento.png");
    private static final Identifier FURY_ON = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/furia/estagio_aceso.png");
    private static final Identifier FURY_OFF = Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/furia/estagio_apagado.png");
    private static final int FILL_TEXTURE_WIDTH = 1043, FILL_TEXTURE_HEIGHT = 112, STAGE_TEXTURE = 54, STAGE_SIZE = 10;

    /** Barra de furia acima da linha da reliquia: preenchimento do proximo estagio e os estagios acesos. */
    private static void renderFury(GuiGraphicsExtractor guiGraphics, net.minecraft.world.item.ItemStack stack, int barY) {
        int stages = (int) org.dantesys.nexus.relic.RelicItem.modelValue(stack, 1, 0);
        int max = Math.max(1, (int) org.dantesys.nexus.relic.RelicItem.modelValue(stack, 2, 3));
        int pct = (int) org.dantesys.nexus.relic.RelicItem.modelValue(stack, 3, 0);
        int x = (guiGraphics.guiWidth() - WIDTH) / 2;
        int y = barY - 12 - HEIGHT;
        int fillWidth = stages >= max ? INNER_WIDTH : Math.max(0, Math.min(INNER_WIDTH, pct * INNER_WIDTH / 100));
        if (fillWidth > 0) {
            int sourceWidth = Math.max(1, FILL_TEXTURE_WIDTH * fillWidth / INNER_WIDTH);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FURY_FILL, x + INNER_X, y + INNER_Y, 0, 0, fillWidth, INNER_HEIGHT,
                    sourceWidth, FILL_TEXTURE_HEIGHT, FILL_TEXTURE_WIDTH, FILL_TEXTURE_HEIGHT);
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, FURY_BASE, x, y, 0, 0, WIDTH, HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        int gap = 2, rowWidth = max * STAGE_SIZE + (max - 1) * gap;
        int sx = (guiGraphics.guiWidth() - rowWidth) / 2, sy = y - STAGE_SIZE - 1;
        for (int i = 0; i < max; i++) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, i < stages ? FURY_ON : FURY_OFF, sx + i * (STAGE_SIZE + gap), sy, 0, 0,
                    STAGE_SIZE, STAGE_SIZE, STAGE_TEXTURE, STAGE_TEXTURE, STAGE_TEXTURE, STAGE_TEXTURE);
        }
    }

    // ---------- Oculty: icone do proximo Plano F ----------

    private static final Identifier[] PLAN_ICONS = {
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/plano_f/invisibilidade.png"),
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/plano_f/velocidade.png"),
            Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/gui/plano_f/voo.png")};

    /** Linha acima da barra: habilidade ativa selecionada, recarga e info da reliquia (ex.: fragmentos). */
    private static void renderRelic(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int barY) {
        var stack = org.dantesys.nexus.relic.RelicItem.controlledRelic(minecraft.player);
        if (!(stack.getItem() instanceof org.dantesys.nexus.relic.RelicItem relic) || relic.getSkills().isEmpty()) return;
        int slot = relic.selectedSkill(stack);
        var skill = relic.getSkills().get(slot);
        net.minecraft.network.chat.MutableComponent line;
        if (skill.kind() != org.dantesys.nexus.relic.RelicSkill.Kind.ACTIVE || !relic.isUnlocked(minecraft.player, slot)) {
            line = net.minecraft.network.chat.Component.translatable("message.nexus.magichud.sem_habilidade_ativa");
        } else {
            line = skill.name().copy();
            long cooldown = relic.getCooldownRemaining(stack, minecraft.player.level().getGameTime(), slot);
            if (cooldown > 0) line.append(" (" + (cooldown + 19) / 20 + "s)");
        }
        var info = relic.hudInfo(stack);
        if (info != null) line.append("  ·  ").append(info);
        int width = minecraft.font.width(line);
        int textX = (guiGraphics.guiWidth() - width) / 2;
        guiGraphics.text(minecraft.font, line, textX, barY - 10, 0xFFC890FF, true);
        if (relic instanceof org.dantesys.nexus.relic.BrutixItem) {
            renderFury(guiGraphics, stack, barY);
        } else if (relic instanceof org.dantesys.nexus.relic.OcultyItem && relic.isUnlocked(minecraft.player, 3)) {
            int plan = Math.floorMod((int) org.dantesys.nexus.relic.RelicItem.modelValue(stack, 1, 0), PLAN_ICONS.length);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PLAN_ICONS[plan], textX + width + 3, barY - 12, 0, 0, 12, 12, 18, 18, 18, 18);
        }
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
