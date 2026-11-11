package dev.vinterus.vanillaexplained.client.inspect;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import java.util.Locale;

import dev.vinterus.vanillaexplained.client.core.Report;
import dev.vinterus.vanillaexplained.client.core.Report.Kind;
import dev.vinterus.vanillaexplained.rules.CropMath;
import dev.vinterus.vanillaexplained.rules.CropMath.Soil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Explains why a crop grows fast or slow. Works the same in singleplayer and on servers. */
public final class CropInspector {
	private CropInspector() {
	}

	/** Returns the crop position for a looked-at block (the crop itself or the farmland under it), or null. */
	public static BlockPos cropAt(Level level, BlockPos hit) {
		if (isCrop(level.getBlockState(hit))) {
			return hit;
		}
		if (dev.vinterus.vanillaexplained.client.core.Mc.isFarmland(level.getBlockState(hit)) && isCrop(level.getBlockState(hit.above()))) {
			return hit.above();
		}
		return null;
	}

	private static boolean isCrop(BlockState s) {
		return s.getBlock() instanceof CropBlock || s.getBlock() instanceof StemBlock;
	}

	private static Soil soil(BlockState s) {
		if (!dev.vinterus.vanillaexplained.client.core.Mc.isFarmland(s)) {
			return Soil.NONE;
		}
		return dev.vinterus.vanillaexplained.client.core.Mc.farmlandMoisture(s) > 0 ? Soil.WET : Soil.DRY;
	}

	/**
	 * @param randomTickSpeed the game rule value, or -1 if unknown (multiplayer: assume 3)
	 */
	public static Report inspect(Level level, BlockPos pos, int randomTickSpeed) {
		BlockState state = level.getBlockState(pos);
		Block type = state.getBlock();
		Report r = new Report(t("crop.title", type.getName())).icon(new net.minecraft.world.item.ItemStack(type.asItem()))
				.tip(dev.vinterus.vanillaexplained.client.core.Tips.next("crop"));

		int age, maxAge;
		if (type instanceof CropBlock crop) {
			age = crop.getAge(state);
			maxAge = crop.getMaxAge();
		} else {
			age = state.getValue(StemBlock.AGE);
			maxAge = StemBlock.MAX_AGE;
		}
		if (age >= maxAge) {
			r.verdict(Kind.OK, type instanceof StemBlock ? t("crop.stem_grown") : t("crop.grown"));
			if (!(type instanceof StemBlock)) {
				return r;
			}
		} else {
			r.add(Kind.INFO, t("crop.stage", age, maxAge));
		}

		int light = level.getRawBrightness(pos, 0);
		if (light < CropMath.MIN_GROW_LIGHT) {
			r.verdict(Kind.BAD, t("crop.verdict.dark"));
			r.add(Kind.BAD, t("crop.light.dark", light, CropMath.MIN_GROW_LIGHT));
			r.brief(Kind.BAD, t("crop.brief.light", light));
			if (light < CropMath.MIN_SURVIVE_LIGHT && !level.canSeeSky(pos)) {
				r.sub(Kind.WARN, t("crop.light.will_break"));
			}
		} else {
			r.add(Kind.OK, t("crop.light.ok", light));
		}

		BlockPos below = pos.below();
		Soil[][] grid = new Soil[3][3];
		int dryNeighbours = 0, missingNeighbours = 0;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				Soil s = soil(level.getBlockState(below.offset(x, 0, z)));
				grid[x + 1][z + 1] = s;
				if (x != 0 || z != 0) {
					if (s == Soil.DRY) dryNeighbours++;
					if (s == Soil.NONE) missingNeighbours++;
				}
			}
		}
		Soil center = grid[1][1];
		if (center == Soil.WET) {
			r.add(Kind.OK, t("crop.soil.wet"));
		} else if (center == Soil.DRY) {
			r.add(Kind.BAD, t("crop.soil.dry"));
			r.brief(Kind.BAD, t("crop.brief.dry"));
			r.sub(Kind.INFO, t("crop.soil.water_hint"));
		}
		if (dryNeighbours > 0) {
			r.add(Kind.WARN, t("crop.neighbours.dry", dryNeighbours));
		}
		if (missingNeighbours > 0) {
			r.add(Kind.INFO, t("crop.neighbours.missing", missingNeighbours));
		}

		boolean we = level.getBlockState(pos.west()).is(type) || level.getBlockState(pos.east()).is(type);
		boolean ns = level.getBlockState(pos.north()).is(type) || level.getBlockState(pos.south()).is(type);
		boolean diag = level.getBlockState(pos.west().north()).is(type) || level.getBlockState(pos.east().north()).is(type)
				|| level.getBlockState(pos.east().south()).is(type) || level.getBlockState(pos.west().south()).is(type);
		CropMath.Penalty penalty = CropMath.penalty(we, ns, diag);
		if (penalty.halved()) {
			r.brief(Kind.BAD, t("crop.brief.penalty"));
		}
		if (penalty.rowBoth()) {
			r.add(Kind.BAD, t("crop.penalty.both"));
			r.sub(Kind.INFO, t("crop.penalty.hint"));
		} else if (penalty.diagonal()) {
			r.add(Kind.BAD, t("crop.penalty.diagonal"));
			r.sub(Kind.INFO, t("crop.penalty.hint"));
		} else {
			r.add(Kind.OK, t("crop.penalty.none"));
		}

		float speed = CropMath.speed(grid, penalty);
		int oneIn = CropMath.oneIn(speed);
		Kind sk = oneIn <= 3 ? Kind.OK : oneIn <= 6 ? Kind.WARN : Kind.BAD;
		r.verdict(sk, t(oneIn <= 3 ? "crop.verdict.full" : oneIn <= 6 ? "crop.verdict.half" : "crop.verdict.slow"));
		r.add(sk, t("crop.speed", String.format(Locale.ROOT, "%.2f", speed), oneIn));

		if (age < maxAge && light >= CropMath.MIN_GROW_LIGHT) {
			int rts = randomTickSpeed < 0 ? 3 : randomTickSpeed;
			double extra = (type instanceof BeetrootBlock || type instanceof TorchflowerCropBlock) ? 2.0 / 3.0 : 1.0;
			double minutes = CropMath.expectedMinutes(maxAge - age, speed, rts, extra);
			if (Double.isInfinite(minutes)) {
				r.add(Kind.BAD, t("crop.eta.never"));
			} else {
				r.add(Kind.INFO, t(randomTickSpeed < 0 ? "crop.eta.assumed" : "crop.eta", Math.max(1, Math.round(minutes))));
				r.brief(Kind.INFO, t("crop.brief.eta", oneIn, Math.max(1, Math.round(minutes))));
			}
		}
		return r;
	}
}
