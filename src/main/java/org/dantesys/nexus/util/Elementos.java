package org.dantesys.nexus.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum Elementos implements StringRepresentable {

    NAO_ELEMENTAL("nao_elemental", ChatFormatting.WHITE),
    TEMPESTADE("tempestade", ChatFormatting.AQUA),
    SOMBRA("sombra", ChatFormatting.DARK_PURPLE),
    IGNEA("ignea", ChatFormatting.RED),
    LUZ("luz", ChatFormatting.YELLOW),
    VIDA("natureza", ChatFormatting.GREEN),
    METAL("metal", ChatFormatting.GRAY);

    private final String id;
    private final ChatFormatting cor;

    Elementos(String id, ChatFormatting cor) {
        this.id = id;
        this.cor = cor;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public ChatFormatting getCor() {
        return cor;
    }

    public Component getTooltip() {
        return Component.translatable("elemento.nexus." + id)
                .withStyle(cor);
    }

    public static Elementos byId(String id) {
        for (Elementos elemento : values()) {
            if (elemento.getSerializedName().equals(id)) {
                return elemento;
            }
        }

        return NAO_ELEMENTAL;
    }
}
