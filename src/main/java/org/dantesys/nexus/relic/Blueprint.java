package org.dantesys.nexus.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

/**
 * Planta da Construcao Instantanea. data[x + sx*(y + sy*z)] = indice na paleta + 1 (0 = vazio).
 * So estados de bloco: nunca inventario ou dados de bloco.
 */
public record Blueprint(int sx, int sy, int sz, List<BlockState> palette, int[] data) {
    public static final Codec<Blueprint> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("sx").forGetter(Blueprint::sx),
            Codec.INT.fieldOf("sy").forGetter(Blueprint::sy),
            Codec.INT.fieldOf("sz").forGetter(Blueprint::sz),
            BlockState.CODEC.listOf().fieldOf("palette").forGetter(Blueprint::palette),
            Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream).fieldOf("data").forGetter(Blueprint::data)
    ).apply(i, Blueprint::new));

    public int index(int x, int y, int z) { return x + sx * (y + sy * z); }

    public BlockState at(int x, int y, int z) {
        int value = data[index(x, y, z)];
        return value <= 0 || value > palette.size() ? null : palette.get(value - 1);
    }

    public int count() {
        int count = 0;
        for (int value : data) if (value > 0) count++;
        return count;
    }

    /** Tamanho em X e Z depois de girar. */
    public static int rotatedX(int sx, int sz, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? sz : sx;
    }
    public static int rotatedZ(int sx, int sz, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? sx : sz;
    }

    /** Posicao relativa (x,z) depois de girar, mantendo o canto minimo em 0,0. */
    public static int[] rotate(int x, int z, int sx, int sz, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new int[]{x, z};
            case CLOCKWISE_90 -> new int[]{sz - 1 - z, x};
            case CLOCKWISE_180 -> new int[]{sx - 1 - x, sz - 1 - z};
            case COUNTERCLOCKWISE_90 -> new int[]{z, sx - 1 - x};
        };
    }

    /** Canto minimo no mundo: a planta fica centralizada no ponto mirado, apoiada nele. */
    public BlockPos corner(BlockPos origin, Rotation rotation) {
        return origin.offset(-rotatedX(sx, sz, rotation) / 2, 0, -rotatedZ(sx, sz, rotation) / 2);
    }

    /** Percorre os blocos ja girados, em coordenadas do mundo. */
    public void forEach(BlockPos origin, Rotation rotation, BiConsumer<BlockPos, BlockState> action) {
        BlockPos corner = corner(origin, rotation);
        for (int z = 0; z < sz; z++) for (int y = 0; y < sy; y++) for (int x = 0; x < sx; x++) {
            BlockState state = at(x, y, z);
            if (state == null) continue;
            int[] r = rotate(x, z, sx, sz, rotation);
            action.accept(corner.offset(r[0], y, r[1]), state.rotate(rotation));
        }
    }
}
