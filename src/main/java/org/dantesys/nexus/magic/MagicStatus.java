package org.dantesys.nexus.magic;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.dantesys.nexus.util.Elementos;

import java.util.EnumMap;

public class MagicStatus implements ValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicStatus> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MagicStatus decode(RegistryFriendlyByteBuf buf) {
            MagicStatus status = new MagicStatus();
            status.created = buf.readBoolean();
            status.mana = buf.readVarInt();
            status.maxMana = buf.readVarInt();
            status.regeneracao = buf.readVarInt();
            status.regeneracaoAcumulada = buf.readVarInt();
            status.nexusBiomeEventCooldown = buf.readVarInt();
            status.nexusBiomeEventPending = buf.readBoolean();
            status.manaSkinActive = buf.readBoolean();
            status.selectedMana = Elementos.byId(buf.readUtf(32));

            for (Elementos elemento : Elementos.values()) {
                status.elementos.get(elemento).copyFrom(ElementalStatus.decode(buf));
            }

            return status;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, MagicStatus status) {
            buf.writeBoolean(status.created);
            buf.writeVarInt(status.mana);
            buf.writeVarInt(status.maxMana);
            buf.writeVarInt(status.regeneracao);
            buf.writeVarInt(status.regeneracaoAcumulada);
            buf.writeVarInt(status.nexusBiomeEventCooldown);
            buf.writeBoolean(status.nexusBiomeEventPending);
            buf.writeBoolean(status.manaSkinActive);
            buf.writeUtf(status.selectedMana.getSerializedName(), 32);

            for (Elementos elemento : Elementos.values()) {
                status.elementos.get(elemento).encode(buf);
            }
        }
    };

    private boolean created = false;

    private int mana;
    private int maxMana;
    private int regeneracao;
    private int regeneracaoAcumulada;
    private int nexusBiomeEventCooldown;
    private boolean nexusBiomeEventPending;
    private boolean manaSkinActive;
    private Elementos selectedMana = Elementos.NAO_ELEMENTAL;
    private long lastManaUseTick = Long.MIN_VALUE;
    private long nextSimpleSpellTick;
    private long nextManaBeamImpactTick;

    private final EnumMap<Elementos, ElementalStatus> elementos =
            new EnumMap<>(Elementos.class);

    public MagicStatus() {
        for (Elementos elemento : Elementos.values()) {
            elementos.put(elemento, new ElementalStatus());
        }
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public int getRegeneracao() {
        return regeneracao;
    }

    public void setRegeneracao(int regeneracao) {
        this.regeneracao = regeneracao;
    }

    public ElementalStatus get(Elementos elemento) {
        return elementos.get(elemento);
    }

    public int getNexusBiomeEventCooldown() {
        return nexusBiomeEventCooldown;
    }

    public void setNexusBiomeEventCooldown(int nexusBiomeEventCooldown) {
        this.nexusBiomeEventCooldown = Math.max(0, nexusBiomeEventCooldown);
    }

    public void tickNexusBiomeEventCooldown(int ticks) {
        setNexusBiomeEventCooldown(nexusBiomeEventCooldown - ticks);
    }

    public boolean isNexusBiomeEventPending() {
        return nexusBiomeEventPending;
    }

    public void setNexusBiomeEventPending(boolean nexusBiomeEventPending) {
        this.nexusBiomeEventPending = nexusBiomeEventPending;
    }

    public Elementos getSelectedMana() {
        return selectedMana;
    }

    public void setSelectedMana(Elementos selectedMana) {
        this.selectedMana = selectedMana == null ? Elementos.NAO_ELEMENTAL : selectedMana;
    }

    public boolean isManaSkinActive() {
        return manaSkinActive;
    }

    public void setManaSkinActive(boolean manaSkinActive) {
        this.manaSkinActive = manaSkinActive;
    }

    public boolean canUseMana(int amount) {
        return mana >= amount;
    }

    public boolean useMana(int amount) {
        if (!canUseMana(amount)) {
            return false;
        }

        mana -= amount;
        return true;
    }

    public boolean startManaUseTick(long gameTime) {
        if (lastManaUseTick == gameTime) {
            return false;
        }
        lastManaUseTick = gameTime;
        return true;
    }

    public boolean canCastSimpleSpell(long gameTime) {
        return gameTime >= nextSimpleSpellTick;
    }

    public void setSimpleSpellCooldown(long gameTime, int cooldownTicks) {
        nextSimpleSpellTick = gameTime + Math.max(1, cooldownTicks);
    }

    public boolean startManaBeamImpact(long gameTime) {
        if (gameTime < nextManaBeamImpactTick) {
            return false;
        }
        nextManaBeamImpactTick = gameTime + 20L;
        return true;
    }

    public void addMana(int amount) {
        mana = Math.min(mana + amount, maxMana);
    }

    public boolean regenerarMana() {
        if (!created || regeneracao <= 0 || mana >= maxMana) {
            regeneracaoAcumulada = 0;
            return false;
        }
        regeneracaoAcumulada += regeneracao;
        int quantidade = regeneracaoAcumulada / 60;
        regeneracaoAcumulada %= 60;

        int manaAnterior = mana;
        addMana(quantidade);
        if (mana >= maxMana) {
            regeneracaoAcumulada = 0;
        }
        return mana != manaAnterior;
    }

    public void selectNextMana() {
        selectMana(1);
    }

    public void selectPreviousMana() {
        selectMana(-1);
    }

    private void selectMana(int step) {
        Elementos[] values = Elementos.values();
        int index = selectedMana.ordinal();

        do {
            index = Math.floorMod(index + step, values.length);
        } while (!this.elementos.get(values[index]).isUnlocked());

        selectedMana = values[index];
    }

    public void copyFrom(MagicStatus other) {
        this.created = other.created;
        this.mana = other.mana;
        this.maxMana = other.maxMana;
        this.regeneracao = other.regeneracao;
        this.regeneracaoAcumulada = other.regeneracaoAcumulada;
        this.nexusBiomeEventCooldown = other.nexusBiomeEventCooldown;
        this.nexusBiomeEventPending = other.nexusBiomeEventPending;
        this.manaSkinActive = other.manaSkinActive;
        this.selectedMana = other.selectedMana;
        this.nextSimpleSpellTick = other.nextSimpleSpellTick;

        for (Elementos elemento : Elementos.values()) {
            this.elementos.get(elemento).copyFrom(other.elementos.get(elemento));
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("created", created);
        output.putInt("mana", mana);
        output.putInt("maxMana", maxMana);
        output.putInt("regeneracao", regeneracao);
        output.putInt("regeneracaoAcumulada", regeneracaoAcumulada);
        output.putInt("nexusBiomeEventCooldown", nexusBiomeEventCooldown);
        output.putBoolean("nexusBiomeEventPending", nexusBiomeEventPending);
        output.putBoolean("manaSkinActive", manaSkinActive);
        output.putString("selectedMana", selectedMana.getSerializedName());
        output.putLong("nextSimpleSpellTick", nextSimpleSpellTick);

        ValueOutput elementosOutput = output.child("elementos");

        for (Elementos elemento : Elementos.values()) {
            elementosOutput.putChild(key(elemento), elementos.get(elemento));
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        created = input.getBooleanOr("created", false);
        mana = input.getIntOr("mana", 0);
        maxMana = input.getIntOr("maxMana", 0);
        regeneracao = input.getIntOr("regeneracao", 0);
        regeneracaoAcumulada = input.getIntOr("regeneracaoAcumulada", 0);
        nexusBiomeEventCooldown = input.getIntOr("nexusBiomeEventCooldown", 0);
        nexusBiomeEventPending = input.getBooleanOr("nexusBiomeEventPending", false);
        manaSkinActive = input.getBooleanOr("manaSkinActive", false);
        selectedMana = Elementos.byId(input.getStringOr("selectedMana", Elementos.NAO_ELEMENTAL.getSerializedName()));
        nextSimpleSpellTick = input.getLongOr("nextSimpleSpellTick", 0L);

        ValueInput elementosInput = input.childOrEmpty("elementos");

        for (Elementos elemento : Elementos.values()) {
            elementosInput.readChild(key(elemento), elementos.get(elemento));
        }
    }

    private static String key(Elementos elemento) {
        return elemento.getSerializedName();
    }
}
