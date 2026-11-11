package dev.vinterus.vanillaexplained.client.anvil;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Enchantment access across versions: data-driven holders (1.21+), components (1.20.5+) or NBT. */
public final class EnchCompat {
	/** One enchantment on a book. {@code key} is a Holder (1.20.5+) or an Enchantment (older). */
	public record Ench(Object key, Component name, int level, int bookFee) {
	}

	private EnchCompat() {
	}

	//? if >=1.20.5 {
	@SuppressWarnings("unchecked")
	private static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> h(Object o) {
		return (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>) o;
	}

	public static List<Ench> stored(ItemStack book) {
		List<Ench> out = new ArrayList<>();
		var ench = book.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
				net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
		for (var e : ench.entrySet()) {
			var holder = e.getKey();
			int level = e.getIntValue();
			//? if >=1.21 {
			Component name = net.minecraft.world.item.enchantment.Enchantment.getFullname(holder, level);
			//?} else {
			/*Component name = holder.value().getFullname(level);
			*///?}
			out.add(new Ench(holder, name, level, Math.max(1, holder.value().getAnvilCost() / 2)));
		}
		return out;
	}

	public static List<Object> onItem(ItemStack item) {
		return new ArrayList<>(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(item).keySet());
	}

	public static boolean canApply(Object key, ItemStack item) {
		return h(key).value().canEnchant(item);
	}

	public static boolean compatible(Object a, Object b) {
		//? if >=1.21 {
		return net.minecraft.world.item.enchantment.Enchantment.areCompatible(h(a), h(b));
		//?} else {
		/*return h(a).value().isCompatibleWith(h(b).value());
		*///?}
	}

	public static int repairCost(ItemStack s) {
		return s.getOrDefault(net.minecraft.core.component.DataComponents.REPAIR_COST, 0);
	}
	//?} else {
	/*public static List<Ench> stored(ItemStack book) {
		List<Ench> out = new ArrayList<>();
		var map = net.minecraft.world.item.enchantment.EnchantmentHelper
				.deserializeEnchantments(net.minecraft.world.item.EnchantedBookItem.getEnchantments(book));
		for (var e : map.entrySet()) {
			var ench = e.getKey();
			int fee = switch (ench.getRarity()) {
				case COMMON -> 1;
				case UNCOMMON -> 2;
				case RARE -> 4;
				case VERY_RARE -> 8;
			};
			out.add(new Ench(ench, ench.getFullname(e.getValue()), e.getValue(), Math.max(1, fee / 2)));
		}
		return out;
	}

	public static List<Object> onItem(ItemStack item) {
		return new ArrayList<>(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(item).keySet());
	}

	public static boolean canApply(Object key, ItemStack item) {
		return ((net.minecraft.world.item.enchantment.Enchantment) key).canEnchant(item);
	}

	public static boolean compatible(Object a, Object b) {
		return ((net.minecraft.world.item.enchantment.Enchantment) a).isCompatibleWith((net.minecraft.world.item.enchantment.Enchantment) b);
	}

	public static int repairCost(ItemStack s) {
		return s.getBaseRepairCost();
	}
	*///?}
}
