package org.dantesys.nexus.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.Nexus;

/** Lobo temporario da Alcateia (Domix): um lobo comum com a aparencia espectral. */
public class LoboEspectral extends Wolf {
    private static final Identifier MANSO = texture("manso"), BRAVO = texture("bravo"), FILHOTE = texture("filhote");

    public LoboEspectral(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Nexus.MODID, "textures/entity/lobo_espectral/" + name + ".png");
    }

    @Override
    public Identifier getTexture() {
        if (isBaby()) return FILHOTE;
        return isAngry() ? BRAVO : MANSO;
    }
}
