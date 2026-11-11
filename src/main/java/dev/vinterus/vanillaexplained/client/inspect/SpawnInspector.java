package dev.vinterus.vanillaexplained.client.inspect;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dev.vinterus.vanillaexplained.client.core.Report;
import dev.vinterus.vanillaexplained.client.core.Report.Kind;
import dev.vinterus.vanillaexplained.rules.SpawnLight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

/**
 * Explains whether hostile mobs can spawn on the block the player looks at.
 * Singleplayer runs the real vanilla checks on the integrated server thread; on a server it
 * falls back to what the client can see (light, floor, room) and says so.
 */
public final class SpawnInspector {
	private static final int SAMPLES = 256;

	private SpawnInspector() {
	}

	/** Facts the client can always compute. */
	private record LightFacts(int sky, int block, int raw, SpawnLight.Result light) {
	}

	private static LightFacts light(Level level, BlockPos pos) {
		DimensionType dim = level.dimensionType();
		IntProvider test = dim.monsterSpawnLightTest();
		int sky = level.getBrightness(LightLayer.SKY, pos);
		int block = level.getBrightness(LightLayer.BLOCK, pos);
		int raw = level.isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
		var res = SpawnLight.chance(sky, block, raw, dim.monsterSpawnBlockLightLimit(), dev.vinterus.vanillaexplained.client.core.Mc.intMin(test), dev.vinterus.vanillaexplained.client.core.Mc.intMax(test));
		return new LightFacts(sky, block, raw, res);
	}

	private static void addLight(Report r, Level level, LightFacts f) {
		int limit = level.dimensionType().monsterSpawnBlockLightLimit();
		Kind k = f.light().chance() >= 0.999 ? Kind.OK : f.light().chance() <= 0 ? Kind.BAD : Kind.WARN;
		r.add(k, t("spawn.light", f.block(), f.sky(), percent(f.light().chance())));
		r.brief(k, t("spawn.brief.light", f.block(), f.sky(), percent(f.light().chance())));
		if (f.light().blocker() == SpawnLight.Blocker.BLOCK_LIGHT) {
			r.sub(Kind.INFO, t("spawn.light.block_limit", limit));
		} else if (f.light().blocker() == SpawnLight.Blocker.BRIGHTNESS) {
			r.sub(Kind.INFO, t("spawn.light.too_bright", f.raw(), dev.vinterus.vanillaexplained.client.core.Mc.intMax(level.dimensionType().monsterSpawnLightTest())));
		} else if (f.light().chance() < 0.999 && f.sky() > 0) {
			r.sub(Kind.INFO, t("spawn.light.sky_hint"));
		}
	}

	private static void addDistance(Report r, Vec3 player, BlockPos pos) {
		double d2 = player.distanceToSqr(Vec3.atBottomCenterOf(pos));
		int d = (int) Math.sqrt(d2);
		int despawn = MobCategory.MONSTER.getDespawnDistance();
		if (d2 <= 576.0) {
			r.add(Kind.WARN, t("spawn.distance.too_close", d));
			r.brief(Kind.WARN, t("spawn.brief.close", d));
		} else if (d2 > (double) despawn * despawn) {
			r.add(Kind.WARN, t("spawn.distance.too_far", d, despawn));
			r.brief(Kind.WARN, t("spawn.brief.far", d));
		} else {
			r.add(Kind.OK, t("spawn.distance.ok", d));
		}
	}

	/** Mobs to test when we can't read the biome (multiplayer). */
	private static List<EntityType<?>> defaultMobs(Level level) {
		//? if >=26.2 {
		if (level.dimension() == Level.NETHER) {
			return List.of(net.minecraft.world.entity.EntityTypes.ZOMBIFIED_PIGLIN, net.minecraft.world.entity.EntityTypes.PIGLIN,
					net.minecraft.world.entity.EntityTypes.MAGMA_CUBE, net.minecraft.world.entity.EntityTypes.GHAST,
					net.minecraft.world.entity.EntityTypes.ENDERMAN);
		}
		if (level.dimension() == Level.END) {
			return List.of(net.minecraft.world.entity.EntityTypes.ENDERMAN);
		}
		return List.of(net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.world.entity.EntityTypes.SKELETON,
				net.minecraft.world.entity.EntityTypes.CREEPER, net.minecraft.world.entity.EntityTypes.SPIDER,
				net.minecraft.world.entity.EntityTypes.ENDERMAN, net.minecraft.world.entity.EntityTypes.WITCH);
		//?} else {
		/*if (level.dimension() == Level.NETHER) {
			return List.of(EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN, EntityType.MAGMA_CUBE, EntityType.GHAST, EntityType.ENDERMAN);
		}
		if (level.dimension() == Level.END) {
			return List.of(EntityType.ENDERMAN);
		}
		return List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.ENDERMAN, EntityType.WITCH);
		*///?}
	}

