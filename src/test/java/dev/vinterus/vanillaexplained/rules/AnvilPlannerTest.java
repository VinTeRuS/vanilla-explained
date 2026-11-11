package dev.vinterus.vanillaexplained.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class AnvilPlannerTest {
	private record S(boolean item, int value, int work, int cost) {
	}

	/** Exhaustive search over every possible merge order, used as an oracle. */
	private static int brute(List<S> pool, int limit) {
		if (pool.size() == 1) {
			return pool.get(0).item() ? pool.get(0).cost() : Integer.MAX_VALUE;
		}
		int best = Integer.MAX_VALUE;
		for (int i = 0; i < pool.size(); i++) {
			for (int j = 0; j < pool.size(); j++) {
				if (i == j || pool.get(j).item()) {
					continue;
				}
				S l = pool.get(i), r = pool.get(j);
				int step = r.value() + AnvilPlanner.penalty(l.work()) + AnvilPlanner.penalty(r.work());
				if (step >= limit) {
					continue;
				}
				List<S> next = new ArrayList<>();
				for (int k = 0; k < pool.size(); k++) {
					if (k != i && k != j) {
						next.add(pool.get(k));
					}
				}
				next.add(new S(l.item(), l.value() + r.value(), Math.max(l.work(), r.work()) + 1, l.cost() + r.cost() + step));
				best = Math.min(best, brute(next, limit));
			}
		}
		return best;
	}

	@Test
	void singleBook() {
		var plan = AnvilPlanner.plan("Sword", 0, List.of(new AnvilPlanner.Book("Sharpness V", 5)), 40);
		assertEquals(5, plan.totalCost());
		assertEquals(1, plan.finalWork());
		assertEquals(1, plan.steps().size());
	}

	@Test
	void priorWorkIsCharged() {
		var plan = AnvilPlanner.plan("Sword", 3, List.of(new AnvilPlanner.Book("Mending", 2)), 40);
		assertEquals(2 + 7, plan.totalCost());
	}

	@Test
	void tooExpensiveReturnsNull() {
		var plan = AnvilPlanner.plan("Sword", 6, List.of(new AnvilPlanner.Book("Mending", 2)), 40);
		assertNull(plan); // 2 + 63 >= 40
		assertNotNull(AnvilPlanner.plan("Sword", 6, List.of(new AnvilPlanner.Book("Mending", 2)), Integer.MAX_VALUE));
	}

	@Test
	void matchesBruteForce() {
		Random rnd = new Random(42);
		for (int trial = 0; trial < 300; trial++) {
			int n = 1 + rnd.nextInt(5);
			int itemWork = rnd.nextInt(3);
			List<AnvilPlanner.Book> books = new ArrayList<>();
			List<S> pool = new ArrayList<>();
			pool.add(new S(true, 0, itemWork, 0));
			for (int i = 0; i < n; i++) {
				int v = 1 + rnd.nextInt(10);
				books.add(new AnvilPlanner.Book("b" + i, v));
				pool.add(new S(false, v, 0, 0));
			}
			int limit = rnd.nextBoolean() ? 40 : Integer.MAX_VALUE;
			int expected = brute(pool, limit);
			var plan = AnvilPlanner.plan("item", itemWork, books, limit);
			if (expected == Integer.MAX_VALUE) {
				assertNull(plan, "trial " + trial);
			} else {
				assertNotNull(plan, "trial " + trial);
				assertEquals(expected, plan.totalCost(), "trial " + trial);
				assertEquals(n, plan.steps().size());
				assertTrue(plan.maxStepCost() < limit);
				assertEquals(plan.totalCost(), plan.steps().stream().mapToInt(AnvilPlanner.Step::cost).sum());
			}
		}
	}
}
