package dev.vinterus.vanillaexplained.client.anvil;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import java.util.ArrayList;
import java.util.List;

import dev.vinterus.vanillaexplained.rules.AnvilPlanner;
import net.minecraft.client.Minecraft;
import dev.vinterus.vanillaexplained.client.core.Gfx;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Adds a "Plan" button to the anvil: takes the item in the first slot and every enchanted book
 * in the inventory that fits it, and shows the cheapest combining order.
 */
public final class AnvilHelper {
	private static List<Component> panel = List.of();

	private AnvilHelper() {
	}

	public static void onInit(Screen screen, int w, int h, java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> addWidget) {
		if (!(screen instanceof AnvilScreen anvil)) {
			return;
		}
		panel = List.of();
		int x = (w - 176) / 2 + 176 + 4;
		int y = (h - 166) / 2;
		addWidget.accept(Button.builder(t("anvil.button"), b -> panel = plan(anvil.getMenu().getSlot(0).getItem())).bounds(x, y, 80, 20).build());
	}

	public static void render(Screen screen, Gfx g) {
		if (!(screen instanceof AnvilScreen) || panel.isEmpty()) {
			return;
		}
		var font = Minecraft.getInstance().font;
		int x = (screen.width - 176) / 2 + 176 + 4;
		int y = (screen.height - 166) / 2 + 24;
		int w = Math.min(220, screen.width - x - 6);
		if (w < 120) {
			return;
		}
		int inner = w - 16;
		// first line is the header (total or error), last line is the note
		Component head = panel.get(0);
		List<Component> steps = panel.size() > 2 ? panel.subList(1, panel.size() - 1) : List.of();
		Component note = panel.size() > 1 ? panel.get(panel.size() - 1) : Component.empty();
		int h = 30;
		for (Component c : steps) {
			h += Math.max(1, font.split(c, inner - 34).size()) * 10 + 8;
		}
		List<FormattedCharSequence> noteRows = font.split(note, inner);
		h += noteRows.size() * 10 + 8;
		dev.vinterus.vanillaexplained.client.core.Ui.panel(g, x, y, w, h, dev.vinterus.vanillaexplained.client.core.Ui.BG,
				dev.vinterus.vanillaexplained.client.core.Ui.BORDER);
		dev.vinterus.vanillaexplained.client.core.Ui.round(g, x + 1, y + 1, w - 2, 23, dev.vinterus.vanillaexplained.client.core.Ui.BG_HEADER);
		g.item(new ItemStack(Items.ANVIL), x + 6, y + 4);
		boolean ok = steps.size() > 0;
		g.text(head, x + 26, y + 9, ok ? dev.vinterus.vanillaexplained.client.core.Ui.OK : dev.vinterus.vanillaexplained.client.core.Ui.WARN, true);
		int cy = y + 30;
		int n = 1;
		for (Component c : steps) {
			// number badge
			dev.vinterus.vanillaexplained.client.core.Ui.round(g, x + 8, cy - 1, 14, 12, dev.vinterus.vanillaexplained.client.core.Ui.CARD_HOVER);
			g.centeredText(Component.literal(Integer.toString(n++)), x + 15, cy + 1, dev.vinterus.vanillaexplained.client.core.Ui.ACCENT);
			for (var row : font.split(c, inner - 34)) {
				g.text(row, x + 28, cy + 1, dev.vinterus.vanillaexplained.client.core.Ui.TEXT, true);
				cy += 10;
			}
			cy += 8;
		}
		for (var row : noteRows) {
			g.text(row, x + 8, cy, dev.vinterus.vanillaexplained.client.core.Ui.MUTED, true);
			cy += 10;
		}
	}

	private static int work(ItemStack s) {
		int rc = EnchCompat.repairCost(s);
		return 31 - Integer.numberOfLeadingZeros(rc + 1); // repair cost is 2^work - 1
	}

	/** Builds the plan panel for {@code item} using the enchanted books in the player's inventory. */
	public static List<Component> plan(ItemStack item) {
		var mc = Minecraft.getInstance();
		List<Component> out = new ArrayList<>();
		if (item.isEmpty() || item.is(Items.ENCHANTED_BOOK)) {
			out.add(t("anvil.put_item"));
			return out;
		}
		List<Object> taken = EnchCompat.onItem(item);
		List<AnvilPlanner.Book> books = new ArrayList<>();
		int skipped = 0;
		var inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.is(Items.ENCHANTED_BOOK)) {
				continue;
			}
			List<EnchCompat.Ench> ench = EnchCompat.stored(s);
			boolean ok = !ench.isEmpty();
			int value = 0;
			for (EnchCompat.Ench e : ench) {
				if (!EnchCompat.canApply(e.key(), item) || taken.contains(e.key())) {
					ok = false;
					break;
				}
				for (Object other : taken) {
					if (!EnchCompat.compatible(e.key(), other)) {
						ok = false;
						break;
					}
				}
				value += e.bookFee() * e.level();
			}
			if (!ok || books.size() >= AnvilPlanner.MAX_BOOKS) {
				skipped++;
				continue;
			}
			for (EnchCompat.Ench e : ench) {
				taken.add(e.key());
			}
			String text = ench.get(0).name().getString() + (ench.size() > 1 ? " +" + (ench.size() - 1) : "");
			books.add(new AnvilPlanner.Book(text, value, work(s)));
		}
		if (books.isEmpty()) {
			out.add(t("anvil.no_books"));
			return out;
		}
		boolean creative = dev.vinterus.vanillaexplained.client.core.Mc.creative(mc.player);
		var plan = AnvilPlanner.plan(item.getHoverName().getString(), work(item), books,
				creative ? Integer.MAX_VALUE : AnvilPlanner.TOO_EXPENSIVE);
		if (plan == null) {
			out.add(t("anvil.impossible"));
			return out;
		}
		out.add(t("anvil.total", plan.totalCost(), plan.steps().size()));
		String itemName = item.getHoverName().getString();
		for (var step : plan.steps()) {
			String left = step.left().get(0).equals(itemName) ? itemName : String.join(" + ", step.left());
			out.add(t("anvil.step", left,
					String.join(" + ", step.right()), step.cost()));
		}
		out.add((skipped > 0 ? t("anvil.skipped", skipped).append(" ").append(t("anvil.note")) : t("anvil.note"))
				.append("\n").append(t("tip.header")).append(" ").append(dev.vinterus.vanillaexplained.client.core.Tips.next("anvil")));
		return out;
	}
}
