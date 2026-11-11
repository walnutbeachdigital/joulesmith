package com.walnutbeachdigital.joulesmith.transport;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shape every duct, pipe and cable shares: a 4-wide run, with a 6-wide nub where it turns, branches,
 * ends or meets a block. A straight run between two of its own kind has no nub ({@link #NODE} false), so a
 * line of them reads as one length.
 */
public final class PipeShapes {
    /** Whether the block shows its nub (false only in the middle of a straight run). */
    public static final BooleanProperty NODE = BooleanProperty.create("node");

    public static final VoxelShape NUB = Block.box(5, 5, 5, 11, 11, 11);
    public static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    public static final Map<Direction, VoxelShape> PLATES = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 8));
        ARMS.put(Direction.SOUTH, Block.box(6, 6, 8, 10, 10, 16));
        ARMS.put(Direction.WEST, Block.box(0, 6, 6, 8, 10, 10));
        ARMS.put(Direction.EAST, Block.box(8, 6, 6, 16, 10, 10));
        ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 8, 10));
        ARMS.put(Direction.UP, Block.box(6, 8, 6, 10, 16, 10));
        PLATES.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 2));
        PLATES.put(Direction.SOUTH, Block.box(4, 4, 14, 12, 12, 16));
        PLATES.put(Direction.WEST, Block.box(0, 4, 4, 2, 12, 12));
        PLATES.put(Direction.EAST, Block.box(14, 4, 4, 16, 12, 12));
        PLATES.put(Direction.DOWN, Block.box(4, 0, 4, 12, 2, 12));
        PLATES.put(Direction.UP, Block.box(4, 14, 4, 12, 16, 12));
    }

    /** Side looks: 0 nothing, 1 an arm, 2 an arm with a plate. */
    private static final VoxelShape[] CACHE = new VoxelShape[729 * 2];

    public static VoxelShape shape(int[] sides, boolean node) {
        int key = 0;
        for (int s : sides) key = key * 3 + s;
        key = key * 2 + (node ? 1 : 0);
        VoxelShape shape = CACHE[key];
        if (shape != null) return shape;
        shape = node ? NUB : Shapes.empty();
        Direction[] dirs = Direction.values();
        for (int i = 0; i < 6; i++) {
            if (sides[i] >= 1) shape = Shapes.or(shape, ARMS.get(dirs[i]));
            if (sides[i] == 2) shape = Shapes.or(shape, PLATES.get(dirs[i]));
        }
        return CACHE[key] = shape.optimize();
    }

    /**
     * Whether to show the nub: everywhere except the middle of a straight run, where exactly two opposite
     * sides join others of the same kind ({@code link}) and nothing else is connected ({@code any}).
     */
    public static boolean node(boolean[] any, boolean[] link) {
        int count = 0;
        Direction first = null;
        for (Direction d : Direction.values()) {
            if (!any[d.get3DDataValue()]) continue;
            count++;
            if (first == null) first = d;
        }
        if (count != 2) return true;
        Direction opposite = first.getOpposite();
        return !(any[opposite.get3DDataValue()] && link[first.get3DDataValue()] && link[opposite.get3DDataValue()]);
    }

    private PipeShapes() {}
}
