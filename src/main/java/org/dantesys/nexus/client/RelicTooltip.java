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
        if (event.getItemStack().getItem() instanceof org.dantesys.nexus.items.PlantaItem) {
            var data = org.dantesys.nexus.items.PlantaItem.data(event.getItemStack());
            var lines = event.getToolTip();
            if (data.getStringOr(org.dantesys.nexus.items.PlantaItem.ID, "").isEmpty()) {
                lines.add(Component.translatable("message.nexus.relictooltip.planta_vazia").withStyle(ChatFormatting.GRAY));
                return;
            }
            lines.add(Component.translatable("message.nexus.relictooltip.tamanho", data.getIntOr("Sx", 0), data.getIntOr("Sy", 0), data.getIntOr("Sz", 0))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("message.nexus.relictooltip.blocos", data.getIntOr("Blocos", 0)).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("message.nexus.relictooltip.custo_de_mana", data.getIntOr("Custo", 0)).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("message.nexus.relictooltip.rotacao", Math.floorMod(data.getIntOr("Rotacao", 0), 4) * 90).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("message.nexus.relictooltip.na_outra_mao_com_a")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (!(event.getItemStack().getItem() instanceof RelicItem relic) || event.getEntity() == null) return;
        RelicMastery mastery = event.getEntity().getData(ModAttachmentType.RELIC_MASTERY);
        int level = mastery.level(relic.masteryId());
        var lines = event.getToolTip();
        lines.add(Component.translatable("message.nexus.relictooltip.maestria", level, RelicMastery.MAX_LEVEL).withStyle(ChatFormatting.GOLD));
        var elemento = relic.getElement(event.getItemStack());
            lines.add(Component.translatable("message.nexus.relictooltip.mana_de_funcionamento").append(elemento == null
                    ? Component.translatable("element.nexus.nao_elemental")
                    : org.dantesys.nexus.api.element.NexusElement.name(elemento.unwrapKey().orElseThrow().identifier())));
        if (!shiftDown() && !controlDown()) {
            lines.add(Component.translatable("message.nexus.relictooltip.segure_shift_ou_ctrl_para").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (level < RelicMastery.MAX_LEVEL) {
            lines.add(Component.translatable("message.nexus.relictooltip.experiencia", mastery.experience(relic.masteryId()) % RelicMastery.XP_PER_LEVEL, RelicMastery.XP_PER_LEVEL).withStyle(ChatFormatting.GRAY));
        }
        lines.addAll(relic.extraTooltip(event.getItemStack()));
        int selected = relic.selectedSkill(event.getItemStack());
        int last = relic.getSkills().size() - 1;
        for (int slot = 0; slot < relic.getSkills().size(); slot++) {
            RelicSkill skill = relic.getSkills().get(slot);
            int required = RelicSkill.requiredLevel(slot);
            boolean unlocked = level >= required;
            boolean active = skill.kind() == RelicSkill.Kind.ACTIVE;
            String type = active ? (slot == last && slot == RelicSkill.MAX_SKILLS - 1 ? "message.nexus.relictooltip.ativa_final" : "message.nexus.relictooltip.ativa") : "message.nexus.relictooltip.passiva";
            lines.add(Component.translatable("message.nexus.relictooltip.nv", (active && selected == slot ? "> " : ""), required)
                    .append(skill.name()).append(" — ").append(Component.translatable(type))
                    .append(unlocked ? Component.empty() : Component.translatable("message.nexus.relictooltip.bloqueada"))
                    .withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (active && unlocked) {
                int cost = relic.getManaCost(event.getItemStack(), slot);
                long cooldown = relic.getCooldownRemaining(event.getItemStack(), event.getEntity().level().getGameTime(), slot);
                lines.add((cooldown > 0 ? Component.translatable("message.nexus.relictooltip.mana_recarga", cost, (cooldown + 19) / 20)
                        : Component.translatable("message.nexus.relictooltip.mana", cost))
                        .withStyle(ChatFormatting.AQUA));
            }
            Component description = relic.skillDescription(event.getItemStack(), slot);
            if (!description.getString().isEmpty()) lines.add(description.copy().withStyle(ChatFormatting.GRAY));
            if (!skill.conditionDescription().getString().isEmpty()) {
                lines.add(Component.translatable("message.nexus.relictooltip.requisitos").append(skill.conditionDescription()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        lines.add(Component.translatable("message.nexus.relictooltip.trocar_habilidade_shift_rolagem_do")
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("message.nexus.relictooltip.tecla_da_reliquia_usar_a")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
