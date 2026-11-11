package dev.vinterus.vanillaexplained.client.core;

import java.util.List;

import dev.vinterus.vanillaexplained.client.screen.CompanionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dev-only check for versions without the Fabric client gametest API. Run the client with
 * {@code -Dve.smoke=true}: on the title screen it opens every planner tab plus a sample "Why?"
 * tooltip, saves screenshots and quits. Any rendering crash shows up in the log.
 */
public final class SmokeTest {
	public static final boolean ENABLED = Boolean.getBoolean("ve.smoke");
	private static int state = -1;
	private static int ticks;

	private SmokeTest() {
	}

	public static void tick(Minecraft mc) {
		if (!ENABLED) {
			return;
		}
		if (state < 0) {
			if (Mc.screen() instanceof TitleScreen) {
				state = 0;
				ticks = 0;
				Report r = new Report(Report.t("crop.title", Component.literal("Wheat"))).icon(Ui.stack(Items.WHEAT))
						.verdict(Report.Kind.WARN, Report.t("crop.verdict.half"))
						.brief(Report.Kind.BAD, Report.t("crop.brief.penalty"))
						.brief(Report.Kind.INFO, Report.t("crop.brief.eta", 6, 34))
						.briefIcons(Report.Kind.OK, List.of(Ui.stack(Items.ZOMBIE_HEAD), Ui.stack(Items.CREEPER_HEAD)),
								Report.t("spawn.group.can", "100%"))
						.tip(Tips.next("crop"));
				r.add(Report.Kind.OK, Report.t("crop.light.ok", 15));
				ReportHud.show(r);
				CompanionScreen.selectTab(2);
				Mc.setScreen(new CompanionScreen());
			}
			return;
		}
		ticks++;
		ReportHud.show(ReportHud.current() != null ? ReportHud.current() : new Report(Component.literal("smoke")));
		if (ticks % 30 == 0) {
			//? if >=26.2 {
			Screenshot.grab(mc, false);
			//?} else {
			/*Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), msg -> {
			});
			*///?}
			System.out.println("[VE-SMOKE] screenshot tab " + state);
			state++;
			if (state > 4) {
				System.out.println("[VE-SMOKE] OK");
				mc.stop();
				return;
			}
			CompanionScreen.selectTab(state == 3 ? 3 : state == 4 ? 4 : state == 1 ? 1 : 0);
			Mc.setScreen(new CompanionScreen());
		}
	}

	/** Draws the sample tooltip over the planner during the smoke run. */
	public static void renderOverScreen(Gfx g) {
		if (ENABLED && state >= 0) {
			ReportHud.render(g, 0F, true);
		}
	}
}
