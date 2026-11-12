package org.dantesys.nexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.relic.RelicItem;
import org.dantesys.nexus.relic.RelicMastery;
import org.dantesys.nexus.relic.RelicSkill;

@EventBusSubscriber(modid = Nexus.MODID, value = Dist.CLIENT)
public final class RelicTooltip {
    private RelicTooltip() {}
    public static boolean shiftDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }
    public static boolean controlDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof RelicItem relic) || event.getEntity() == null) return;
        RelicMastery mastery = event.getEntity().getData(ModAttachmentType.RELIC_MASTERY);
        int level = mastery.level(relic.masteryId());
        var lines = event.getToolTip();
        lines.add(Component.literal("Maestria: " + level + "/100").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Mana de funcionamento: ").append(relic.getElement(event.getItemStack()).getTooltip()));
        if (!shiftDown() && !controlDown()) {
            lines.add(Component.literal("Segure Shift ou Ctrl para ver as habilidades").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (level < RelicMastery.MAX_LEVEL) {
            lines.add(Component.literal("Experiência: " + mastery.experience(relic.masteryId()) % RelicMastery.XP_PER_LEVEL
                    + "/" + RelicMastery.XP_PER_LEVEL).withStyle(ChatFormatting.GRAY));
        }
        for (int slot = 0; slot < relic.getSkills().size(); slot++) {
            RelicSkill skill = relic.getSkills().get(slot);
            int required = RelicSkill.requiredLevel(slot);
            if (slot == 3 && !mastery.isDiscovered(relic.masteryId())) {
                lines.add(Component.literal((relic.selectedSkill(event.getItemStack()) == slot ? "> " : "")
                        + "Habilidade secreta: ??? (maestria 100)").withStyle(ChatFormatting.DARK_PURPLE));
                if (level == 100 && skill.kind() == RelicSkill.Kind.PASSIVE) {
                    lines.add(Component.literal(mastery.isPassiveEnabled(relic.masteryId(), slot)
                            ? "Passiva: ligada" : "Passiva: desligada").withStyle(ChatFormatting.GRAY));
                }
                continue;
            }
            boolean unlocked = level >= required;
            String type = skill.kind() == RelicSkill.Kind.PASSIVE ? "Passiva" : "Ativa";
            lines.add(Component.literal((relic.selectedSkill(event.getItemStack()) == slot ? "> " : "")
                    + (slot + 1) + ". ").append(skill.name()).append(" — " + type
                    + (unlocked ? " (liberada)" : " (maestria " + required + ")"))
                    .withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (unlocked && skill.kind() == RelicSkill.Kind.PASSIVE) {
                lines.add(Component.literal(mastery.isPassiveEnabled(relic.masteryId(), slot)
                        ? "Ligada" : "Desligada").withStyle(ChatFormatting.YELLOW));
            }
            lines.add(Component.literal("Mana base por efeito: " + relic.getManaCost(event.getItemStack(), slot))
                    .withStyle(ChatFormatting.AQUA));
            if (!relic.skillDescription(event.getItemStack(), slot).getString().isEmpty()) {
                lines.add(relic.skillDescription(event.getItemStack(), slot).copy().withStyle(ChatFormatting.GRAY));
            }
            if (slot == 3) {
                lines.add(Component.literal("Condição: ").append(skill.conditionDescription()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        lines.add(Component.literal("Selecionar: Shift + clique direito ou Shift + rolagem")
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("Tecla da relíquia: usar selecionada ou ligar/desligar passiva")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
