package org.dantesys.nexus.relic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Leitura das listas de configuracao ("a=b" e ids). */
public final class ConfigLists {
    private ConfigLists() {}

    public static Map<String, String> pairs(List<? extends String> list) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String entry : list) {
            int eq = entry.indexOf('=');
            if (eq > 0) map.put(entry.substring(0, eq).trim(), entry.substring(eq + 1).trim());
        }
        return map;
    }

    public static Optional<Holder.Reference<MobEffect>> effect(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.get(key);
    }

    public static List<Holder<MobEffect>> effects(List<? extends String> ids) {
        List<Holder<MobEffect>> list = new ArrayList<>();
        for (String id : ids) effect(id).ifPresent(list::add);
        return list;
    }
}