	/** Runs on the integrated server thread. */
	public static Report inspectServer(ServerLevel level, BlockPos pos, Vec3 player) {
		Report r = new Report(t("spawn.title", pos.getX(), pos.getY(), pos.getZ())).icon(icon(level, pos))
				.tip(dev.vinterus.vanillaexplained.client.core.Tips.next("spawn"));
		if (occupied(r, level, pos)) {
			return r;
		}
		Kind blocked = null;
		Component blockedWhy = null;
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			blocked = Kind.BAD;
			blockedWhy = t("spawn.peaceful");
		} else if (!dev.vinterus.vanillaexplained.client.core.Mc.monsterSpawningOn(level)) {
			blocked = Kind.BAD;
			blockedWhy = t("spawn.gamerule_off");
		}
		LightFacts f = light(level, pos);
		addLight(r, level, f);
		addDistance(r, player, pos);

		var state = level.getChunkSource().getLastSpawnState();
		if (state != null) {
			int count = state.getMobCategoryCounts().getInt(MobCategory.MONSTER);
			int cap = MobCategory.MONSTER.getMaxInstancesPerChunk() * state.getSpawnableChunkCount() / 289;
			r.add(count < cap ? Kind.OK : Kind.BAD, t("spawn.mobcap", count, cap));
			if (count >= cap) {
				r.brief(Kind.BAD, t("spawn.brief.cap", count, cap));
				r.sub(Kind.INFO, t("spawn.mobcap.full_hint"));
			}
		}

