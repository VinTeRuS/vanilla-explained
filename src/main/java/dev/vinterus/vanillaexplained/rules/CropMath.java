package dev.vinterus.vanillaexplained.rules;

/**
 * Crop growth rules from CropBlock#getGrowthSpeed / #randomTick (also used by stems and pitcher).
 */
public final class CropMath {
	public static final int MIN_GROW_LIGHT = 9;
	public static final int MIN_SURVIVE_LIGHT = 8;
	public static final float MAX_SPEED = 10.0F;
	public static final int SECTION_BLOCKS = 4096;

	/** Soil under a crop column: not farmland, dry farmland, or hydrated farmland. */
	public enum Soil {
		NONE, DRY, WET
	}

	public record Penalty(boolean rowBoth, boolean diagonal) {
		public boolean halved() {
			return rowBoth || diagonal;
		}
	}

	private CropMath() {
	}

	/**
	 * @param soil 3x3 soil grid, soil[1][1] is under the crop
	 */
	public static float speed(Soil[][] soil, Penalty penalty) {
		float speed = 1.0F;
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				float s = switch (soil[x][z]) {
					case NONE -> 0.0F;
					case DRY -> 1.0F;
					case WET -> 3.0F;
				};
				if (x != 1 || z != 1) {
					s /= 4.0F;
				}
				speed += s;
			}
		}
		if (penalty.halved()) {
			speed /= 2.0F;
		}
		return speed;
	}

	/** Penalty as computed in vanilla: both axes have the same crop, else any diagonal same crop. */
	public static Penalty penalty(boolean westOrEast, boolean northOrSouth, boolean anyDiagonal) {
		boolean both = westOrEast && northOrSouth;
		return new Penalty(both, !both && anyDiagonal);
	}

	/** The N in "1 in N chance to grow one stage per random tick". */
	public static int oneIn(float speed) {
		return (int) (25.0F / speed) + 1;
	}

	/**
	 * Expected minutes until fully grown.
	 *
	 * @param extraChance 1 for most crops, 2/3 for beetroot and torchflower
	 */
	public static double expectedMinutes(int stagesLeft, float speed, int randomTickSpeed, double extraChance) {
		if (randomTickSpeed <= 0 || stagesLeft <= 0) {
			return stagesLeft <= 0 ? 0 : Double.POSITIVE_INFINITY;
		}
		double perTick = (double) randomTickSpeed / SECTION_BLOCKS / oneIn(speed) * extraChance;
		return stagesLeft / perTick / 20.0 / 60.0;
	}
}
