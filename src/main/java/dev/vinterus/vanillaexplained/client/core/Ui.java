package dev.vinterus.vanillaexplained.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** Shared look: palette and small drawing helpers. */
public final class Ui {
	// vanilla tooltip look
	public static final int BG = 0xF0100010;
	public static final int BG_HEADER = 0xF0180A22;
	public static final int CARD = 0xFF1A0E24;
	public static final int CARD_HOVER = 0xFF2A1840;
	public static final int BORDER = 0xFF3C1C78;
	public static final int BORDER_TOP = 0x805000FF;
	public static final int BORDER_BOTTOM = 0x8028007F;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int MUTED = 0xFFAAAAAA;
	public static final int ACCENT = 0xFF55FFFF;
	public static final int OK = 0xFF55FF55;
	public static final int BAD = 0xFFFF5555;
	public static final int WARN = 0xFFFFFF55;

	private Ui() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	public static int withAlpha(int color, float a) {
		int alpha = (int) (((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	/** Rectangle with 1px rounded corners. */
	public static void round(Gfx g, int x, int y, int w, int h, int color) {
		g.fill(x + 1, y, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	/** Rounded panel with a border. */
	public static void panel(Gfx g, int x, int y, int w, int h, int bg, int border) {
		round(g, x, y, w, h, border);
		round(g, x + 1, y + 1, w - 2, h - 2, bg);
	}

	/** Shortens text to fit {@code w} pixels, adding an ellipsis. */
	public static FormattedCharSequence fit(Component c, int w) {
		Font f = font();
		if (f.width(c) <= w) {
			return c.getVisualOrderText();
		}
		String s = c.getString();
		while (s.length() > 1 && f.width(s + "…") > w) {
			s = s.substring(0, s.length() - 1);
		}
		return Component.literal(s.trim() + "…").withStyle(c.getStyle()).getVisualOrderText();
	}

	public static void text(Gfx g, Component c, int x, int y, int color) {
		g.text(c, x, y, color, true);
	}

	public static void text(Gfx g, FormattedCharSequence c, int x, int y, int color) {
		g.text(c, x, y, color, true);
	}

	/** Exactly the vanilla tooltip frame: dark body, purple gradient border. */
	public static void tooltip(Gfx g, int x, int y, int w, int h, float a) {
		int bg = withAlpha(BG, a), top = withAlpha(BORDER_TOP, a), bottom = withAlpha(BORDER_BOTTOM, a);
		g.fill(x, y - 1, x + w, y, bg);
		g.fill(x, y + h, x + w, y + h + 1, bg);
		g.fill(x, y, x + w, y + h, bg);
		g.fill(x - 1, y, x, y + h, bg);
		g.fill(x + w, y, x + w + 1, y + h, bg);
		g.fillGradient(x, y + 1, x + 1, y + h - 1, top, bottom);
		g.fillGradient(x + w - 1, y + 1, x + w, y + h - 1, top, bottom);
		g.fill(x, y, x + w, y + 1, top);
		g.fill(x, y + h - 1, x + w, y + h, bottom);
	}

	public static void scaledText(Gfx g, Component c, int x, int y, float scale, int color) {
		g.scaledText(c, x, y, scale, color);
	}

	/** A small pill-shaped label. Returns its width. */
	public static int pill(Gfx g, Component c, int x, int y, int color, boolean filled) {
		int w = font().width(c) + 8;
		if (filled) {
			round(g, x, y, w, 12, withAlpha(color, 0.25F));
		}
		round(g, x, y + 11, w, 1, withAlpha(color, filled ? 0.0F : 0.0F));
		text(g, c, x + 4, y + 2, color);
		return w;
	}

	/** Key cap like [G]. Returns its width. */
	public static int keycap(Gfx g, String key, int x, int y) {
		int w = Math.max(14, font().width(key) + 8);
		panel(g, x, y, w, 14, 0xFF2A2A3A, 0xFF55557A);
		g.text(key, x + (w - font().width(key)) / 2, y + 3, TEXT, true);
		return w;
	}

	/** An ItemStack, or EMPTY when items can't be created yet (26.x title screen: components not bound). */
	public static ItemStack stack(net.minecraft.world.item.Item item) {
		try {
			return new ItemStack(item);
		} catch (RuntimeException e) {
			return ItemStack.EMPTY;
		}
	}

	public static void item(Gfx g, ItemStack stack, int x, int y) {
		if (!stack.isEmpty()) {
			g.item(stack, x, y);
		}
	}

	/** Stat tile: icon, big value, small label. */
	public static void tile(Gfx g, int x, int y, int w, int h, ItemStack icon, Component value, Component label,
			int valueColor) {
		panel(g, x, y, w, h, CARD, BORDER);
		item(g, icon, x + 6, y + (h - 16) / 2);
		scaledText(g, value, x + 28, y + 7, 1.5F, valueColor);
		text(g, label, x + 28, y + h - 12, MUTED);
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	public static int kindColor(Report.Kind k) {
		return switch (k) {
			case OK -> OK;
			case BAD -> BAD;
			case WARN -> WARN;
			case INFO -> MUTED;
			case HEADER -> TEXT;
		};
	}
}
