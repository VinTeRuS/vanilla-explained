package dev.vinterus.vanillaexplained.client.screen;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.vinterus.vanillaexplained.client.core.Tips;
import dev.vinterus.vanillaexplained.client.core.Ui;
import dev.vinterus.vanillaexplained.client.elytra.ElytraTracker;
import dev.vinterus.vanillaexplained.client.portal.PortalInspector;
import dev.vinterus.vanillaexplained.client.portal.PortalMemory;
import dev.vinterus.vanillaexplained.rules.PortalLinker;
import dev.vinterus.vanillaexplained.rules.RedstoneMath;
import dev.vinterus.vanillaexplained.client.core.Gfx;
import dev.vinterus.vanillaexplained.client.core.VeScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** The J panel, docked to the right so the world stays visible. */
public final class CompanionScreen extends VeScreen {
	private enum Tab {
		ELYTRA(Items.ELYTRA, "tab.elytra"), PORTALS(Items.OBSIDIAN, "tab.portals"), REDSTONE(Items.REDSTONE, "tab.redstone"),
		TIPS(Items.GLOWSTONE_DUST, "tab.tips"), HELP(Items.BOOK, "tab.help");

		final Item item;
		final String key;

		Tab(Item item, String key) {
			this.item = item;
			this.key = key;
		}
	}

	private record Container(Item icon, String key, int slots) {
	}

	private static final List<Container> CONTAINERS = List.of(new Container(Items.CHEST, "chest", 27),
			new Container(Items.CHEST, "double_chest", 54), new Container(Items.BARREL, "barrel", 27),
			new Container(Items.SHULKER_BOX, "shulker", 27), new Container(Items.HOPPER, "hopper", 5),
			new Container(Items.DROPPER, "dispenser", 9), new Container(Items.FURNACE, "furnace", 3));
	private static final int[] STACKS = { 64, 16, 1 };
	private static final Map<String, Item> TOPIC_ICONS = Map.of("spawn", Items.ZOMBIE_HEAD, "crop", Items.WHEAT, "rs", Items.REDSTONE,
			"ench", Items.ENCHANTING_TABLE, "anvil", Items.ANVIL, "portal", Items.OBSIDIAN, "beacon", Items.BEACON, "elytra", Items.ELYTRA);
	private static final int TAB = 24, HEAD = 46, M = 10;

	private static Tab tab = Tab.ELYTRA;
	private static int container = 0, stack = 0;
	private static double tipsScroll = 0;

	private EditBox xBox, zBox;
	private Tab hoverTab;
	private int resetX = -1, resetY, resetW;
	private int left, top, pw, ph, cw;

	/** Used by the smoke test. */
	public static void selectTab(int index) {
		tab = Tab.values()[index];
	}

	public CompanionScreen() {
		super(t("screen.title"));
	}

	@Override
	protected void init() {
		pw = Math.max(236, Math.min(280, (int) (this.width * 0.42)));
		pw = Math.min(pw, this.width - 8);
		ph = Math.min(260, this.height - 16);
		left = this.width - pw - 8;
		top = (this.height - ph) / 2;
		cw = pw - M * 2;
		if (tab == Tab.ELYTRA) {
			int y = top + HEAD + 14;
			int bw = 54;
			xBox = new EditBox(this.font, left + M, y, bw, 18, Component.literal("X"));
			zBox = new EditBox(this.font, left + M + bw + 4, y, bw, 18, Component.literal("Z"));
			xBox.setHint(Component.literal("X"));
			zBox.setHint(Component.literal("Z"));
			var target = ElytraTracker.target();
			if (target != null) {
				xBox.setValue(Integer.toString(target.x()));
				zBox.setValue(Integer.toString(target.z()));
			}
			addRenderableWidget(xBox);
			addRenderableWidget(zBox);
			int fx = left + M + bw * 2 + 8;
			int rest = left + pw - M - fx;
			addRenderableWidget(Button.builder(t("elytra.set"), b -> setTarget()).bounds(fx, y - 1, rest - 24, 20).build());
			addRenderableWidget(Button.builder(Component.literal("✕"), b -> ElytraTracker.setTarget(null)).bounds(fx + rest - 20, y - 1, 20, 20).build());
		}
	}

