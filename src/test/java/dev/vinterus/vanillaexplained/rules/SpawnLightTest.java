package dev.vinterus.vanillaexplained.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

class SpawnLightTest {
	@Test
	void darkCaveAlwaysPasses() {
		assertEquals(1.0, SpawnLight.chance(0, 0, 0, 0, 0, 7).chance());
	}

	@Test
	void torchBlocksInOverworld() {
		var r = SpawnLight.chance(0, 1, 1, 0, 0, 7);
		assertEquals(0.0, r.chance());
		assertEquals(SpawnLight.Blocker.BLOCK_LIGHT, r.blocker());
	}

	@Test
	void netherAllowsLightUpTo7() {
		// Nether: block light limit 15, light test constant 7
		assertEquals(1.0, SpawnLight.chance(0, 7, 7, 15, 7, 7).chance());
		assertEquals(0.0, SpawnLight.chance(0, 8, 8, 15, 7, 7).chance());
	}

	@Test
	void matchesMonteCarlo() {
		Random rnd = new Random(1);
		for (int sky = 0; sky <= 15; sky += 3) {
			for (int bright = 0; bright <= 8; bright++) {
				int hits = 0, n = 200_000;
				for (int i = 0; i < n; i++) {
					if (sky > rnd.nextInt(32)) continue;
					if (bright <= rnd.nextInt(8)) hits++;
				}
				assertEquals(hits / (double) n, SpawnLight.chance(sky, 0, bright, 0, 0, 7).chance(), 0.01);
			}
		}
	}
}
