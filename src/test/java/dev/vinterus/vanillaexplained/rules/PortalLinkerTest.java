package dev.vinterus.vanillaexplained.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vinterus.vanillaexplained.rules.PortalLinker.Pos;

class PortalLinkerTest {
	static final double B = 29_999_984;

	@Test
	void scalesCoordinates() {
		assertEquals(new Pos(12, 64, -13), PortalLinker.destination(100.5, 64, -100.5, true, -B, B));
		assertEquals(new Pos(800, 70, -804), PortalLinker.destination(100, 70, -100.5, false, -B, B));
	}

	@Test
	void picksNearestInSquareRadius() {
		var portals = List.of(new Pos(30, 64, 12), new Pos(10, 100, 10), new Pos(12, 70, 14));
		var r = PortalLinker.predict(100, 64, 100, true, portals, -B, B);
		assertEquals(new Pos(12, 64, 12), r.destination());
		assertEquals(new Pos(12, 70, 14), r.linkedPortal().orElseThrow());
	}

	@Test
	void noPortalOutsideRadius() {
		var r = PortalLinker.predict(100, 64, 100, true, List.of(new Pos(29, 64, 12)), -B, B);
		assertTrue(r.linkedPortal().isEmpty());
		var ow = PortalLinker.predict(10, 64, 10, false, List.of(new Pos(80 + 128, 64, 80)), -B, B);
		assertTrue(ow.linkedPortal().isPresent());
	}

	@Test
	void tieBreaksByLowerY() {
		var r = PortalLinker.predict(80, 64, 80, true, List.of(new Pos(10, 66, 10), new Pos(10, 62, 10)), -B, B);
		assertEquals(62, r.linkedPortal().orElseThrow().y());
	}
}