	private void setTarget() {
		try {
			int x = Integer.parseInt(xBox.getValue().trim());
			int z = Integer.parseInt(zBox.getValue().trim());
			ElytraTracker.setTarget(new ElytraTracker.Target(x, z, dev.vinterus.vanillaexplained.client.core.Mc.dimId(this.minecraft.player.level())));
		} catch (NumberFormatException e) {
			ElytraTracker.setTarget(null);
		}
	}

	private int tabX(Tab t) {
		return left + M + t.ordinal() * (TAB + 4);
	}

	@Override
	protected boolean click(double mx, double my) {
		for (Tab t : Tab.values()) {
			if (Ui.inside(mx, my, tabX(t), top + 6, TAB, TAB)) {
				tab = t;
				tipsScroll = 0;
				this.rebuildWidgets();
				return true;
			}
		}
		if (Ui.inside(mx, my, left + pw - 20, top + 6, 14, 14)) {
			this.onClose();
			return true;
		}
		if (tab == Tab.ELYTRA && resetX >= 0 && Ui.inside(mx, my, resetX, resetY, resetW, 10)) {
			ElytraTracker.resetStats();
			return true;
		}
		if (tab == Tab.REDSTONE) {
			int y = top + HEAD + 14;
			for (int i = 0; i < CONTAINERS.size(); i++) {
				if (Ui.inside(mx, my, left + M + i * 24, y, 22, 22)) {
					container = i;
					return true;
				}
			}
			for (int i = 0; i < STACKS.length; i++) {
				if (Ui.inside(mx, my, left + M + i * 30, y + 26, 28, 13)) {
					stack = i;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected boolean scroll(double x, double y, double delta) {
		if (tab == Tab.TIPS) {
			tipsScroll = Math.max(0, tipsScroll - delta * 20);
			return true;
		}
		return false;
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY) {
		// no blur or dimming: the world stays visible next to the panel
		Ui.panel(g, left, top, pw, ph, 0xF0101016, Ui.BORDER);
		Ui.round(g, left + 1, top + 1, pw - 2, HEAD - 12, Ui.BG_HEADER);
		for (Tab t : Tab.values()) {
			int tx = tabX(t);
			boolean active = t == tab;
			boolean hover = Ui.inside(mouseX, mouseY, tx, top + 6, TAB, TAB);
			Ui.round(g, tx, top + 6, TAB, TAB, active ? Ui.CARD_HOVER : hover ? Ui.CARD : 0x00000000);
			if (active) {
				g.fill(tx + 3, top + 6 + TAB, tx + TAB - 3, top + 8 + TAB, Ui.ACCENT);
			}
			Ui.item(g, Ui.stack(t.item), tx + 4, top + 10);
		}
		boolean closeHover = Ui.inside(mouseX, mouseY, left + pw - 20, top + 6, 14, 14);
		Ui.text(g, Component.literal("✕"), left + pw - 17, top + 9, closeHover ? Ui.TEXT : Ui.MUTED);
		Ui.text(g, t(tab.key), left + M, top + HEAD - 4, Ui.ACCENT);
	}

	@Override
	protected void drawContent(Gfx g, int mouseX, int mouseY) {
		for (Tab t : Tab.values()) {
			if (Ui.inside(mouseX, mouseY, tabX(t), top + 6, TAB, TAB)) {
				hoverTab = t;
			}
		}
		boolean world = this.minecraft.player != null;
		switch (tab) {
			case ELYTRA -> {
				if (world) {
					elytra(g);
				}
			}
			case PORTALS -> {
				if (world) {
					portals(g);
				}
			}
			case REDSTONE -> redstone(g, mouseX, mouseY);
			case TIPS -> tips(g);
			case HELP -> help(g);
		}
		if (hoverTab != null) {
			g.tooltip(t(hoverTab.key), mouseX, mouseY);
			hoverTab = null;
		}
	}

	private int wrapped(Gfx g, Component text, int x, int y, int w, int color) {
		for (FormattedCharSequence row : this.font.split(text, w)) {
			if (y + 9 <= top + ph - 3) {
				Ui.text(g, row, x, y, color);
			}
			y += 10;
		}
		return y;
	}

	private void elytra(Gfx g) {
		var p = this.minecraft.player;
		int x = left + M, ty = top + HEAD + 40;
		int tw = (cw - 4) / 2, th = 34;
		Component dist = Component.literal("—"), rockets = Component.literal("—/" + ElytraTracker.countRockets(p)),
				time = Component.literal("—"), dur = Component.literal("—");
		int rocketColor = Ui.TEXT, durColor = Ui.TEXT;
		var el = ElytraTracker.elytra(p);
		var target = ElytraTracker.target();
		if (target != null) {
			double dx = target.x() - p.getX(), dz = target.z() - p.getZ();
			double d = Math.sqrt(dx * dx + dz * dz);
			var e = ElytraTracker.estimateTo(p, d);
			dist = Component.literal(Long.toString(Math.round(d)));
			rockets = Component.literal(e.rockets() + "/" + ElytraTracker.countRockets(p));
			time = Component.literal(fmtTime(e.seconds()));
			rocketColor = e.enoughRockets() ? Ui.OK : Ui.BAD;
			if (!el.isEmpty()) {
				dur = Component.literal((int) Math.ceil(e.durabilityUsed()) + "/" + (el.getMaxDamage() - el.getDamageValue()));
				durColor = e.enoughDurability() ? Ui.OK : Ui.BAD;
			}
		}
		if (el.isEmpty()) {
			durColor = Ui.WARN;
		}
		Ui.tile(g, x, ty, tw, th, Ui.stack(Items.COMPASS), dist, t("elytra.tile.distance"), Ui.TEXT);
		Ui.tile(g, x + tw + 4, ty, tw, th, Ui.stack(Items.FIREWORK_ROCKET), rockets, t("elytra.tile.rockets"), rocketColor);
		Ui.tile(g, x, ty + th + 4, tw, th, Ui.stack(Items.CLOCK), time, t("elytra.tile.time"), Ui.TEXT);
		Ui.tile(g, x + tw + 4, ty + th + 4, tw, th, Ui.stack(Items.ELYTRA), dur, t("elytra.tile.durability"), durColor);
		Component hint = target == null ? t("elytra.no_target") : el.isEmpty() ? t("elytra.no_elytra") : t("elytra.hud_hint");
		int sy = wrapped(g, hint, x, ty + th * 2 + 10, cw, Ui.MUTED) + 6;
		g.horizontalLine(left + M, left + pw - M, sy, Ui.BORDER);
		sy += 4;
		Ui.text(g, t("elytra.section.style"), x, sy + 3, Ui.ACCENT);
		Component reset = t("elytra.reset_link");
		resetW = this.font.width(reset);
		resetX = left + pw - M - resetW;
		resetY = sy + 3;
		Ui.text(g, reset, resetX, resetY, Ui.MUTED);
		g.horizontalLine(resetX, resetX + resetW, resetY + 9, Ui.MUTED);
		sy -= 4;
		var s = ElytraTracker.stats();
		Component style = ElytraTracker.learned()
				? t("elytra.learned", Math.round(s.distance), s.rockets, String.format(Locale.ROOT, "%.0f", s.distance / s.rockets),
						String.format(Locale.ROOT, "%.1f", ElytraTracker.speedPerTick() * 20))
				: t("elytra.learning", s.rockets, Math.round(s.distance));
		int yy = wrapped(g, style, x, sy + 18, cw, Ui.TEXT);
		if (yy > top + ph - 8) {
			return;
		}
		if (!ElytraTracker.learned()) {
			wrapped(g, t("elytra.baseline", String.format(Locale.ROOT, "%.0f", ElytraTracker.blocksPerRocket(ElytraTracker.rocketDuration(p))),
					ElytraTracker.rocketDuration(p)), x, yy + 2, cw, Ui.MUTED);
		}
	}

	private void portals(Gfx g) {
		var p = this.minecraft.player;
		boolean ow = p.level().dimension() == Level.OVERWORLD;
		boolean nether = p.level().dimension() == Level.NETHER;
		int x = left + M, y = top + HEAD + 12;
		if (!ow && !nether) {
			wrapped(g, t("portal.wrong_dim"), x, y, cw, Ui.MUTED);
			return;
		}
		var d = PortalLinker.destination(p.getX(), p.blockPosition().getY(), p.getZ(), ow, -29_999_984, 29_999_984);
		int owX = ow ? p.blockPosition().getX() : d.x(), owZ = ow ? p.blockPosition().getZ() : d.z();
		int nX = ow ? d.x() : p.blockPosition().getX(), nZ = ow ? d.z() : p.blockPosition().getZ();
		int cardW = (cw - 4) / 2, cardH = 56;
		card(g, x, y, cardW, cardH, Ui.stack(Items.GRASS_BLOCK), t("portal.card.overworld"), owX, owZ, ow);
		card(g, x + cardW + 4, y, cardW, cardH, Ui.stack(Items.NETHERRACK), t("portal.card.nether"), nX, nZ, nether);
		Ui.text(g, Component.literal("÷8 → / ← ×8"), x, y + cardH + 4, Ui.MUTED);

		String there = PortalInspector.dimId(ow ? Level.NETHER : Level.OVERWORLD);
		var res = PortalLinker.predict(p.getX(), p.blockPosition().getY(), p.getZ(), ow, PortalMemory.portals(there), -29_999_984, 29_999_984);
		boolean linked = res.linkedPortal().isPresent();
		Component status = linked
				? t("portal.calc.would_link", res.linkedPortal().get().x(), res.linkedPortal().get().y(), res.linkedPortal().get().z())
				: t("portal.calc.would_create", res.searchRadius());
		int sy = y + cardH + 18;
		int sh = this.font.split(status, cw - 14).size() * 10 + 10;
		Ui.panel(g, x, sy, cw, sh, Ui.CARD, Ui.BORDER);
		g.fill(x + 1, sy + 1, x + 4, sy + sh - 1, linked ? Ui.OK : Ui.WARN);
		wrapped(g, status, x + 9, sy + 5, cw - 14, linked ? Ui.OK : Ui.WARN);
		int yy = wrapped(g, t("portal.calc.known", PortalMemory.portals(PortalInspector.dimId(Level.OVERWORLD)).size(),
				PortalMemory.portals(PortalInspector.dimId(Level.NETHER)).size()), x, sy + sh + 8, cw, Ui.MUTED);
		wrapped(g, t("portal.calc.hint"), x, yy + 4, cw, Ui.MUTED);
	}

	private void card(Gfx g, int x, int y, int w, int h, ItemStack icon, Component name, int cx, int cz, boolean here) {
		Ui.panel(g, x, y, w, h, here ? Ui.CARD_HOVER : Ui.CARD, here ? Ui.ACCENT : Ui.BORDER);
		Ui.item(g, icon, x + 5, y + 5);
		Ui.text(g, name, x + 24, y + 9, Ui.TEXT);
		Ui.scaledText(g, Component.literal("X " + cx), x + 7, y + 26, 1.15F, Ui.TEXT);
		Ui.scaledText(g, Component.literal("Z " + cz), x + 7, y + 39, 1.15F, Ui.TEXT);
	}

	private void redstone(Gfx g, int mouseX, int mouseY) {
		int x = left + M, y = top + HEAD + 14;
		Container c = CONTAINERS.get(container);
		Ui.text(g, t("rs.calc.container", t("rs.calc." + c.key()), c.slots()), x + 96, y + 29, Ui.TEXT);
		for (int i = 0; i < CONTAINERS.size(); i++) {
			int bx = x + i * 24;
			boolean sel = i == container;
			boolean hover = Ui.inside(mouseX, mouseY, bx, y, 22, 22);
			Ui.panel(g, bx, y, 22, 22, sel || hover ? Ui.CARD_HOVER : Ui.CARD, sel ? Ui.ACCENT : Ui.BORDER);
			Ui.item(g, Ui.stack(CONTAINERS.get(i).icon()), bx + 3, y + 3);
			if (i == 1) {
				Ui.text(g, Component.literal("2"), bx + 15, y + 13, Ui.TEXT);
			}
		}
		for (int i = 0; i < STACKS.length; i++) {
			int bx = x + i * 30;
			boolean sel = i == stack;
			Ui.panel(g, bx, y + 26, 28, 13, sel ? Ui.CARD_HOVER : Ui.CARD, sel ? Ui.ACCENT : Ui.BORDER);
			g.centeredText(Component.literal("×" + STACKS[i]), bx + 14, y + 29, sel ? Ui.TEXT : Ui.MUTED);
		}
		int gy = y + 46, cols = 3, gap = 3;
		int cellW = (cw - gap * (cols - 1)) / cols, cellH = 20;
		int st = STACKS[stack];
		for (int sig = 1; sig <= 15; sig++) {
			int i = sig - 1;
			int cx = x + (i % cols) * (cellW + gap), cy = gy + (i / cols) * (cellH + gap);
			Ui.panel(g, cx, cy, cellW, cellH, Ui.CARD, Ui.BORDER);
			int red = 0xFF000000 | ((80 + sig * 11) << 16) | 0x1818;
			Ui.round(g, cx + 2, cy + 2, 15, cellH - 4, red);
			g.centeredText(Component.literal(Integer.toString(sig)), cx + 10, cy + 6, 0xFFFFFFFF);
			int n = RedstoneMath.itemsForSignal(c.slots(), st, sig);
			Ui.text(g, Component.literal(Integer.toString(n)), cx + 21, cy + 2, Ui.TEXT);
			String stacks = st > 1 && n >= st ? (n / st) + "×" + st + (n % st > 0 ? "+" + (n % st) : "") : "";
			Ui.text(g, Component.literal(stacks), cx + 21, cy + 11, Ui.MUTED);
		}
		int ty = gy + 5 * (cellH + gap) + 3;
		if (ty + 20 <= top + ph - 4) {
			wrapped(g, t("rs.calc.tip3"), x, ty, cw, Ui.MUTED);
		}
	}

	private void tips(Gfx g) {
		int x = left + M, y0 = top + HEAD + 10, bottom = top + ph - 8;
		g.enableScissor(left + 1, y0, left + pw - 1, bottom);
		int y = y0 - (int) tipsScroll;
		for (var entry : Tips.BY_TOPIC.entrySet()) {
			Ui.item(g, Ui.stack(TOPIC_ICONS.get(entry.getKey())), x, y);
			Ui.text(g, t("tip.topic." + entry.getKey()), x + 20, y + 4, Ui.ACCENT);
			y += 20;
			for (String id : entry.getValue()) {
				var rows = this.font.split(Tips.key(entry.getKey(), id), cw - 12);
				g.fill(x + 3, y + 3, x + 6, y + 6, Ui.WARN);
				for (var row : rows) {
					Ui.text(g, row, x + 12, y, Ui.TEXT);
					y += 10;
				}
				y += 4;
			}
			y += 4;
		}
		g.disableScissor();
		int total = y + (int) tipsScroll - y0;
		tipsScroll = Math.min(tipsScroll, Math.max(0, total - (bottom - y0)));
		if (total > bottom - y0) {
			int barH = Math.max(16, (bottom - y0) * (bottom - y0) / total);
			int barY = y0 + (int) ((bottom - y0 - barH) * (tipsScroll / Math.max(1, total - (bottom - y0))));
			Ui.round(g, left + pw - 5, barY, 3, barH, Ui.BORDER);
		}
	}

	private void help(Gfx g) {
		int x = left + M, y = top + HEAD + 12;
		Ui.keycap(g, keyName("key.vanillaexplained.why", "G"), x, y);
		y = wrapped(g, t("help.why"), x + 24, y + 3, cw - 24, Ui.TEXT) + 4;
		for (String k : new String[] { "help.2", "help.3", "help.4", "help.8", "help.9" }) {
			y = wrapped(g, Component.literal(t(k).getString().replaceFirst("^•\\s*", "")), x + 8, y, cw - 8, Ui.MUTED) + 3;
		}
		y += 4;
		Ui.keycap(g, keyName("key.vanillaexplained.companion", "J"), x, y);
		y = wrapped(g, t("help.planner"), x + 24, y + 3, cw - 24, Ui.TEXT) + 6;
		Ui.item(g, Ui.stack(Items.ANVIL), x, y - 2);
		y = wrapped(g, t("help.6"), x + 20, y + 2, cw - 20, Ui.MUTED) + 6;
		wrapped(g, t("help.7"), x, y, cw, Ui.OK);
	}

	private String keyName(String name, String fallback) {
		for (var km : this.minecraft.options.keyMappings) {
			if (km.getName().equals(name)) {
				return km.getTranslatedKeyMessage().getString();
			}
		}
		return fallback;
	}

	private static String fmtTime(double seconds) {
		long s = Math.round(seconds);
		return s >= 60 ? (s / 60) + ":" + String.format(Locale.ROOT, "%02d", s % 60) : s + "s";
	}

}
