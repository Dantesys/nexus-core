package org.dantesys.nexus.items;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.dantesys.nexus.tags.ModTags;

public final class ModToolMaterial {
    public static final ToolMaterial STEEL_MATERIA = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            900,
            7.0F,
            2.5F,
            12,
            ModTags.Items.STEEL_INGOTS
    );

    /**
     * Material das reliquias: mesmo nivel de mineracao do netherite (2031 / 9.0 / 4.0 / 15),
     * com velocidade e dano um pouco acima. A durabilidade e so referencia: reliquias sao inquebraveis.
     */
    public static final ToolMaterial RELIC = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2031,
            10.0F,
            4.5F,
            15,
            ModTags.Items.MATRIZ_RELIQUIA
    );

    /** Clava das reliquias (Dwarky): mesma velocidade da clava vanilla, dano base no nivel do netherite. */
    public static ItemAttributeModifiers relicMaceAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 7.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private ModToolMaterial() {
    }
}
