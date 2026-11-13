package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.core.component.DataComponents;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;
import java.util.Optional;

/** Bastry: escudo que guarda o dano bloqueado e o devolve. Identidade: refletir projeteis. */
public final class BastryItem extends RelicItem {
    public BastryItem(Properties properties) {
        super(properties.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                        0.25F, 1.0F, List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK))),
                MatrizModos.ESPADA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Carga"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Campo Reflexivo"), BastryEffects.FIELD_MANA, BastryEffects::field),
                RelicSkill.active(Component.literal("Counter"), BastryEffects.COUNTER_MANA, BastryEffects::counter),
                RelicSkill.active(Component.literal("Pele de Aço"), BastryEffects.STEEL_MANA, BastryEffects::steelSkin),
                new RelicSkill(Component.literal("Intocável"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Capacidade rank A ou maior, vida em até 75% ou carga em pelo menos 75%, "
                                + "e todas as outras habilidades da Bastry em recarga."),
                        BastryEffects::canUntouchable, BastryEffects::untouchable, BastryEffects.UNTOUCHABLE_MANA));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BLOCK; }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "O dano bloqueado vira carga (limite pela maestria e pela Capacidade). Com a carga cheia, o dano passa. "
                    + "A carga cai com o tempo e é gasta no próximo golpe: vale o maior entre o dano da carga e o do item.";
            case 1 -> "Campo de 2 blocos por ~10s que devolve projéteis para quem atirou. Quem está dentro fica protegido; "
                    + "você não pode usar projéteis enquanto ele dura.";
            case 2 -> "Por 10s, recebe só metade do dano corpo a corpo e devolve o dobro do que recebeu. Sem escudo nesse tempo.";
            case 3 -> "Gasta a carga e a transforma em resistências (25%, 50%, 75% e 100% da carga).";
            case 4 -> "Por 30s, ignora todo dano (menos void e /kill) e efeitos negativos. A carga converte os efeitos ruins em bons.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> BastryEffects.FIELD_COOLDOWN;
            case 2 -> BastryEffects.COUNTER_COOLDOWN;
            case 3 -> BastryEffects.STEEL_COOLDOWN;
            case 4 -> BastryEffects.UNTOUCHABLE_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, carga, limite]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of((float) Math.floor(BastryEffects.charge(player)), (float) Math.floor(BastryEffects.cap(player)));
    }

    @Override public Component hudInfo(ItemStack stack) {
        return Component.literal("Carga " + (int) modelValue(stack, 1, 0) + "/" + (int) modelValue(stack, 2, 0));
    }
}
