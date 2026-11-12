package org.dantesys.nexus.relic;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Progresso por tipo de reliquia (niveis 1 a 20), pertencente ao jogador, nunca ao ItemStack. */
public class RelicMastery implements ValueIOSerializable {
    public static final int MAX_LEVEL = 20;
    public static final int XP_PER_LEVEL = 100;
    public static final int XP_PER_USE = 1;
    private static final int MAX_XP = (MAX_LEVEL - 1) * XP_PER_LEVEL;
    private static final Codec<Map<String, Integer>> XP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private final Map<String, Integer> experience = new HashMap<>();
    private final Set<String> discovered = new HashSet<>();
    private final Map<String, Integer> enabledPassives = new HashMap<>();

    public boolean isPassiveEnabled(String relic, int slot) {
        return slot >= 0 && slot < RelicSkill.MAX_SKILLS && (enabledPassives.getOrDefault(relic, 0) & (1 << slot)) != 0;
    }

    /** Preferencia do jogador; alternar nao conta como uso nem revela a secreta. */
    public boolean togglePassive(String relic, int slot) {
        if (slot < 0 || slot >= RelicSkill.MAX_SKILLS) return false;
        if (!isPassiveEnabled(relic, slot) && level(relic) < RelicSkill.requiredLevel(slot)) return false;
        int mask = enabledPassives.getOrDefault(relic, 0) ^ (1 << slot);
        if (mask == 0) enabledPassives.remove(relic);
        else enabledPassives.put(relic, mask);
        return true;
    }

    public int experience(String relic) { return experience.getOrDefault(relic, 0); }
    public int level(String relic) { return 1 + experience(relic) / XP_PER_LEVEL; }
    public boolean isDiscovered(String relic) { return discovered.contains(relic); }
    public boolean discover(String relic) { return discovered.add(relic); }

    /** Define o inicio do nivel, preservando descobertas e desligando passivas que voltaram a ficar bloqueadas. */
    public void setLevel(String relic, int level) {
        if (level < 1 || level > MAX_LEVEL) throw new IllegalArgumentException("Mastery level must be between 1 and " + MAX_LEVEL);
        experience.put(relic, (level - 1) * XP_PER_LEVEL);
        int mask = enabledPassives.getOrDefault(relic, 0);
        for (int slot = 0; slot < RelicSkill.MAX_SKILLS; slot++) {
            if (level < RelicSkill.requiredLevel(slot)) mask &= ~(1 << slot);
        }
        if (mask == 0) enabledPassives.remove(relic);
        else enabledPassives.put(relic, mask);
    }

    public boolean addExperience(String relic, int amount) {
        int previous = experience(relic);
        int next = (int) Math.min(MAX_XP, previous + (long) Math.max(0, amount));
        if (previous == next) return false;
        experience.put(relic, next);
        return true;
    }

    @Override public void serialize(ValueOutput output) {
        output.store("experience", XP_CODEC, experience);
        output.store("enabled_passives", XP_CODEC, enabledPassives);
        output.store("discovered", Codec.STRING.listOf(), java.util.List.copyOf(discovered));
    }

    @Override public void deserialize(ValueInput input) {
        experience.clear();
        enabledPassives.clear();
        input.read("enabled_passives", XP_CODEC).orElse(Map.of()).forEach((id, mask) ->
                enabledPassives.put(id, mask & 31));
        input.read("experience", XP_CODEC).orElse(Map.of()).forEach((id, xp) ->
                experience.put(id, Math.clamp(xp, 0, MAX_XP)));
        discovered.clear();
        discovered.addAll(input.read("discovered", Codec.STRING.listOf()).orElse(java.util.List.of()));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RelicMastery> STREAM_CODEC = new StreamCodec<>() {
        @Override public RelicMastery decode(RegistryFriendlyByteBuf buf) {
            RelicMastery result = new RelicMastery();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) result.experience.put(buf.readUtf(), Math.clamp(buf.readVarInt(), 0, MAX_XP));
            int secrets = buf.readVarInt();
            for (int i = 0; i < secrets; i++) result.discovered.add(buf.readUtf());
            int passives = buf.readVarInt();
            for (int i = 0; i < passives; i++) result.enabledPassives.put(buf.readUtf(), buf.readVarInt() & 31);
            return result;
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, RelicMastery value) {
            buf.writeVarInt(value.experience.size());
            value.experience.forEach((id, xp) -> { buf.writeUtf(id); buf.writeVarInt(xp); });
            buf.writeVarInt(value.discovered.size());
            value.discovered.forEach(buf::writeUtf);
            buf.writeVarInt(value.enabledPassives.size());
            value.enabledPassives.forEach((id, mask) -> { buf.writeUtf(id); buf.writeVarInt(mask); });
        }
    };
}
