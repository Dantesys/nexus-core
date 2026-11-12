package org.dantesys.nexus.magic;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

public class ElementalStatus implements ValueIOSerializable {

    private boolean unlocked = false;
    private int afinidade;
    private int ressonancia;

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public int getAfinidade() {
        return afinidade;
    }

    public void setAfinidade(int afinidade) {
        this.afinidade = afinidade;
    }

    public int getRessonancia() {
        return ressonancia;
    }

    public void setRessonancia(int ressonancia) {
        this.ressonancia = ressonancia;
    }

    public void copyFrom(ElementalStatus other) {
        this.unlocked = other.unlocked;
        this.afinidade = other.afinidade;
        this.ressonancia = other.ressonancia;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(unlocked);
        buf.writeVarInt(afinidade);
        buf.writeVarInt(ressonancia);
    }

    public static ElementalStatus decode(RegistryFriendlyByteBuf buf) {
        ElementalStatus status = new ElementalStatus();
        status.unlocked = buf.readBoolean();
        status.afinidade = buf.readVarInt();
        status.ressonancia = buf.readVarInt();
        return status;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("unlocked", unlocked);
        output.putInt("afinidade", afinidade);
        output.putInt("ressonancia", ressonancia);
    }

    @Override
    public void deserialize(ValueInput input) {
        unlocked = input.getBooleanOr("unlocked", false);
        afinidade = input.getIntOr("afinidade", 0);
        ressonancia = input.getIntOr("ressonancia", 0);
    }
}
