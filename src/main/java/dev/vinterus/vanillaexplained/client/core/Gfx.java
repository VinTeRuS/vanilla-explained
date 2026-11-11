package dev.vinterus.vanillaexplained.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The only place that talks to the version-specific GUI drawing class. Everything else draws
 * through this, so porting between Minecraft versions touches one file.
 */
public final class Gfx {
	//? if >=26.1 {
	public final net.minecraft.client.gui.GuiGraphicsExtractor g;

	public Gfx(net.minecraft.client.gui.GuiGraphicsExtractor g) {
		this.g = g;
	}
	//?} else {
	/*public final net.minecraft.client.gui.GuiGraphics g;

	public Gfx(net.minecraft.client.gui.GuiGraphics g) {
		this.g = g;
	}
	*///?}

	private static Font font() {
		return Minecraft.getInstance().font;
	}

	public int guiWidth() {
		return g.guiWidth();
	}

	public int guiHeight() {
		return g.guiHeight();
	}

	public void fill(int x0, int y0, int x1, int y1, int color) {
		g.fill(x0, y0, x1, y1, color);
	}

	public void fillGradient(int x0, int y0, int x1, int y1, int top, int bottom) {
		g.fillGradient(x0, y0, x1, y1, top, bottom);
	}

	public void horizontalLine(int x0, int x1, int y, int color) {
		g.fill(Math.min(x0, x1), y, Math.max(x0, x1) + 1, y + 1, color);
	}

	public void text(FormattedCharSequence s, int x, int y, int color, boolean shadow) {
		//? if >=26.1 {
		g.text(font(), s, x, y, color, shadow);
		//?} else {
		/*g.drawString(font(), s, x, y, color, shadow);
		*///?}
	}

	public void text(Component s, int x, int y, int color, boolean shadow) {
		text(s.getVisualOrderText(), x, y, color, shadow);
	}

	public void text(String s, int x, int y, int color, boolean shadow) {
		text(Component.literal(s), x, y, color, shadow);
	}

	public void centeredText(Component s, int x, int y, int color) {
		text(s, x - font().width(s) / 2, y, color, true);
	}

	public void item(ItemStack stack, int x, int y) {
		//? if >=26.1 {
		g.item(stack, x, y);
		//?} else {
		/*g.renderItem(stack, x, y);
		*///?}
	}

	public void enableScissor(int x0, int y0, int x1, int y1) {
		g.enableScissor(x0, y0, x1, y1);
	}

	public void disableScissor() {
		g.disableScissor();
	}

	/** Draws text scaled around its top-left corner. */
	public void scaledText(Component s, int x, int y, float scale, int color) {
		//? if >=1.21.6 {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		text(s, 0, 0, color, true);
		g.pose().popMatrix();
		//?} else {
		/*g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1);
		text(s, 0, 0, color, true);
		g.pose().popPose();
		*///?}
	}

	public void tooltip(Component text, int x, int y) {
		//? if >=1.21.6 {
		g.setTooltipForNextFrame(text, x, y);
		//?} else {
		/*g.renderTooltip(font(), text, x, y);
		*///?}
	}
}
