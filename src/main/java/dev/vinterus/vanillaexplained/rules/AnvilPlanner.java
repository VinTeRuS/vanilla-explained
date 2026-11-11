package dev.vinterus.vanillaexplained.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the cheapest order to combine enchanted books onto an item in an anvil.
 *
 * <p>Vanilla rules (Java Edition): combining target T with sacrifice S costs
 * {@code value(S) + (2^work(T) - 1) + (2^work(S) - 1)} levels, where value(S) is the sum of
 * {@code level * bookMultiplier} over all enchantments carried by S. The result has
 * {@code work = max(work(T), work(S)) + 1}. In survival a single step costing 40 or more is
 * "Too Expensive!".
 *
 * <p>Assumes every book carries different, compatible enchantments (no level merging).
 */
public final class AnvilPlanner {
	public static final int TOO_EXPENSIVE = 40;
	public static final int MAX_BOOKS = 12;

	public record Book(String label, int value, int work) {
		public Book(String label, int value) {
			this(label, value, 0);
		}
	}

	/** One anvil operation: put {@code left} in the first slot, {@code right} in the second. */
	public record Step(List<String> left, List<String> right, int cost) {
	}

	public record Plan(List<Step> steps, int totalCost, int finalWork) {
		public int maxStepCost() {
			return steps.stream().mapToInt(Step::cost).max().orElse(0);
		}
	}

	private record Node(int cost, int work, int leftMask, int leftWork, int rightMask, int rightWork, int stepCost) {
	}

	private AnvilPlanner() {
	}

	public static int penalty(int work) {
		return (1 << work) - 1;
	}

	/**
	 * @param itemLabel label for the target item (slot 0)
	 * @param itemWork  prior work count of the target item (0 for a fresh item)
	 * @param books     books to apply
	 * @param limit     max cost per single step (use {@link #TOO_EXPENSIVE} for survival,
	 *                  {@code Integer.MAX_VALUE} for creative)
	 * @return the cheapest plan, or {@code null} if every order hits the limit
	 */
	public static Plan plan(String itemLabel, int itemWork, List<Book> books, int limit) {
		int n = books.size();
		if (n == 0) {
			return new Plan(List.of(), 0, itemWork);
		}
		if (n > MAX_BOOKS) {
			throw new IllegalArgumentException("Too many books: " + n + " (max " + MAX_BOOKS + ")");
		}
		int count = n + 1;
		int full = (1 << count) - 1;
		int[] value = new int[1 << count];
		for (int mask = 1; mask <= full; mask++) {
			int low = Integer.numberOfTrailingZeros(mask);
			value[mask] = value[mask & (mask - 1)] + (low == 0 ? 0 : books.get(low - 1).value());
		}

		@SuppressWarnings("unchecked")
		Map<Integer, Node>[] best = new Map[1 << count];
		best[1] = Map.of(itemWork, new Node(0, itemWork, 0, 0, 0, 0, 0));
		for (int i = 0; i < n; i++) {
			int w = books.get(i).work();
			best[1 << (i + 1)] = Map.of(w, new Node(0, w, 0, 0, 0, 0, 0));
		}

		for (int mask = 1; mask <= full; mask++) {
			if (Integer.bitCount(mask) < 2) {
				continue;
			}
			// Masks with the item are intermediate item states; masks without it are merged books.
			Map<Integer, Node> out = new HashMap<>();
			boolean hasItem = (mask & 1) != 0;
			for (int left = (mask - 1) & mask; left > 0; left = (left - 1) & mask) {
				int right = mask ^ left;
				if (hasItem && (left & 1) == 0) {
					continue; // the item always stays in the first slot
				}
				Map<Integer, Node> lb = best[left];
				Map<Integer, Node> rb = best[right];
				if (lb == null || rb == null) {
					continue;
				}
				for (Node ln : lb.values()) {
					for (Node rn : rb.values()) {
						int step = value[right] + penalty(ln.work()) + penalty(rn.work());
						if (step >= limit) {
							continue;
						}
						int total = ln.cost() + rn.cost() + step;
						int work = Math.max(ln.work(), rn.work()) + 1;
						Node prev = out.get(work);
						if (prev == null || total < prev.cost()) {
							out.put(work, new Node(total, work, left, ln.work(), right, rn.work(), step));
						}
					}
				}
			}
			best[mask] = out.isEmpty() ? null : out;
		}

		Map<Integer, Node> fin = best[full];
		if (fin == null) {
			return null;
		}
		Node top = null;
		for (Node node : fin.values()) {
			if (top == null || node.cost() < top.cost() || (node.cost() == top.cost() && node.work() < top.work())) {
				top = node;
			}
		}
		List<Step> steps = new ArrayList<>();
		emit(best, full, top, itemLabel, books, steps);
		return new Plan(steps, top.cost(), top.work());
	}

	private static void emit(Map<Integer, Node>[] best, int mask, Node node, String itemLabel, List<Book> books, List<Step> steps) {
		if (Integer.bitCount(mask) < 2) {
			return;
		}
		emit(best, node.leftMask(), best[node.leftMask()].get(node.leftWork()), itemLabel, books, steps);
		emit(best, node.rightMask(), best[node.rightMask()].get(node.rightWork()), itemLabel, books, steps);
		steps.add(new Step(labels(node.leftMask(), itemLabel, books), labels(node.rightMask(), itemLabel, books), node.stepCost()));
	}

	private static List<String> labels(int mask, String itemLabel, List<Book> books) {
		List<String> out = new ArrayList<>();
		if ((mask & 1) != 0) {
			out.add(itemLabel);
		}
		for (int i = 0; i < books.size(); i++) {
			if ((mask & (1 << (i + 1))) != 0) {
				out.add(books.get(i).label());
			}
		}
		return out;
	}
}
