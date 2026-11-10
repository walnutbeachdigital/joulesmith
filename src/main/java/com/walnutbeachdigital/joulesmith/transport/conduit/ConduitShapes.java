package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.walnutbeachdigital.joulesmith.transport.PipeConnection;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The boxes a conduit is drawn and clicked as, in sixteenths of a block. Each conduit is 4 wide and runs in
 * its own lane 4 off the centre; a side joined to another conduit gets an arm to the block's edge, and a side
 * touching a block gets an arm ending in a plate (8 x 8 push or pull plates on ducts and pipes, a small cap
 * on cables).
 */
public final class ConduitShapes {
    public static final float HALF = 2, OFFSET = 4;

    public enum Kind { CORE, ARM, PUSH, PULL, CAP }

    /** One box. {@code axis} is the axis an arm or plate runs along (0 x, 1 y, 2 z); -1 for the core. */
    public record Piece(float[] min, float[] max, Kind kind, int axis, Direction dir) {
        public boolean contains(double x, double y, double z, double grow) {
            return x >= min[0] - grow && x <= max[0] + grow && y >= min[1] - grow && y <= max[1] + grow
                    && z >= min[2] - grow && z <= max[2] + grow;
        }
    }

    public static float[] centre(ConduitType type) {
        return new float[] {8 + OFFSET * type.lane[0], 8 + OFFSET * type.lane[1], 8 + OFFSET * type.lane[2]};
    }

    public static List<Piece> pieces(ConduitType type, PipeConnection[] connections) {
        List<Piece> out = new ArrayList<>();
        float[] c = centre(type);
        Direction straight = straightRun(connections);
        if (straight != null) {
            // one length right through, with no joint in the middle; plates on either end as usual
            int a = straight.getAxis().ordinal();
            float lo = endDepth(type, connections[straight.getOpposite().get3DDataValue()]);
            float hi = 16 - endDepth(type, connections[straight.get3DDataValue()]);
            out.add(box(c, a, lo, hi, HALF, Kind.ARM, straight));
            for (Direction dir : new Direction[] {straight, straight.getOpposite()}) addPlate(out, type, c, dir, connections[dir.get3DDataValue()]);
            return out;
        }
        out.add(new Piece(new float[] {c[0] - HALF, c[1] - HALF, c[2] - HALF}, new float[] {c[0] + HALF, c[1] + HALF, c[2] + HALF},
                Kind.CORE, -1, null));
        for (Direction dir : Direction.values()) {
            PipeConnection conn = connections[dir.get3DDataValue()];
            if (conn == PipeConnection.NONE) continue;
            int a = dir.getAxis().ordinal();
            boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            float plate = endDepth(type, conn);
            // the arm: from the core to the block's edge (or to the plate)
            float lo = positive ? c[a] + HALF : plate, hi = positive ? 16 - plate : c[a] - HALF;
            if (hi > lo) out.add(box(c, a, lo, hi, HALF, Kind.ARM, dir));
            addPlate(out, type, c, dir, conn);
        }
        return out;
    }

