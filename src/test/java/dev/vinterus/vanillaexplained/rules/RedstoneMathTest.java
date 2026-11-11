package dev.vinterus.vanillaexplained.rules;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RedstoneMathTest {
	@Test
	void knownChestTable() {
		// Minecraft Wiki comparator table for a 27-slot chest with 64-stack items
		assertEquals(1, RedstoneMath.itemsForSignal(27, 64, 1));
		assertEquals(124, RedstoneMath.itemsForSignal(27, 64, 2));
		assertEquals(1728, RedstoneMath.itemsForSignal(27, 64, 15));
		// hopper: 5 slots
		assertEquals(23, RedstoneMath.itemsForSignal(5, 64, 2));
		assertEquals(320, RedstoneMath.itemsForSignal(5, 64, 15));
		for (int s = 1; s <= 15; s++) {
			int n = RedstoneMath.itemsForSignal(27, 64, s);
			assertEquals(s, RedstoneMath.signalFor(27, 64, n));
			if (n > 1) {
				assertTrue(RedstoneMath.signalFor(27, 64, n - 1) < s);
			}
		}
	}

	@Test
	void comparatorModes() {
		assertEquals(10, RedstoneMath.comparatorOutput(10, 5, false));
		assertEquals(0, RedstoneMath.comparatorOutput(4, 5, false));
		assertEquals(5, RedstoneMath.comparatorOutput(10, 5, true));
		assertEquals(0, RedstoneMath.comparatorOutput(3, 5, true));
	}

	@Test
	void notes() {
		assertEquals("F#₃", RedstoneMath.noteName(0));
		assertEquals("C₄", RedstoneMath.noteName(6));
		assertEquals("F#₅", RedstoneMath.noteName(24));
		assertEquals(24, RedstoneMath.clicksTo(1, 0));
	}

	@Test
	void enchantingAndBeacon() {
		assertArrayEquals(new int[] { 30, 30 }, RedstoneMath.topEnchantLevel(15));
		assertArrayEquals(new int[] { 30, 30 }, RedstoneMath.topEnchantLevel(20));
		assertArrayEquals(new int[] { 1, 8 }, RedstoneMath.topEnchantLevel(0));
		assertEquals(50, RedstoneMath.beaconRange(4));
	}
}
