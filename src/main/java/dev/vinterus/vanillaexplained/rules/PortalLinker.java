package dev.vinterus.vanillaexplained.rules;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Predicts where a Nether portal will lead, following vanilla PortalForcer:
 * the destination point is the entry position scaled by 8 (or 1/8), and the game picks the
 * nearest portal block within a square search radius (16 blocks in the Nether, 128 in the
 * Overworld), closest by 3D distance, then lowest Y. If none is found, a new portal is built
 * near the destination point.
 */
public final class PortalLinker {
	public static final double SCALE = 8.0;
	public static final int SEARCH_RADIUS_NETHER = 16;
	public static final int SEARCH_RADIUS_OVERWORLD = 128;

	public record Pos(int x, int y, int z) {
		public long distSqr(Pos o) {
			long dx = x - o.x, dy = y - o.y, dz = z - o.z;
			return dx * dx + dy * dy + dz * dz;
		}
	}

	public record Result(Pos destination, Optional<Pos> linkedPortal, int searchRadius) {
	}

	private PortalLinker() {
	}

	/** Destination point in the other dimension for an entity standing at (x, y, z). */
	public static Pos destination(double x, int y, double z, boolean toNether, double borderMin, double borderMax) {
		double f = toNether ? 1.0 / SCALE : SCALE;
		double dx = clamp(x * f, borderMin, borderMax);
		double dz = clamp(z * f, borderMin, borderMax);
		return new Pos((int) Math.floor(dx), y, (int) Math.floor(dz));
	}

	public static Result predict(double x, int y, double z, boolean toNether, Collection<Pos> knownPortalBlocksInTargetDim,
			double borderMin, double borderMax) {
		Pos dest = destination(x, y, z, toNether, borderMin, borderMax);
		int radius = toNether ? SEARCH_RADIUS_NETHER : SEARCH_RADIUS_OVERWORLD;
		Optional<Pos> link = knownPortalBlocksInTargetDim.stream()
				.filter(p -> Math.abs(p.x() - dest.x()) <= radius && Math.abs(p.z() - dest.z()) <= radius)
				.min(Comparator.<Pos>comparingLong(p -> p.distSqr(dest)).thenComparingInt(Pos::y));
		return new Result(dest, link, radius);
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}
}