    /** The side a conduit runs straight through on, if it's joined on exactly two opposite sides. */
    private static Direction straightRun(PipeConnection[] connections) {
        Direction first = null;
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (connections[dir.get3DDataValue()] == PipeConnection.NONE) continue;
            count++;
            if (first == null) first = dir;
        }
        if (count != 2 || connections[first.getOpposite().get3DDataValue()] == PipeConnection.NONE) return null;
        return first.getOpposite();          // the positive one (first in Direction order is the negative side)
    }

    private static float endDepth(ConduitType type, PipeConnection conn) {
        return conn == PipeConnection.PUSH || conn == PipeConnection.PULL ? (type.plates ? 2 : 1) : 0;
    }

    private static void addPlate(List<Piece> out, ConduitType type, float[] c, Direction dir, PipeConnection conn) {
        if (conn != PipeConnection.PUSH && conn != PipeConnection.PULL) return;
        int a = dir.getAxis().ordinal();
        boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float plate = endDepth(type, conn), half = type.plates ? 4 : 3;
        Kind kind = !type.plates ? Kind.CAP : conn == PipeConnection.PULL ? Kind.PULL : Kind.PUSH;
        out.add(box(c, a, positive ? 16 - plate : 0, positive ? 16 : plate, half, kind, dir));
    }

    private static Piece box(float[] c, int axis, float lo, float hi, float half, Kind kind, Direction dir) {
        float[] min = new float[3], max = new float[3];
        for (int i = 0; i < 3; i++) {
            min[i] = i == axis ? lo : c[i] - half;
            max[i] = i == axis ? hi : c[i] + half;
        }
        return new Piece(min, max, kind, axis, dir);
    }

    private static final Map<Long, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private static final VoxelShape FALLBACK = Shapes.box(6 / 16.0, 6 / 16.0, 6 / 16.0, 10 / 16.0, 10 / 16.0, 10 / 16.0);

    /** The bundle's outline and collision shape, built once per look. */
    public static VoxelShape shape(BundleView view) {
        if (view.entries().isEmpty()) return FALLBACK;
        return SHAPES.computeIfAbsent(view.key() & SHAPE_MASK, k -> {
            VoxelShape shape = Shapes.empty();
            for (BundleView.Entry e : view.entries()) {
                for (Piece p : pieces(e.type(), e.connections())) {
                    shape = Shapes.or(shape, Shapes.box(p.min[0] / 16, p.min[1] / 16, p.min[2] / 16, p.max[0] / 16, p.max[1] / 16, p.max[2] / 16));
                }
            }
            return shape.optimize();
        });
    }

    // the key without the tiers (bits 1-3 of each conduit's 16), since tiers don't change the shape
    private static final long SHAPE_MASK = ~(0xEL | 0xEL << 16 | 0xEL << 32 | 0xEL << 48);

    /** The conduit under a point on the bundle (x, y, z in 0..1 within the block), or null. */
    public static ConduitType conduitAt(BundleView view, Vec3 local) {
        double x = local.x * 16, y = local.y * 16, z = local.z * 16;
        ConduitType best = null;
        double bestDist = Double.MAX_VALUE;
        for (BundleView.Entry e : view.entries()) {
            for (Piece p : pieces(e.type(), e.connections())) {
                if (p.contains(x, y, z, 0.05)) return e.type();
            }
            float[] c = centre(e.type());
            double d = (x - c[0]) * (x - c[0]) + (y - c[1]) * (y - c[1]) + (z - c[2]) * (z - c[2]);
            if (d < bestDist) {
                bestDist = d;
                best = e.type();
            }
        }
        return best;
    }

    /** Which side of a conduit a click hit: the arm or plate under the cursor, or the face of the core. */
    public static Direction sideAt(BundleView.Entry entry, Vec3 local, Direction face) {
        double x = local.x * 16, y = local.y * 16, z = local.z * 16;
        float[] c = centre(entry.type());
        double[] at = {x, y, z};
        for (Piece p : pieces(entry.type(), entry.connections())) {
            if (p.kind() != Kind.CORE && p.contains(x, y, z, 0.05)) {
                // the half of the arm (a straight run is one arm through the block) the click is on
                Direction.Axis axis = Direction.Axis.values()[p.axis()];
                return Direction.get(at[p.axis()] < c[p.axis()] ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE, axis);
            }
        }
        // on the core (or a side that isn't joined): the side the click is furthest out on
        double dx = x - c[0], dy = y - c[1], dz = z - c[2];
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (Math.max(ax, Math.max(ay, az)) < HALF - 0.1) return face;
        if (ax >= ay && ax >= az) return dx < 0 ? Direction.WEST : Direction.EAST;
        if (ay >= az) return dy < 0 ? Direction.DOWN : Direction.UP;
        return dz < 0 ? Direction.NORTH : Direction.SOUTH;
    }

    private ConduitShapes() {}
}
