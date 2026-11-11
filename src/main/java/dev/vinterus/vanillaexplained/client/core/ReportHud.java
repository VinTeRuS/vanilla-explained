package dev.vinterus.vanillaexplained.client.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Small Jade-style tooltip at the top of the screen. Shows the verdict and a few short facts;
 * holding Shift expands it to every detail and the pro tip.
 */
public final class ReportHud {
	private static final int SHOW_TICKS = 20 * 15;
	private static final int FADE_TICKS = 5;
	private static final int LOOK_AWAY_GRACE = 15;
	private static final int COMPACT_W = 240;
	private static final int EXPANDED_W = 250;

	private static Report current;
	private static int age;
	private static int ticksLeft;
	private static int lookingAway;

	private ReportHud() {
	}

	public static void show(Report report) {
		current = report;
		age = 0;
		lookingAway = 0;
		ticksLeft = SHOW_TICKS;
	}

	public static Report current() {
		return current;
	}

	public static boolean visible() {
		return current != null && ticksLeft > FADE_TICKS;
	}

	public static void hide() {
		if (current != null) {
			ticksLeft = Math.min(ticksLeft, FADE_TICKS);
		}
	}

	public static void tick() {
		if (current == null) {
			return;
		}
		age++;
		Minecraft mc = Minecraft.getInstance();
		if (current.target() != null && !Mc.shiftDown()) {
			HitResult hit = Mc.lookTarget(mc);
			boolean same = hit instanceof BlockHitResult b && hit.getType() != HitResult.Type.MISS && b.getBlockPos().equals(current.target());
			lookingAway = same ? 0 : lookingAway + 1;
			if (lookingAway == LOOK_AWAY_GRACE) {
				hide();
			}
		}
		if (--ticksLeft <= 0) {
			current = null;
		}
	}

	private record Row(FormattedCharSequence text, int color, int indent, List<ItemStack> icons) {
		int height() {
			return icons.isEmpty() ? 10 : 17;
		}
	}

	public static void render(Gfx g, float pt) {
		render(g, pt, false);
	}

	public static void render(Gfx g, float pt, boolean force) {
		Report r = current;
		Minecraft mc = Minecraft.getInstance();
		if (r == null || (Mc.screen() != null && !force)) {
			return;
		}
		Font font = mc.font;
		float a = Math.min(Math.min(1F, (age + pt) / FADE_TICKS), Math.max(0F, (ticksLeft - pt) / FADE_TICKS));
		boolean expanded = Mc.shiftDown();
		int maxW = Math.min(expanded ? EXPANDED_W : COMPACT_W, g.guiWidth() - 20);

		List<Row> rows = new ArrayList<>();
		if (r.verdict() != null) {
			String mark = switch (r.verdictKind()) {
				case OK -> "✔ ";
				case BAD -> "✘ ";
				case WARN -> "! ";
				default -> "";
			};
			Component v = Component.literal(mark).append(r.verdict());
			for (var line : font.split(v, maxW)) {
				rows.add(new Row(line, Ui.kindColor(r.verdictKind()), 0, List.of()));
			}
		}
		List<Report.Line> facts = expanded || r.brief().isEmpty() ? r.lines() : r.brief();
		for (Report.Line line : facts) {
			if (line.kind() == Report.Kind.HEADER) {
				if (expanded) {
					rows.add(new Row(line.text().getVisualOrderText(), Ui.ACCENT, 0, List.of()));
				}
				continue;
			}
			int color = line.kind() == Report.Kind.INFO ? Ui.MUTED : Ui.kindColor(line.kind());
			Component text = Component.literal("• ").append(line.text());
			int indent = line.indent() * 8;
			if (expanded) {
				boolean first = true;
				for (var part : font.split(text, maxW - indent)) {
					rows.add(new Row(part, color, indent + (first ? 0 : 8), List.of()));
					first = false;
				}
			} else {
				rows.add(new Row(Ui.fit(text, maxW - indent), color, indent, List.of()));
			}
			if (!line.icons().isEmpty()) {
				int per = Math.max(1, (maxW - 8) / 17);
				List<ItemStack> icons = line.icons();
				for (int i = 0; i < icons.size(); i += per) {
					rows.add(new Row(FormattedCharSequence.EMPTY, color, 8, icons.subList(i, Math.min(icons.size(), i + per))));
					if (!expanded) {
						break;
					}
				}
			}
		}
		if (expanded && r.tip() != null) {
			rows.add(new Row(FormattedCharSequence.EMPTY, 0, 0, List.of()));
			rows.add(new Row(Report.t("tip.header").getVisualOrderText(), Ui.WARN, 0, List.of()));
			for (var part : font.split(r.tip(), maxW)) {
				rows.add(new Row(part, 0xFFE0D0A0, 0, List.of()));
			}
		}
		boolean more = !expanded && (r.lines().size() > r.brief().size() || r.tip() != null);
		Component hint = Report.t(expanded ? "hud.release" : "hud.shift");

		int contentW = font.width(r.title()) + 20;
		for (Row row : rows) {
			contentW = Math.max(contentW, font.width(row.text()) + row.indent() + row.icons().size() * 17);
		}
		if (more || expanded) {
			contentW = Math.max(contentW, font.width(hint));
		}
		contentW = Math.min(contentW, maxW);
		int contentH = 18;
		for (Row row : rows) {
			contentH += row.height();
		}
		if (more || expanded) {
			contentH += 11;
		}
		int maxH = g.guiHeight() - 40;
		contentH = Math.min(contentH, maxH);

		int x = (g.guiWidth() - contentW) / 2;
		int y = 6 + (int) ((1 - a) * -6);
		Ui.tooltip(g, x - 4, y - 4, contentW + 8, contentH + 7, a);
		Ui.item(g, r.icon(), x, y - 1);
		Ui.text(g, Ui.fit(r.title(), contentW - 20), x + 20, y + 3, Ui.withAlpha(Ui.TEXT, a));
		int cy = y + 18;
		int limit = y + contentH - (more || expanded ? 11 : 0);
		for (Row row : rows) {
			if (cy + row.height() > limit) {
				break;
			}
			if (!row.icons().isEmpty()) {
				int ix = x + row.indent();
				for (ItemStack icon : row.icons()) {
					Ui.item(g, icon, ix, cy);
					ix += 17;
				}
			} else {
				Ui.text(g, row.text(), x + row.indent(), cy, Ui.withAlpha(row.color(), a));
			}
			cy += row.height();
		}
		if (more || expanded) {
			Ui.text(g, hint, x, y + contentH - 9, Ui.withAlpha(0xFF707070, a));
		}
	}
}
