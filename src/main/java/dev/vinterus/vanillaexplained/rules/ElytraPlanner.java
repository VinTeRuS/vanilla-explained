package dev.vinterus.vanillaexplained.rules;

/**
 * Estimates rockets, time and elytra durability for a trip, using the vanilla elytra and
 * firework boost physics (LivingEntity#updateFallFlyingMovement, FireworkRocketEntity#tick).
 *
 * <p>The baseline strategy is "keep boosting": a new rocket is fired the moment the previous one
 * burns out, at the pitch that keeps the player level. Players who glide between rockets use
 * fewer, so the client also learns a personal blocks-per-rocket rate from real flights.
 */
public final class ElytraPlanner {
	public static final double GRAVITY = 0.08;
	public static final int TICKS_PER_DURABILITY = 20;

	public record Sim(double horizontal, double vertical, int ticks) {
		public double speed() {
			return horizontal / ticks;
		}
	}

	public record Estimate(double distance, int rockets, double seconds, double durabilityUsed,
			double blocksPerRocket, boolean enoughRockets, boolean enoughDurability) {
	}

	private ElytraPlanner() {
	}

	/** Average rocket burn time in ticks: 10 * (1 + flight) + U[0,5] + U[0,6]. */
	public static double meanRocketTicks(int flightDuration) {
		return 10.0 * (1 + flightDuration) + 2.5 + 3.0;
	}

	/** Chance that one second of flight costs 1 durability. Elytra uses the non-armor Unbreaking formula. */
	public static double damageChance(int unbreakingLevel) {
		return 1.0 / (unbreakingLevel + 1);
	}

	/**
	 * Simulates flight in the vertical plane with continuous boosting.
	 *
	 * @param pitchDeg vanilla xRot: negative looks up, positive looks down
	 */
	public static Sim simulate(double pitchDeg, int ticks, double startSpeed) {
		double lean = Math.toRadians(pitchDeg);
		// Look vector in the (forward, up) plane, as Entity#calculateViewVector with yaw 0.
		double lookF = Math.cos(lean);
		double lookY = -Math.sin(lean);
		double vf = startSpeed, vy = 0.0;
		double x = 0.0, y = 0.0;
		for (int t = 0; t < ticks; t++) {
			// firework boost
			vf += lookF * 0.1 + (lookF * 1.5 - vf) * 0.5;
			vy += lookY * 0.1 + (lookY * 1.5 - vy) * 0.5;
			// elytra movement
			double lookHor = Math.abs(lookF);
			double moveHor = Math.abs(vf);
			double lift = Math.cos(lean) * Math.cos(lean);
			vy += GRAVITY * (-1.0 + lift * 0.75);
			if (vy < 0.0 && lookHor > 0.0) {
				double convert = vy * -0.1 * lift;
				vf += lookF * convert / lookHor;
				vy += convert;
			}
			if (lean < 0.0 && lookHor > 0.0) {
				double convert = moveHor * -Math.sin(lean) * 0.04;
				vf += -lookF * convert / lookHor;
				vy += convert * 3.2;
			}
			if (lookHor > 0.0) {
				vf += (lookF / lookHor * moveHor - vf) * 0.1;
			}
			vf *= 0.99;
			vy *= 0.98;
			x += vf;
			y += vy;
		}
		return new Sim(x, y, ticks);
	}

	/** Pitch (degrees) at which continuous boosting keeps altitude, found by bisection. */
	public static double levelPitch() {
		double lo = -45.0, hi = 20.0; // lo climbs, hi sinks
		for (int i = 0; i < 50; i++) {
			double mid = (lo + hi) / 2;
			if (simulate(mid, 2000, 0.0).vertical() > 0) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		return (lo + hi) / 2;
	}

	/** Blocks per tick when boosting level. */
	public static double cruiseSpeed() {
		return simulate(levelPitch(), 2000, 0.0).speed();
	}

	/** Baseline blocks per rocket for continuous boosting. */
	public static double baselineBlocksPerRocket(int flightDuration) {
		return cruiseSpeed() * meanRocketTicks(flightDuration);
	}

	/**
	 * @param blocksPerRocket learned rate, or baseline from {@link #baselineBlocksPerRocket}
	 * @param speedPerTick    learned average speed, or {@link #cruiseSpeed()}
	 */
	public static Estimate estimate(double distance, double blocksPerRocket, double speedPerTick, int unbreaking,
			int rocketsAvailable, int durabilityLeft) {
		int rockets = (int) Math.ceil(distance / blocksPerRocket);
		double ticks = distance / speedPerTick;
		double durability = Math.ceil(ticks / TICKS_PER_DURABILITY) * damageChance(unbreaking);
		return new Estimate(distance, rockets, ticks / 20.0, durability, blocksPerRocket,
				rocketsAvailable >= rockets, durabilityLeft > durability);
	}
}
