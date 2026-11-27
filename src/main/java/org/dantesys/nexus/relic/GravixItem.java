package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Gravix: bota gravitacional. So vale calcada. */
public final class GravixItem extends RelicItem {
    public GravixItem(Properties properties) {
        super(properties.attributes(SteelyItem.armor("gravix", EquipmentSlotGroup.FEET, 3, 3, 0.1))
                        .component(DataComponents.EQUIPPABLE, SteelyItem.equip(EquipmentSlot.FEET, "gravix")),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Gravidade Zero"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Campo Gravitacional"), GravixEffects.FIELD_MANA, GravixEffects::heavyField),
                RelicSkill.active(Component.literal("Zona 0"), GravixEffects.ZONE_MANA, GravixEffects::zeroZone),
                RelicSkill.active(Component.literal("Gravidade Manipulada"), GravixEffects.SHIFT_MANA, GravixEffects::armShift),
                new RelicSkill(Component.literal("Gravity Max"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Em deriva (Gravidade Zero ligada) e com a Zona 0 em recarga."),
                        GravixEffects::canMax, GravixEffects::gravityMax, GravixEffects.MAX_MANA));
    }

    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        setArmorAsset(stack, EquipmentSlot.FEET, "gravix", stage, false);
    }

    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(GravixEffects.drifting(player) ? 1f : 0f);
    }

    @Override public Component hudInfo(ItemStack stack) {
        return modelValue(stack, 1, 0) > 0 ? Component.literal("Gravidade Zero") : null;
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Agachar + pular desliga a gravidade (deriva, gastando mana); repetir religa. Imune a dano de queda.";
            case 1 -> "Por 15s, gravidade pesada num raio de 5: Lentidão, sem pulo alto nem élitro, projéteis inimigos caem.";
            case 2 -> "Por 15s, gravidade zero num raio de 5: inimigos flutuam e derivam; ao acabar, quem estiver alto cai.";
            case 3 -> "O próximo golpe muda a gravidade do alvo para onde você olha, por 5s (3s para cima).";
            case 4 -> "Por 10s, pulsos gravitacionais num raio de 30: dano, lentidão, prende e encolhe (mais forte perto de você).";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> GravixEffects.FIELD_COOLDOWN;
            case 2 -> GravixEffects.ZONE_COOLDOWN;
            case 3 -> GravixEffects.SHIFT_COOLDOWN;
            case 4 -> GravixEffects.MAX_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
