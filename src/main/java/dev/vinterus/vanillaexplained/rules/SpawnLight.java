package dev.vinterus.vanillaexplained.rules;

/**
 * Probability that a monster passes Monster#isDarkEnoughToSpawn at one position.
 *
 * <pre>
 * 1. fail if skyLight > random.nextInt(32)
 * 2. fail if blockLightLimit < 15 and blockLight > blockLightLimit
 * 3. pass if maxLocalRawBrightness <= monsterSpawnLightTest.sample()  (uniform [min,max])
 * </pre>
 */
public final class SpawnLight {
	public enum Blocker {
		NONE, BLOCK_LIGHT, BRIGHTNESS
	}

	public record Result(double chance, Blocker blocker) {
	}

	private SpawnLight() {
	}

	public static Result chance(int skyLight, int blockLight, int rawBrightness, int blockLightLimit, int testMin, int testMax) {
		if (blockLightLimit < 15 && blockLight > blockLightLimit) {
			return new Result(0.0, Blocker.BLOCK_LIGHT);
		}
		double skyPass = Math.max(0, 32 - skyLight) / 32.0;
		int span = testMax - testMin + 1;
		int okSamples = Math.max(0, testMax - Math.max(testMin, rawBrightness) + 1);
		double brightPass = Math.min(span, okSamples) / (double) span;
		double p = skyPass * brightPass;
		return new Result(p, p == 0.0 ? Blocker.BRIGHTNESS : Blocker.NONE);
	}
}
