package dev.vinterus.vanillaexplained.rules;

/** Redstone and related formulas taken from the 26.3 code. */
public final class RedstoneMath {
	public static final String[] NOTES = { "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F" };

	private RedstoneMath() {
	}

	/** Mth.lerpDiscrete(alpha, 0, 15) as used by AbstractContainerMenu#getRedstoneSignalFromContainer. */
	public static int signalFromFullness(float fullness) {
		return (int) Math.floor(fullness * 14) + (fullness > 0.0F ? 1 : 0);
	}

	/** Signal a comparator reads from a container holding {@code items} items of one kind. */
	public static int signalFor(int slots, int stackSize, int items) {
		float total = 0.0F;
		int left = items;
		for (int i = 0; i < slots && left > 0; i++) {
			int n = Math.min(stackSize, left);
			total += (float) n / stackSize;
			left -= n;
		}
		return signalFromFullness(total / slots);
	}

	/** Smallest number of items (of one kind) that makes a comparator output at least {@code signal}. */
	public static int itemsForSignal(int slots, int stackSize, int signal) {
		if (signal <= 0) {
			return 0;
		}
		int max = slots * stackSize;
		for (int n = 1; n <= max; n++) {
			if (signalFor(slots, stackSize, n) >= signal) {
				return n;
			}
		}
		return -1;
	}

	/** Comparator output: compare mode passes the rear if it's at least the side; subtract mode subtracts. */
	public static int comparatorOutput(int rear, int side, boolean subtract) {
		return subtract ? Math.max(rear - side, 0) : (rear >= side ? rear : 0);
	}

	public static String noteName(int note) {
		return NOTES[Math.floorMod(note, 12)] + (note < 6 ? "₃" : note < 18 ? "₄" : "₅");
	}

	/** Clicks needed to go from {@code note} to {@code target} (notes wrap 24 → 0). */
	public static int clicksTo(int note, int target) {
		return Math.floorMod(target - note, 25);
	}

	/** Range of the third enchanting slot level for a number of bookshelves: {min, max}. */
	public static int[] topEnchantLevel(int bookshelves) {
		int b = Math.min(15, bookshelves);
		int maxSel = 8 + (b >> 1) + b;
		return new int[] { Math.max(1 + (b >> 1), b * 2), Math.max(maxSel, b * 2) };
	}

	/** Beacon effect radius in blocks for a pyramid of {@code levels}. */
	public static int beaconRange(int levels) {
		return levels * 10 + 10;
	}
}
