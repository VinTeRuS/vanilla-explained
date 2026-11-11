package dev.vinterus.vanillaexplained.client.portal;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import dev.vinterus.vanillaexplained.client.core.Report;
import dev.vinterus.vanillaexplained.client.core.Report.Kind;
import dev.vinterus.vanillaexplained.rules.PortalLinker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Predicts where a Nether portal leads and whether the link works both ways. */
public final class PortalInspector {
	private static final double BORDER = 29_999_984;

	private PortalInspector() {
	}

	public static String dimId(net.minecraft.resources.ResourceKey<Level> key) {
		return dev.vinterus.vanillaexplained.client.core.Mc.dimId(key);
	}

	public static Report inspect(Level level, BlockPos portalBlock) {
		boolean inNether = level.dimension() == Level.NETHER;
		if (!inNether && level.dimension() != Level.OVERWORLD) {
			return new Report(t("portal.title")).add(Kind.BAD, t("portal.wrong_dim"));
		}
		boolean toNether = !inNether;
		String here = dimId(level.dimension());
		String there = dimId(toNether ? Level.NETHER : Level.OVERWORLD);
		Report r = new Report(t("portal.title")).icon(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OBSIDIAN))
				.tip(dev.vinterus.vanillaexplained.client.core.Tips.next("portal"));
		var res = PortalLinker.predict(portalBlock.getX() + 0.5, portalBlock.getY(), portalBlock.getZ() + 0.5, toNether,
				PortalMemory.portals(there), -BORDER, BORDER);
		var d = res.destination();
		r.add(Kind.INFO, t(toNether ? "portal.dest.nether" : "portal.dest.overworld", d.x(), d.y(), d.z()));
		r.brief(Kind.INFO, t("portal.brief.dest", d.x(), d.y(), d.z()));
		if (res.linkedPortal().isPresent()) {
			var p = res.linkedPortal().get();
			int dist = (int) Math.sqrt(p.distSqr(d));
			r.add(Kind.OK, t("portal.links_to", p.x(), p.y(), p.z(), dist));
			r.brief(Kind.OK, t("portal.brief.link", p.x(), p.y(), p.z()));
			// Reverse direction: where does the target portal lead back to?
			var back = PortalLinker.predict(p.x() + 0.5, p.y(), p.z() + 0.5, !toNether, PortalMemory.portals(here), -BORDER, BORDER);
			var self = new PortalLinker.Pos(portalBlock.getX(), portalBlock.getY(), portalBlock.getZ());
			if (back.linkedPortal().isPresent() && back.linkedPortal().get().distSqr(self) <= 2 * 2 + 21 * 21) {
				r.verdict(Kind.OK, t("portal.two_way"));
			} else if (back.linkedPortal().isPresent()) {
				var b = back.linkedPortal().get();
				r.verdict(Kind.WARN, t("portal.verdict.one_way"));
				r.add(Kind.WARN, t("portal.one_way", b.x(), b.y(), b.z()));
				r.brief(Kind.WARN, t("portal.brief.fix", (int) Math.floor(p.x() * (toNether ? 8.0 : 0.125)), (int) Math.floor(p.z() * (toNether ? 8.0 : 0.125))));
				r.sub(Kind.INFO, t("portal.fix_hint", (int) Math.floor(p.x() * (toNether ? 8.0 : 0.125)),
						(int) Math.floor(p.z() * (toNether ? 8.0 : 0.125))));
			} else {
				r.verdict(Kind.OK, t("portal.verdict.linked"));
				r.add(Kind.WARN, t("portal.back_unknown"));
			}
		} else {
			r.verdict(Kind.WARN, t("portal.verdict.new"));
			r.add(Kind.WARN, t("portal.new_portal", res.searchRadius()));
			r.sub(Kind.INFO, t("portal.new_portal_hint", d.x(), d.z()));
			r.brief(Kind.WARN, t("portal.brief.build", d.x(), d.z()));
			r.sub(Kind.INFO, t("portal.memory_note"));
		}
		return r;
	}
}
