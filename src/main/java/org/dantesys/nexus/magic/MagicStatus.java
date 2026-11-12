package org.dantesys.nexus.magic;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Mana do jogador no Nexus Core: uma mana única (não elemental) com 4 status de 0 a 1000.
 * <ul>
 *     <li>capacidade: mana máxima;</li>
 *     <li>regeneração: velocidade de recuperação;</li>
 *     <li>afinidade: força das habilidades (dano/poder);</li>
 *     <li>ressonância: eficiência (reduz custo e recarga).</li>
 * </ul>
 * Mana elemental fica para os addons (veja {@code org.dantesys.nexus.api.element}).
 */
public class MagicStatus implements ValueIOSerializable {
    public static final int MAX_STATUS = 1000;

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicStatus> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MagicStatus decode(RegistryFriendlyByteBuf buf) {
            MagicStatus status = new MagicStatus();
            status.created = buf.readBoolean();
            status.mana = buf.readVarInt();
            status.maxMana = buf.readVarInt();
            status.regeneracao = buf.readVarInt();
            status.afinidade = buf.readVarInt();
            status.ressonancia = buf.readVarInt();
            status.regeneracaoAcumulada = buf.readVarInt();
            status.manaSkinActive = buf.readBoolean();
            return status;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, MagicStatus status) {
            buf.writeBoolean(status.created);
            buf.writeVarInt(status.mana);
            buf.writeVarInt(status.maxMana);
            buf.writeVarInt(status.regeneracao);
            buf.writeVarInt(status.afinidade);
            buf.writeVarInt(status.ressonancia);
            buf.writeVarInt(status.regeneracaoAcumulada);
            buf.writeBoolean(status.manaSkinActive);
        }
    };

    private boolean created = false;

    private int mana;
    private int maxMana;
    private int regeneracao;
    private int afinidade;
    private int ressonancia;
    private int regeneracaoAcumulada;
    private boolean manaSkinActive;
    private long lastManaUseTick = Long.MIN_VALUE;
    private long nextManaBeamImpactTick;

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
        this.mana = Math.max(0, Math.min(mana, maxMana));
    }

    /** Capacidade: a mana máxima. */
    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = clamp(maxMana);
        this.mana = Math.min(this.mana, this.maxMana);
    }

    public int getRegeneracao() {
        return regeneracao;
    }

    public void setRegeneracao(int regeneracao) {
        this.regeneracao = clamp(regeneracao);
    }

    public int getAfinidade() {
        return afinidade;
    }

    public void setAfinidade(int afinidade) {
        this.afinidade = clamp(afinidade);
    }

    public int getRessonancia() {
        return ressonancia;
    }

    public void setRessonancia(int ressonancia) {
        this.ressonancia = clamp(ressonancia);
    }

    private static int clamp(int valor) {
        return Math.max(0, Math.min(MAX_STATUS, valor));
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

    public void copyFrom(MagicStatus other) {
        this.created = other.created;
        this.mana = other.mana;
        this.maxMana = other.maxMana;
        this.regeneracao = other.regeneracao;
        this.afinidade = other.afinidade;
        this.ressonancia = other.ressonancia;
        this.regeneracaoAcumulada = other.regeneracaoAcumulada;
        this.manaSkinActive = other.manaSkinActive;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("created", created);
        output.putInt("mana", mana);
        output.putInt("maxMana", maxMana);
        output.putInt("regeneracao", regeneracao);
        output.putInt("afinidade", afinidade);
        output.putInt("ressonancia", ressonancia);
        output.putInt("regeneracaoAcumulada", regeneracaoAcumulada);
        output.putBoolean("manaSkinActive", manaSkinActive);
    }

    @Override
    public void deserialize(ValueInput input) {
        created = input.getBooleanOr("created", false);
        maxMana = clamp(input.getIntOr("maxMana", 0));
        mana = Math.max(0, Math.min(maxMana, input.getIntOr("mana", 0)));
        regeneracao = clamp(input.getIntOr("regeneracao", 0));
        afinidade = clamp(input.getIntOr("afinidade", 0));
        ressonancia = clamp(input.getIntOr("ressonancia", 0));
        regeneracaoAcumulada = input.getIntOr("regeneracaoAcumulada", 0);
        manaSkinActive = input.getBooleanOr("manaSkinActive", false);
    }
}
