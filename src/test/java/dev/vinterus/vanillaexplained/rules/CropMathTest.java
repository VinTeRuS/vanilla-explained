package dev.vinterus.vanillaexplained.rules;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import dev.vinterus.vanillaexplained.rules.CropMath.Soil;

class CropMathTest {
	static Soil[][] all(Soil s) {
		return new Soil[][] { { s, s, s }, { s, s, s }, { s, s, s } };
	}

	@Test
	void bestCaseIsTen() {
		float s = CropMath.speed(all(Soil.WET), CropMath.penalty(false, false, false));
		assertEquals(10.0F, s);
		assertEquals(3, CropMath.oneIn(s));
	}

	@Test
	void rowsInBothDirectionsHalve() {
		float s = CropMath.speed(all(Soil.WET), CropMath.penalty(true, true, false));
		assertEquals(5.0F, s);
		assertEquals(6, CropMath.oneIn(s));
		// a single-axis row is fine, but a diagonal neighbour still halves it
		assertEquals(10.0F, CropMath.speed(all(Soil.WET), CropMath.penalty(true, false, false)));
		assertEquals(5.0F, CropMath.speed(all(Soil.WET), CropMath.penalty(true, false, true)));
	}

	@Test
	void lonelyDryCrop() {
		Soil[][] g = all(Soil.NONE);
		g[1][1] = Soil.DRY;
		float s = CropMath.speed(g, CropMath.penalty(false, false, false));
		assertEquals(2.0F, s);
		assertEquals(13, CropMath.oneIn(s));
	}

	@Test
	void wheatTakesAbout24MinutesAtBest() {
		// 7 stages, 1/3 per random tick, 3/4096 random ticks per game tick -> ~24 min
		double m = CropMath.expectedMinutes(7, 10.0F, 3, 1.0);
		assertEquals(7 * 3 * 4096 / 3.0 / 20 / 60, m, 1e-9);
		assertTrue(m > 23 && m < 25);
	}
}
