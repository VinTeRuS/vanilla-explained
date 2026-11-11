package dev.vinterus.vanillaexplained.client.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/** What the "Why?" card shows: icon, title, a headline verdict and explained facts. */
public final class Report {
	public enum Kind {
		OK, BAD, WARN, INFO, HEADER
	}

	/** A fact. {@code icons} are drawn in a row before the text (used for groups of mobs). */
	public record Line(Kind kind, Component text, int indent, List<ItemStack> icons) {
	}

	private final Component title;
	private ItemStack icon = ItemStack.EMPTY;
	private Kind verdictKind;
	private Component verdict;
	private final List<Line> lines = new ArrayList<>();
	private Component tip;
	private final List<Line> brief = new ArrayList<>();
	private net.minecraft.core.BlockPos target;

	public Report(Component title) {
		this.title = title;
	}

	public Component title() {
		return title;
	}

	public ItemStack icon() {
		return icon;
	}

	public Report icon(ItemStack stack) {
		this.icon = stack;
		return this;
	}

	public Kind verdictKind() {
		return verdictKind;
	}

	public Component verdict() {
		return verdict;
	}

	/** Sets the headline; the first call wins unless {@code force}. */
	public Report verdict(Kind kind, Component text) {
		if (verdict == null) {
			verdictKind = kind;
			verdict = text;
		}
		return this;
	}

	public Report forceVerdict(Kind kind, Component text) {
		verdictKind = kind;
		verdict = text;
		return this;
	}

	/** Short facts for the compact view (Shift shows {@link #lines()}). */
	public List<Line> brief() {
		return brief;
	}

	public Report brief(Kind kind, Component text) {
		brief.add(new Line(kind, text, 0, List.of()));
		return this;
	}

	public Report briefIcons(Kind kind, List<ItemStack> icons, Component text) {
		brief.add(new Line(kind, text, 0, List.copyOf(icons)));
		return this;
	}

	/** The block this report is about; the card fades when the player looks away. */
	public net.minecraft.core.BlockPos target() {
		return target;
	}

	public Report target(net.minecraft.core.BlockPos pos) {
		this.target = pos;
		return this;
	}

	public Component tip() {
		return tip;
	}

	public Report tip(Component tip) {
		this.tip = tip;
		return this;
	}

	public List<Line> lines() {
		return lines;
	}

	public Report add(Kind kind, Component text) {
		lines.add(new Line(kind, text, 0, List.of()));
		return this;
	}

	public Report sub(Kind kind, Component text) {
		lines.add(new Line(kind, text, 1, List.of()));
		return this;
	}

	public Report header(Component text) {
		lines.add(new Line(Kind.HEADER, text, 0, List.of()));
		return this;
	}

	public Report icons(Kind kind, List<ItemStack> icons, Component text) {
		lines.add(new Line(kind, text, 0, List.copyOf(icons)));
		return this;
	}

	public static MutableComponent t(String key, Object... args) {
		return Component.translatable("vanillaexplained." + key, args);
	}
}
