package dev.vinterus.vanillaexplained.client.redstone;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import dev.vinterus.vanillaexplained.client.core.Report;
import dev.vinterus.vanillaexplained.client.core.Report.Kind;
import dev.vinterus.vanillaexplained.client.mixin.DiodeBlockInvoker;
import dev.vinterus.vanillaexplained.rules.RedstoneMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

/**
 * Explains redstone components, comparator readings, enchanting tables and beacons.
 * Pass the server level in singleplayer (exact container contents), the client level otherwise.
 */
public final class RedstoneInspector {
	private RedstoneInspector() {
	}

	public static boolean handles(BlockState s) {
		Block b = s.getBlock();
		return b == Blocks.REDSTONE_WIRE || b instanceof DiodeBlock || b instanceof ObserverBlock || b instanceof PistonBaseBlock
				|| b instanceof HopperBlock || b instanceof NoteBlock || b instanceof PoweredRailBlock || b instanceof DaylightDetectorBlock
				|| b instanceof RedstoneLampBlock || b instanceof DispenserBlock || b instanceof TargetBlock || b instanceof LeverBlock
				|| b instanceof ButtonBlock || b instanceof RedstoneTorchBlock || isCopperBulb(b)
				|| b instanceof EnchantingTableBlock || b instanceof BeaconBlock || s.hasAnalogOutputSignal();
	}

	private static boolean isCopperBulb(Block b) {
		//? if >=1.20.3 {
		return b instanceof net.minecraft.world.level.block.CopperBulbBlock;
		//?} else {
		/*return false;
		*///?}
	}

	private static Component dir(Direction d) {
		return t("dir." + d.getName());
	}

	public static Report inspect(Level level, BlockPos pos, boolean exact) {
		BlockState s = level.getBlockState(pos);
		Block b = s.getBlock();
		Report r = new Report(t("rs.title", b.getName())).icon(new net.minecraft.world.item.ItemStack(b.asItem()));
		r.tip(dev.vinterus.vanillaexplained.client.core.Tips.next(b instanceof EnchantingTableBlock ? "ench" : b instanceof BeaconBlock ? "beacon" : "rs"));

		if (b instanceof EnchantingTableBlock) {
			enchanting(r, level, pos);
			return r;
		}
		if (b instanceof BeaconBlock) {
			beacon(r, level, pos);
			return r;
		}

		if (b == Blocks.REDSTONE_WIRE) {
			wire(r, level, pos, s);
		} else if (b instanceof ComparatorBlock) {
			comparator(r, level, pos, s, exact);
		} else if (b instanceof RepeaterBlock) {
			repeater(r, level, pos, s);
		} else if (b instanceof ObserverBlock) {
			Direction f = s.getValue(DirectionalBlock.FACING);
			r.add(Kind.INFO, t("rs.observer.watches", dir(f), level.getBlockState(pos.relative(f)).getBlock().getName()));
			r.verdict(Kind.OK, t("rs.brief.watches", level.getBlockState(pos.relative(f)).getBlock().getName()));
			r.brief(Kind.INFO, t("rs.brief.pulse"));
			r.add(Kind.INFO, t("rs.observer.pulse", dir(f.getOpposite())));
			r.add(s.getValue(ObserverBlock.POWERED) ? Kind.OK : Kind.INFO, t(s.getValue(ObserverBlock.POWERED) ? "rs.on" : "rs.off"));
		} else if (b instanceof PistonBaseBlock) {
			piston(r, level, pos, s);
		} else if (b instanceof HopperBlock) {
			boolean enabled = s.getValue(HopperBlock.ENABLED);
			Direction f = s.getValue(HopperBlock.FACING);
			r.verdict(enabled ? Kind.OK : Kind.BAD, t(enabled ? "rs.hopper.on" : "rs.hopper.locked"));
			r.add(Kind.INFO, t("rs.hopper.out", dir(f), level.getBlockState(pos.relative(f)).getBlock().getName()));
			r.brief(Kind.INFO, t("rs.brief.hopper", level.getBlockState(pos.relative(f)).getBlock().getName()));
			r.add(Kind.INFO, t("rs.hopper.rate"));
		} else if (b instanceof NoteBlock) {
			int note = s.getValue(NoteBlock.NOTE);
			var inst = s.getValue(NoteBlock.INSTRUMENT);
			r.verdict(Kind.OK, t("rs.note.note", RedstoneMath.noteName(note), note));
			r.add(Kind.INFO, t("rs.note.instrument", inst.getSerializedName(), level.getBlockState(pos.below()).getBlock().getName()));
			r.brief(Kind.INFO, t("rs.brief.note", inst.getSerializedName(), RedstoneMath.noteName((note + 1) % 25)));
			r.add(Kind.INFO, t("rs.note.next", RedstoneMath.noteName((note + 1) % 25)));
			if (!level.getBlockState(pos.above()).isAir() && inst.worksAboveNoteBlock()) {
				r.add(Kind.BAD, t("rs.note.blocked"));
			}
		} else if (b instanceof PoweredRailBlock) {
			boolean on = s.getValue(PoweredRailBlock.POWERED);
			r.add(on ? Kind.OK : Kind.BAD, t(on ? "rs.on" : "rs.off"));
			r.add(Kind.INFO, t("rs.rail.range"));
		} else if (b instanceof DaylightDetectorBlock) {
			r.add(Kind.INFO, t("rs.daylight", s.getValue(DaylightDetectorBlock.POWER),
					t(s.getValue(DaylightDetectorBlock.INVERTED) ? "rs.daylight.night" : "rs.daylight.day")));
		} else if (b instanceof RedstoneTorchBlock) {
			r.add(Kind.INFO, t("rs.torch"));
		} else if (isCopperBulb(b)) {
			r.add(Kind.INFO, t("rs.bulb"));
		}

		if (!(b == Blocks.REDSTONE_WIRE) && !(b instanceof DiodeBlock) && !(b instanceof PistonBaseBlock)) {
			power(r, level, pos, s);
		}
		if (s.hasAnalogOutputSignal()) {
			analog(r, level, pos, s, exact);
		}
		return r;
	}

