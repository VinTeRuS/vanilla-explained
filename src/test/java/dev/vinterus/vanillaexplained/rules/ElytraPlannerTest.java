package dev.vinterus.vanillaexplained.rules;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ElytraPlannerTest {
	@Test
	void cruiseSpeedMatchesKnownRocketSpeed() {
		// Community measurements: rocket-boosted level flight is ~33.5 m/s (~1.67 blocks/tick).
		double v = ElytraPlanner.cruiseSpeed();
		System.out.printf("level pitch %.2f deg, cruise %.3f b/t, flight1 %.1f b/rocket, flight3 %.1f b/rocket%n",
				ElytraPlanner.levelPitch(), v, ElytraPlanner.baselineBlocksPerRocket(1), ElytraPlanner.baselineBlocksPerRocket(3));
		assertTrue(v > 1.5 && v < 1.8, "cruise " + v);
		assertTrue(Math.abs(ElytraPlanner.simulate(ElytraPlanner.levelPitch(), 2000, 0).vertical()) < 5);
	}

	@Test
	void unbreakingIIIQuartersDamage() {
		assertEquals(0.25, ElytraPlanner.damageChance(3), 1e-9);
		var e = ElytraPlanner.estimate(10_000, 50, 1.6, 3, 300, 432);
		assertEquals(200, e.rockets());
		assertTrue(e.enoughRockets());
		// 10000/1.6 = 6250 ticks = 313 durability ticks, x0.25
		assertEquals(Math.ceil(6250.0 / 20) * 0.25, e.durabilityUsed(), 1e-9);
	}

	@Test
	void notEnoughRockets() {
		assertFalse(ElytraPlanner.estimate(5000, 50, 1.6, 0, 10, 432).enoughRockets());
		assertFalse(ElytraPlanner.estimate(50000, 50, 1.6, 0, 9999, 432).enoughDurability());
	}
}
