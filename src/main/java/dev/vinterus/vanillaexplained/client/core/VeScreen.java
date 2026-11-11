package dev.vinterus.vanillaexplained.client.core;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Screen base that hides render/input signature changes between Minecraft versions. */
public abstract class VeScreen extends Screen {
	protected VeScreen(Component title) {
		super(title);
	}

	/** Panel background; the world stays visible (no blur or dimming). */
	protected abstract void drawBackground(Gfx g, int mouseX, int mouseY);

	protected abstract void drawContent(Gfx g, int mouseX, int mouseY);

	protected boolean click(double mouseX, double mouseY) {
		return false;
	}

	protected boolean scroll(double mouseX, double mouseY, double delta) {
		return false;
	}

	//? if >=26.1 {
	@Override
	public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		drawBackground(new Gfx(g), mouseX, mouseY);
	}

	@Override
	public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		drawContent(new Gfx(g), mouseX, mouseY);
		super.extractRenderState(g, mouseX, mouseY, a);
	}
	//?} else if >=1.20.2 {
	/*@Override
	public void renderBackground(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float a) {
		drawBackground(new Gfx(g), mouseX, mouseY);
	}

	@Override
	public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float a) {
		super.render(g, mouseX, mouseY, a);
		drawContent(new Gfx(g), mouseX, mouseY);
	}
	*///?} else {
	/*@Override
	public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float a) {
		drawBackground(new Gfx(g), mouseX, mouseY);
		super.render(g, mouseX, mouseY, a);
		drawContent(new Gfx(g), mouseX, mouseY);
	}
	*///?}

	//? if >=1.21.9 {
	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean doubleClick) {
		return click(e.x(), e.y()) || super.mouseClicked(e, doubleClick);
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return click(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
	}
	*///?}

	//? if >=1.20.2 {
	@Override
	public boolean mouseScrolled(double x, double y, double sx, double sy) {
		return scroll(x, y, sy) || super.mouseScrolled(x, y, sx, sy);
	}
	//?} else {
	/*@Override
	public boolean mouseScrolled(double x, double y, double delta) {
		return scroll(x, y, delta) || super.mouseScrolled(x, y, delta);
	}
	*///?}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