		Set<EntityType<?>> types = new LinkedHashSet<>(dev.vinterus.vanillaexplained.client.core.Mc.monstersAt(level, pos));
		if (types.isEmpty()) {
			r.forceVerdict(Kind.BAD, t("spawn.no_monsters_here"));
			return r;
		}
		r.header(t("spawn.mobs_header"));
		boolean any = addMobs(r, level, pos, types, true);
		verdict(r, any, player.distanceToSqr(Vec3.atBottomCenterOf(pos)) <= 576.0, blocked, blockedWhy);
		return r;
	}

	private static ItemStack icon(Level level, BlockPos pos) {
		var item = level.getBlockState(pos.below()).getBlock().asItem();
		return item == Items.AIR ? new ItemStack(Items.ZOMBIE_HEAD) : new ItemStack(item);
	}

	/** Client-only approximation for multiplayer. */
	public static Report inspectClient(Level level, BlockPos pos, Vec3 player) {
		Report r = new Report(t("spawn.title", pos.getX(), pos.getY(), pos.getZ())).icon(icon(level, pos))
				.tip(dev.vinterus.vanillaexplained.client.core.Tips.next("spawn"));
		if (occupied(r, level, pos)) {
			return r;
		}
		LightFacts f = light(level, pos);
		addLight(r, level, f);
		addDistance(r, player, pos);
		r.header(t("spawn.mobs_header_typical"));
		boolean any = addMobs(r, level, pos, defaultMobs(level), false) && f.light().chance() > 0;
		boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
		verdict(r, any, player.distanceToSqr(Vec3.atBottomCenterOf(pos)) <= 576.0, peaceful ? Kind.BAD : null,
				peaceful ? t("spawn.peaceful") : null);
		r.add(Kind.INFO, t("spawn.multiplayer_note"));
		return r;
	}

	/** One mob's verdict; mobs with equal {@code group} are shown together. */
	private record MobResult(Kind kind, Component text, String group, boolean canSpawn) {
	}

	private static ItemStack egg(EntityType<?> type) {
		return dev.vinterus.vanillaexplained.client.core.Mc.spawnEgg(type);
	}

	/** Adds the mob list grouped by reason and sets the verdict. Returns true if any mob can spawn. */
	private static boolean addMobs(Report r, Level level, BlockPos pos, Iterable<EntityType<?>> types, boolean server) {
		Map<String, MobResult> groups = new LinkedHashMap<>();
		Map<String, List<ItemStack>> icons = new LinkedHashMap<>();
		boolean any = false;
		for (EntityType<?> type : types) {
			MobResult m = explainMob(level, pos, type, server, light(level, pos).light().chance());
			any |= m.canSpawn();
			groups.putIfAbsent(m.group(), m);
			icons.computeIfAbsent(m.group(), k -> new ArrayList<>()).add(egg(type));
		}
		// can-spawn groups first
		var sorted = groups.values().stream().sorted((x, y) -> Boolean.compare(y.canSpawn(), x.canSpawn())).toList();
		sorted.forEach(m -> r.icons(m.kind(), icons.get(m.group()), m.text()));
		if (!sorted.isEmpty()) {
			MobResult first = sorted.get(0);
			r.briefIcons(first.kind(), icons.get(first.group()), first.text());
		}
		return any;
	}

	private static MobResult explainMob(Level level, BlockPos pos, EntityType<?> type, boolean server, double lightChance) {
		if (!dev.vinterus.vanillaexplained.client.core.Mc.spawnPositionOk(type, level, pos)) {
			var below = level.getBlockState(pos.below()).getBlock().getName();
			return new MobResult(Kind.BAD, t("spawn.group.floor", below), "floor", false);
		}
		if (!level.noCollision(type.getDimensions().makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
			String size = fmt(type.getWidth()) + "×" + fmt(type.getHeight());
			return new MobResult(Kind.BAD, t("spawn.group.room", size), "room" + size, false);
		}
		if (server && level instanceof ServerLevel sl) {
			int ok = 0;
			for (int i = 0; i < SAMPLES; i++) {
				try {
					if (dev.vinterus.vanillaexplained.client.core.Mc.checkSpawnRules(type, sl, pos, RandomSource.create(i * 7919L + pos.asLong()))) {
						ok++;
					}
				} catch (RuntimeException e) {
					ok = -1;
					break;
				}
			}
			if (ok < 0) {
				return new MobResult(Kind.WARN, t("spawn.group.unknown"), "unknown", false);
			} else if (ok == 0) {
				if (lightChance <= 0) {
					return new MobResult(Kind.BAD, t("spawn.group.light"), "light", false);
				}
				return new MobResult(Kind.BAD, t("spawn.group.rules"), "rules", false);
			}
			String pct = percent(ok / (double) SAMPLES);
			return new MobResult(Kind.OK, t("spawn.group.can", pct), "can" + pct, true);
		}
		return new MobResult(Kind.OK, t("spawn.group.can_floor"), "can", true);
	}

	/** Verdict and early exit when the spot itself is filled. Returns true if blocked. */
	private static boolean occupied(Report r, Level level, BlockPos pos) {
		var st = level.getBlockState(pos);
		if (!st.getCollisionShape(level, pos).isEmpty() || !st.getFluidState().isEmpty()) {
			r.forceVerdict(Kind.BAD, t("spawn.verdict.occupied", st.getBlock().getName()));
			return true;
		}
		return false;
	}

	private static void verdict(Report r, boolean any, boolean tooClose, Kind blocked, Component blockedWhy) {
		if (blocked != null) {
			r.forceVerdict(Kind.BAD, blockedWhy);
		} else if (any) {
			r.forceVerdict(Kind.OK, t("spawn.verdict.can"));
		} else {
			r.forceVerdict(Kind.BAD, t("spawn.verdict.cant"));
		}
	}

	static String percent(double p) {
		if (p >= 0.999) {
			return "100%";
		}
		if (p > 0 && p < 0.01) {
			return "<1%";
		}
		return Math.round(p * 100) + "%";
	}

	private static String fmt(float v) {
		return v == (int) v ? Integer.toString((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
	}
}
