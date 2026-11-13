package org.dantesys.nexus.relic;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.dantesys.nexus.Nexus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plantas salvas num arquivo do mundo; o item da planta guarda so o ID e um resumo. */
public class Blueprints extends SavedData {
    private static final Codec<Blueprints> CODEC = Codec.unboundedMap(Codec.STRING, Blueprint.CODEC)
            .xmap(Blueprints::new, data -> data.plantas);
    public static final SavedDataType<Blueprints> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Nexus.MODID, "krefty_plantas"), Blueprints::new, CODEC);

    private final Map<String, Blueprint> plantas;

    public Blueprints() { this(Map.of()); }
    private Blueprints(Map<String, Blueprint> plantas) { this.plantas = new HashMap<>(plantas); }

    public static Blueprints get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Blueprint get(String id) { return id == null ? null : plantas.get(id); }

    public String add(Blueprint blueprint) {
        String id = UUID.randomUUID().toString();
        plantas.put(id, blueprint);
        setDirty();
        return id;
    }
}
