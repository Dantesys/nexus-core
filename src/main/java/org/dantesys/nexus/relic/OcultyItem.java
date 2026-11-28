package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Oculty: oculos do espiao. Usado na cabeca, no lugar do capacete. */
public final class OcultyItem extends RelicItem {
    public OcultyItem(Properties properties) {
        super(properties.humanoidArmor(org.dantesys.nexus.items.ModArmorMaterial.RELIC_ARMOR_MATERIAL, net.minecraft.world.item.equipment.ArmorType.HELMET)
                        .equippable(EquipmentSlot.HEAD), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.oculty.modo_furtivo"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.oculty.micro_agente"), OcultyEffects.MICRO_MANA, OcultyEffects::micro),
                RelicSkill.active(Component.translatable("relic.nexus.oculty.x_ray"), OcultyEffects.XRAY_MANA, OcultyEffects::xray),
                RelicSkill.active(Component.translatable("relic.nexus.oculty.plano_f"), OcultyEffects.PLAN_MANA, OcultyEffects::planF),
                new RelicSkill(Component.translatable("relic.nexus.oculty.imaterial_spy"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.oculty.nenhum_mob_ou_jogador_num"),
                        OcultyEffects::canSpy, OcultyEffects::spy, OcultyEffects.SPY_MANA));
    }

    /** Durante o Imaterial Spy, a tecla da reliquia abre o inventario do jogador que voce esta olhando. */
    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (OcultyEffects.spying(player)) return OcultyEffects.peek(player);
        return super.activate(player, stack, slot);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.oculty.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> OcultyEffects.MICRO_COOLDOWN;
            case 2 -> OcultyEffects.XRAY_COOLDOWN;
            case 3 -> OcultyEffects.PLAN_COOLDOWN;
            case 4 -> OcultyEffects.SPY_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, proximo Plano F, segundos restantes do Micro Agente]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) OcultyEffects.nextPlan(stack), (float) OcultyEffects.microSecondsLeft(player));
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        return List.of(Component.translatable("relic.nexus.oculty.proximo_plano_f", Component.translatable(OcultyEffects.PLAN_NAMES[(int) modelValue(stack, 1, 0)])));
    }

    @Override public Component hudInfo(ItemStack stack) {
        int seconds = (int) modelValue(stack, 2, 0);
        Component plan = Component.translatable("relic.nexus.oculty.hud_plano_f", Component.translatable(OcultyEffects.PLAN_NAMES[(int) modelValue(stack, 1, 0)]));
        return seconds > 0 ? Component.translatable("relic.nexus.oculty.hud_micro", seconds, plan) : plan;
    }
}
