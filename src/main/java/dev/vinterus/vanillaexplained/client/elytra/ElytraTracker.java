package dev.vinterus.vanillaexplained.client.elytra;

import dev.vinterus.vanillaexplained.client.core.Store;
import dev.vinterus.vanillaexplained.rules.ElytraPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Learns how far the player really flies per rocket and how fast, and holds the current
 * flight target set in the companion screen.
 */
public final class ElytraTracker {
	public static final class Stats {
		public double distance;
		public long ticks;
		public int rockets;
	}

	public record Target(int x, int z, String dim) {
	}

	private static final int MIN_ROCKETS_TO_TRUST = 15;
	private static final double MIN_DISTANCE_TO_TRUST = 1500;

	private static Stats stats = Store.load("elytra_stats.json", Stats.class, new Stats());
	private static Target target;
	private static double lastX, lastZ;
	private static boolean wasFlying;
	private static int lastRocketCount = -1;
	private static int saveCountdown;

	private ElytraTracker() {
	}

	public static Target target() {
		return target;
	}

	public static void setTarget(Target t) {
		target = t;
	}

	public static Stats stats() {
		return stats;
	}

	public static void resetStats() {
		stats = new Stats();
		Store.save("elytra_stats.json", stats);
	}

	public static boolean learned() {
		return stats.rockets >= MIN_ROCKETS_TO_TRUST && stats.distance >= MIN_DISTANCE_TO_TRUST;
	}

	public static double blocksPerRocket(int flightDuration) {
		return learned() ? stats.distance / stats.rockets : ElytraPlanner.baselineBlocksPerRocket(flightDuration);
	}

	public static double speedPerTick() {
		return learned() && stats.ticks > 0 ? stats.distance / stats.ticks : ElytraPlanner.cruiseSpeed();
	}

	public static int countRockets(LocalPlayer p) {
		int n = 0;
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(Items.FIREWORK_ROCKET)) {
				n += s.getCount();
			}
		}
		return n;
	}

	/** Flight duration of the first rocket stack found (1 if none). */
	public static int rocketDuration(LocalPlayer p) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.is(Items.FIREWORK_ROCKET)) {
				return dev.vinterus.vanillaexplained.client.core.Mc.fireworkDuration(s);
			}
		}
		return 1;
	}

	public static ItemStack elytra(LocalPlayer p) {
		ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
		return chest.is(Items.ELYTRA) ? chest : ItemStack.EMPTY;
	}

	public static int unbreaking(LocalPlayer p, ItemStack stack) {
		return dev.vinterus.vanillaexplained.client.core.Mc.unbreaking(p, stack);
	}

	public static ElytraPlanner.Estimate estimateTo(LocalPlayer p, double distance) {
		ItemStack el = elytra(p);
		int durLeft = el.isEmpty() ? 0 : el.getMaxDamage() - el.getDamageValue() - 1;
		int unb = el.isEmpty() ? 0 : unbreaking(p, el);
		return ElytraPlanner.estimate(distance, blocksPerRocket(rocketDuration(p)), speedPerTick(), unb, countRockets(p), durLeft);
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			wasFlying = false;
			lastRocketCount = -1;
			return;
		}
		int rockets = countRockets(p);
		boolean flying = p.isFallFlying();
		if (flying && wasFlying) {
			double dx = p.getX() - lastX, dz = p.getZ() - lastZ;
			double step = Math.sqrt(dx * dx + dz * dz);
			if (step < 10) { // ignore teleports
				stats.distance += step;
				stats.ticks++;
			}
			if (lastRocketCount >= 0 && rockets == lastRocketCount - 1 && dev.vinterus.vanillaexplained.client.core.Mc.screen() == null) {
				stats.rockets++;
			}
		}
		if (!flying && wasFlying) {
			Store.save("elytra_stats.json", stats);
		}
		if (flying && ++saveCountdown >= 20 * 60) {
			saveCountdown = 0;
			Store.save("elytra_stats.json", stats);
		}
		if (target != null && dev.vinterus.vanillaexplained.client.core.Mc.dimId(p.level()).equals(target.dim())) {
			double tx = target.x() + 0.5 - p.getX(), tz = target.z() + 0.5 - p.getZ();
			if (tx * tx + tz * tz < 30 * 30) {
				target = null;
			}
		}
		wasFlying = flying;
		lastRocketCount = rockets;
		lastX = p.getX();
		lastZ = p.getZ();
	}
}