	private static void power(Report r, Level level, BlockPos pos, BlockState s) {
		if (s.isSignalSource()) {
			int out = 0;
			for (Direction d : Direction.values()) {
				out = Math.max(out, s.getSignal(level, pos, d));
			}
			r.add(out > 0 ? Kind.OK : Kind.INFO, t("rs.source", out));
			return;
		}
		int in = level.getBestNeighborSignal(pos);
		r.add(in > 0 ? Kind.OK : Kind.INFO, in > 0 ? t("rs.receives", in) : t("rs.unpowered"));
		if (r.brief().isEmpty()) {
			r.brief(in > 0 ? Kind.OK : Kind.INFO, in > 0 ? t("rs.receives", in) : t("rs.unpowered"));
		}
		if (level.getDirectSignalTo(pos) > 0 && s.isRedstoneConductor(level, pos)) {
			r.sub(Kind.INFO, t("rs.strong"));
		}
	}

	private static void wire(Report r, Level level, BlockPos pos, BlockState s) {
		int p = s.getValue(BlockStateProperties.POWER);
		r.verdict(p > 0 ? Kind.OK : Kind.WARN, t("rs.wire.power", p));
		if (p > 0) {
			Direction from = null;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				for (int dy = -1; dy <= 1 && from == null; dy++) {
					BlockState n = level.getBlockState(pos.relative(d).above(dy));
					if (n.is(Blocks.REDSTONE_WIRE) && n.getValue(BlockStateProperties.POWER) == p + 1) {
						from = d;
					}
				}
			}
			if (from != null) {
				r.sub(Kind.INFO, t("rs.wire.from", dir(from)));
			} else if (p == 15) {
				r.sub(Kind.INFO, t("rs.wire.source"));
			}
			r.add(Kind.INFO, t("rs.wire.reach", p - 1));
			r.brief(Kind.INFO, t("rs.brief.reach", p - 1));
		} else {
			r.add(Kind.INFO, t("rs.wire.hint"));
		}
	}

	private static void repeater(Report r, Level level, BlockPos pos, BlockState s) {
		int delay = s.getValue(RepeaterBlock.DELAY);
		if (s.getValue(RepeaterBlock.LOCKED)) {
			r.verdict(Kind.WARN, t("rs.verdict.locked"));
		}
		r.verdict(Kind.OK, t("rs.verdict.delay", String.format(java.util.Locale.ROOT, "%.1f", delay * 0.1)));
		r.add(Kind.INFO, t("rs.repeater.delay", delay, delay * 2, String.format(java.util.Locale.ROOT, "%.1f", delay * 0.1)));
		int in = ((DiodeBlockInvoker) s.getBlock()).vanillaexplained$getInputSignal(level, pos, s);
		Direction back = s.getValue(HorizontalDirectionalBlock.FACING);
		r.add(in > 0 ? Kind.OK : Kind.INFO, t("rs.diode.input", in, dir(back)));
		r.brief(in > 0 ? Kind.OK : Kind.INFO, t("rs.brief.input", in));
		if (s.getValue(RepeaterBlock.LOCKED)) {
			r.add(Kind.WARN, t("rs.repeater.locked"));
		}
		r.add(Kind.INFO, t("rs.repeater.out", dir(back.getOpposite())));
	}

	private static void comparator(Report r, Level level, BlockPos pos, BlockState s, boolean exact) {
		var inv = (DiodeBlockInvoker) s.getBlock();
		int rear = inv.vanillaexplained$getInputSignal(level, pos, s);
		int side = inv.vanillaexplained$getAlternateSignal(level, pos, s);
		boolean sub = s.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT;
		Direction back = s.getValue(HorizontalDirectionalBlock.FACING);
		BlockState behind = level.getBlockState(pos.relative(back));
		r.add(Kind.INFO, t("rs.comparator.mode", t(sub ? "rs.comparator.subtract" : "rs.comparator.compare")));
		r.add(rear > 0 ? Kind.OK : Kind.INFO, t("rs.diode.input", rear, dir(back)));
		if (behind.hasAnalogOutputSignal() && !exact && HopperBlockEntity.getContainerAt(level, pos.relative(back)) != null) {
			r.sub(Kind.WARN, t("rs.comparator.container_mp"));
		}
		r.add(Kind.INFO, t("rs.comparator.side", side));
		r.brief(Kind.INFO, t("rs.brief.comparator", rear, side, t(sub ? "rs.comparator.subtract" : "rs.comparator.compare")));
		int out = RedstoneMath.comparatorOutput(rear, side, sub);
		r.forceVerdict(out > 0 ? Kind.OK : Kind.WARN, t("rs.verdict.output", out));
		r.add(out > 0 ? Kind.OK : Kind.INFO, t(sub ? "rs.comparator.out_sub" : "rs.comparator.out_cmp", out, rear, side));
	}

	private static void piston(Report r, Level level, BlockPos pos, BlockState s) {
		Direction f = s.getValue(DirectionalBlock.FACING);
		boolean extended = s.getValue(PistonBaseBlock.EXTENDED);
		boolean direct = false;
		for (Direction d : Direction.values()) {
			if (d != f && level.hasSignal(pos.relative(d), d)) {
				direct = true;
			}
		}
		boolean qc = false;
		if (!direct && level.hasSignal(pos, Direction.DOWN)) {
			direct = true;
		}
		if (!direct) {
			BlockPos up = pos.above();
			for (Direction d : Direction.values()) {
				if (d != Direction.DOWN && level.hasSignal(up.relative(d), d)) {
					qc = true;
				}
			}
		}
		r.add(Kind.INFO, t(extended ? "rs.piston.extended" : "rs.piston.retracted", dir(f)));
		if (direct) {
			r.add(Kind.OK, t("rs.piston.powered"));
		} else if (qc) {
			r.verdict(Kind.WARN, t("rs.verdict.qc"));
			r.add(Kind.WARN, t("rs.piston.qc"));
			r.brief(Kind.WARN, t("rs.brief.qc"));
		} else {
			r.add(Kind.INFO, t("rs.unpowered"));
		}
		if (!extended) {
			PistonStructureResolver res = new PistonStructureResolver(level, pos, f, true);
			if (res.resolve()) {
				r.verdict(Kind.OK, t("rs.verdict.can_push"));
				r.add(Kind.OK, t("rs.piston.can_push", res.getToPush().size()));
				r.brief(Kind.OK, t("rs.piston.can_push", res.getToPush().size()));
			} else {
				BlockPos cur = pos.relative(f);
				Component why = t("rs.piston.too_many");
				for (int i = 0; i <= PistonStructureResolver.MAX_PUSH_DEPTH; i++, cur = cur.relative(f)) {
					BlockState st = level.getBlockState(cur);
					if (st.isAir()) {
						break;
					}
					if (!PistonBaseBlock.isPushable(st, level, cur, f, true, f)) {
						why = t("rs.piston.immovable", st.getBlock().getName(), cur.getX(), cur.getY(), cur.getZ());
						break;
					}
				}
				r.forceVerdict(Kind.BAD, t("rs.verdict.cant_push"));
				r.add(Kind.BAD, t("rs.piston.cant_push", why));
				r.brief(Kind.BAD, why.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
						&& tc.getKey().endsWith("too_many") ? t("rs.brief.too_many") : why);
			}
		}
		r.add(Kind.INFO, t("rs.piston.limit"));
	}

	private static void analog(Report r, Level level, BlockPos pos, BlockState s, boolean exact) {
		Container c = HopperBlockEntity.getContainerAt(level, pos);
		if (c != null) {
			int slots = c.getContainerSize();
			if (exact) {
				r.verdict(Kind.OK, t("rs.analog.reads", dev.vinterus.vanillaexplained.client.core.Mc.analogOutput(s, level, pos)));
			} else {
				r.add(Kind.WARN, t("rs.analog.mp"));
			}
			r.brief(Kind.INFO, t("rs.brief.table", RedstoneMath.itemsForSignal(slots, 64, 2), RedstoneMath.itemsForSignal(slots, 64, 15)));
			r.add(Kind.INFO, t("rs.analog.table", slots, RedstoneMath.itemsForSignal(slots, 64, 1),
					RedstoneMath.itemsForSignal(slots, 64, 2), RedstoneMath.itemsForSignal(slots, 64, 15),
					RedstoneMath.itemsForSignal(slots, 16, 15), RedstoneMath.itemsForSignal(slots, 1, 15)));
			r.sub(Kind.INFO, t("rs.analog.more"));
		} else {
			r.verdict(Kind.OK, t("rs.analog.reads", dev.vinterus.vanillaexplained.client.core.Mc.analogOutput(s, level, pos)));
		}
	}

	private static void enchanting(Report r, Level level, BlockPos pos) {
		int ok = 0, blocked = 0;
		for (BlockPos off : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, pos, off)) {
				ok++;
			} else if (level.getBlockState(pos.offset(off)).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
				blocked++;
			}
		}
		int[] lv = RedstoneMath.topEnchantLevel(ok);
		r.verdict(ok >= 15 ? Kind.OK : Kind.WARN, t(ok >= 15 ? "ench.verdict.max" : "ench.verdict.low", RedstoneMath.topEnchantLevel(ok)[1]));
		r.add(ok >= 15 ? Kind.OK : Kind.WARN, t("ench.shelves", Math.min(ok, 15)));
		r.brief(ok >= 15 ? Kind.OK : Kind.WARN, t("ench.shelves", Math.min(ok, 15)));
		r.add(Kind.INFO, t("ench.top", lv[0] == lv[1] ? Integer.toString(lv[0]) : lv[0] + "–" + lv[1]));
		if (blocked > 0) {
			r.brief(Kind.BAD, t("ench.brief.blocked", blocked));
			r.add(Kind.BAD, t("ench.blocked", blocked));
		}
		if (ok < 15) {
			r.add(Kind.INFO, t("ench.hint", 15 - ok));
		}
	}

	private static void beacon(Report r, Level level, BlockPos pos) {
		int levels = 0;
		BlockPos bad = null;
		for (int step = 1; step <= 4 && bad == null; step++) {
			int y = pos.getY() - step;
			for (int x = pos.getX() - step; x <= pos.getX() + step && bad == null; x++) {
				for (int z = pos.getZ() - step; z <= pos.getZ() + step; z++) {
					BlockPos p = new BlockPos(x, y, z);
					if (!level.getBlockState(p).is(BlockTags.BEACON_BASE_BLOCKS)) {
						bad = p;
						break;
					}
				}
			}
			if (bad == null) {
				levels = step;
			}
		}
		r.verdict(levels >= 4 ? Kind.OK : levels > 0 ? Kind.WARN : Kind.BAD, t("beacon.levels", levels));
		if (levels < 4 && bad != null) {
			int size = (levels + 1) * 2 + 1;
			r.sub(Kind.INFO, t("beacon.missing", levels + 1, size, size, bad.getX(), bad.getY(), bad.getZ()));
		}
		if (levels > 0) {
			r.add(Kind.INFO, t("beacon.range", RedstoneMath.beaconRange(levels)));
			r.brief(Kind.INFO, t("beacon.range", RedstoneMath.beaconRange(levels)));
			r.add(Kind.INFO, t(levels >= 4 ? "beacon.effects4" : "beacon.effects" + levels));
		}
		for (BlockPos p = pos.above(); p.getY() <= dev.vinterus.vanillaexplained.client.core.Mc.maxY(level); p = p.above()) {
			BlockState st = level.getBlockState(p);
			if (dev.vinterus.vanillaexplained.client.core.Mc.lightDampening(st, level, p) >= 15 && !st.is(Blocks.BEDROCK)) {
				r.add(Kind.BAD, t("beacon.blocked", st.getBlock().getName(), p.getY()));
				r.brief(Kind.BAD, t("beacon.blocked", st.getBlock().getName(), p.getY()));
				return;
			}
		}
		r.add(Kind.OK, t("beacon.sky"));
	}
}
