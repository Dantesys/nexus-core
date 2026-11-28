package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Brutix: machado do barbaro. Quanto mais apanha, mais forte fica. */
public final class BrutixItem extends RelicItem {
    public BrutixItem(Properties properties) {
        super(properties.axe(ModToolMaterial.RELIC, 6.0F, -3.1F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.brutix.furia"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.brutix.boomerang"), BrutixEffects.BOOMERANG_MANA, BrutixEffects::boomerang),
                RelicSkill.active(Component.translatable("relic.nexus.brutix.giro_destruidor"), BrutixEffects.SPIN_MANA, BrutixEffects::spin),
                RelicSkill.active(Component.translatable("relic.nexus.brutix.grito_de_guerra"), BrutixEffects.ROAR_MANA, BrutixEffects::roar),
                new RelicSkill(Component.translatable("relic.nexus.brutix.descarga_de_furia"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.brutix.furia_em_pelo_menos_metade"),
                        BrutixEffects::canDischarge, BrutixEffects::discharge, BrutixEffects.DISCHARGE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.brutix.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> BrutixEffects.BOOMERANG_COOLDOWN;
            case 2 -> BrutixEffects.SPIN_COOLDOWN;
            case 3 -> BrutixEffects.ROAR_COOLDOWN;
            case 4 -> BrutixEffects.DISCHARGE_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, estagios de furia, maximo, % da barra, machado em voo]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) BrutixEffects.stages(player), (float) BrutixEffects.maxStages(player),
                (float) Math.round(BrutixEffects.barPercent(player)), BrutixEffects.flying(player) ? 1f : 0f);
    }

    /** A furia aparece como barra propria no HUD (MagicHud.renderFury). */
    @Override public Component hudInfo(ItemStack stack) { return null; }
}
