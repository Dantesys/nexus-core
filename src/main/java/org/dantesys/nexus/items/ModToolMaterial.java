package org.dantesys.nexus.items;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;
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

    private ModToolMaterial() {
    }
}
