package dev.vinterus.vanillaexplained.client.elytra;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import net.minecraft.client.Minecraft;
import dev.vinterus.vanillaexplained.client.core.Gfx;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Small top-center guidance line while flying toward a target. */
public final class ElytraHud {
	private ElytraHud() {
	}

	public static void render(Gfx g, float pt) {
		Minecraft mc = Minecraft.getInstance();
		var p = mc.player;
		var target = ElytraTracker.target();
		if (p == null || target == null || !p.isFallFlying()
				|| !target.dim().equals(dev.vinterus.vanillaexplained.client.core.Mc.dimId(p.level()))) {
			return;
		}
		double dx = target.x() + 0.5 - p.getX();
		double dz = target.z() + 0.5 - p.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		var est = ElytraTracker.estimateTo(p, dist);
		// Minecraft yaw: 0 = +Z (south), 90 = -X (west)
		float wanted = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		float turn = Mth.wrapDegrees(wanted - p.getYRot());
		String arrow = Math.abs(turn) < 5 ? "↑" : turn < 0 ? "↖ " + Math.round(-turn) + "°" : "↗ " + Math.round(turn) + "°";

		Component line1 = t("elytra.hud.line", arrow, Math.round(dist), est.rockets(), Math.round(est.seconds()));
		boolean bad = !est.enoughRockets() || !est.enoughDurability();
		Component warn = !est.enoughRockets() ? t("elytra.hud.low_rockets", ElytraTracker.countRockets(p))
				: t("elytra.hud.low_durability");
		int w = Math.max(mc.font.width(line1), bad ? mc.font.width(warn) : 0) + 34;
		int h = bad ? 32 : 22;
		int x = (g.guiWidth() - w) / 2;
		int y = 18;
		dev.vinterus.vanillaexplained.client.core.Ui.panel(g, x, y, w, h, dev.vinterus.vanillaexplained.client.core.Ui.BG,
				bad ? dev.vinterus.vanillaexplained.client.core.Ui.BAD : dev.vinterus.vanillaexplained.client.core.Ui.BORDER);
		g.item(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET), x + 4, y + 3);
		g.text(line1, x + 26, y + 7, 0xFFFFFFFF, true);
		if (bad) {
			g.text(warn, x + 26, y + 19, dev.vinterus.vanillaexplained.client.core.Ui.BAD, true);
		}
	}
}
