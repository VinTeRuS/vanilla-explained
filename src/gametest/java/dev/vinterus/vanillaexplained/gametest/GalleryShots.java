package dev.vinterus.vanillaexplained.gametest;

import dev.vinterus.vanillaexplained.client.VanillaExplainedClient;
import dev.vinterus.vanillaexplained.client.elytra.ElytraTracker;
import dev.vinterus.vanillaexplained.client.screen.CompanionScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.levelgen.Heightmap;

/** Store screenshots. Runs only with -Dve.gallery=true (VE_GALLERY=1). */
public class GalleryShots implements FabricClientGameTest {
	private static void look(ClientGameTestContext ctx, TestServerContext server, double px, double py, double pz, double tx, double ty, double tz) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f", px, py, pz));
		ctx.waitTicks(10);
		ctx.runOnClient(mc -> {
			var p = mc.player;
			double dx = tx - p.getX(), dy = ty - p.getEyeY(), dz = tz - p.getZ();
			float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
			float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
			p.setYRot(yaw);
			p.setXRot(pitch);
			p.yRotO = yaw;
			p.xRotO = pitch;
		});
		ctx.waitTicks(3);
	}

	/** Removes toasts and chat lines so they don't clutter the shot. */
	private static void tidy(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		ctx.waitTicks(2);
	}

	private static void shot(ClientGameTestContext ctx, String name) {
		tidy(ctx);
		ctx.takeScreenshot(name);
	}

	private static void why(ClientGameTestContext ctx) {
		ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
		ctx.waitTicks(12);
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (!Boolean.getBoolean("ve.gallery")) {
			return;
		}
		ctx.runOnClient(mc -> {
			mc.getWindow().setWindowed(1280, 720);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		ctx.waitTicks(5);
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			var server = sp.getServer();
			int y = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			String Y = Integer.toString(y), Ym1 = Integer.toString(y - 1);
			for (String c : new String[] { "difficulty hard", "time set 6000", "weather clear", "gamerule advance_time false",
					"gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule show_advancement_messages false",
					"gamemode survival @p", "clear @p" }) {
				server.runCommand(c);
			}
			// dark room with a doorway, 28 blocks from the viewpoint
			server.runCommand("fill 40 " + Ym1 + " -5 50 " + (y + 4) + " 5 stone_bricks hollow");
			server.runCommand("fill 40 " + Y + " -1 40 " + (y + 1) + " 1 air");
			// farm: rows in both directions + water
			server.runCommand("fill -14 " + Ym1 + " -3 -6 " + Ym1 + " 3 farmland[moisture=7]");
			server.runCommand("fill -14 " + Ym1 + " 0 -6 " + Ym1 + " 0 water");
			server.runCommand("fill -14 " + Y + " -3 -6 " + Y + " -1 wheat[age=4]");
			server.runCommand("fill -14 " + Y + " 1 -6 " + Y + " 3 wheat[age=4]");
			// piston blocked by obsidian
			server.runCommand("setblock 0 " + Y + " -18 piston[facing=east]");
			server.runCommand("fill 1 " + Y + " -18 3 " + Y + " -18 oak_planks");
			server.runCommand("setblock 4 " + Y + " -18 obsidian");
			server.runCommand("setblock -1 " + Y + " -18 lever[face=floor,facing=east]");
			// enchanting table with too few shelves and a torch in the gap
			server.runCommand("fill -22 " + Y + " -22 -18 " + Y + " -18 bookshelf");
			server.runCommand("fill -21 " + Y + " -21 -19 " + Y + " -19 air");
			server.runCommand("fill -22 " + Y + " -18 -18 " + Y + " -18 air");
			server.runCommand("setblock -20 " + Y + " -20 enchanting_table");
			server.runCommand("setblock -21 " + Y + " -20 torch");
			// anvil + items
			server.runCommand("setblock 10 " + Y + " 10 anvil");
			server.runCommand("give @p diamond_sword");
			server.runCommand("give @p enchanted_book[stored_enchantments={sharpness:5}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={unbreaking:3}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={mending:1}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={looting:3}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={sweeping_edge:3}]");
			server.runCommand("give @p firework_rocket[fireworks={flight_duration:2}] 64");
			server.runCommand("item replace entity @p armor.chest with elytra[enchantments={unbreaking:3}]");
			ctx.waitTicks(40);

			server.runCommand("time set 18000");
			server.runCommand("gamerule spawn_mobs true");
			look(ctx, server, 16.5, y, 0.5, 44.5, y, 0.5);
			why(ctx);
			shot(ctx, "gallery_1_spawn");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("time set 6000");

			look(ctx, server, -10.5, y + 1, 5.5, -10.5, y + 0.2, 2.5);
			why(ctx);
			ctx.getInput().holdShift();
			ctx.waitTicks(6);
			shot(ctx, "gallery_2_crop_details");
			ctx.getInput().releaseShift();

			look(ctx, server, -1.5, y, -15.5, 0.5, y + 0.5, -17.5);
			why(ctx);
			shot(ctx, "gallery_3_piston");

			look(ctx, server, -20.5, y + 1, -15.5, -19.5, y + 0.6, -19.5);
			why(ctx);
			shot(ctx, "gallery_4_enchanting");

			ctx.runOnClient(mc -> ElytraTracker.setTarget(new ElytraTracker.Target(2400, -800, "minecraft:overworld")));
			look(ctx, server, 0.5, y, 4.5, 30, y + 12, 4.5);
			CompanionScreen.selectTab(0);
			ctx.setScreen(CompanionScreen::new);
			ctx.waitTicks(10);
			shot(ctx, "gallery_5_planner_elytra");
			CompanionScreen.selectTab(2);
			ctx.setScreen(CompanionScreen::new);
			ctx.waitTicks(10);
			shot(ctx, "gallery_6_planner_redstone");
			ctx.setScreen(() -> null);

			look(ctx, server, 10.5, y, 12.5, 10.5, y + 0.5, 10.5);
			ctx.getInput().pressKey(o -> o.keyUse);
			ctx.waitForScreen(AnvilScreen.class);
			ctx.runOnClient(mc -> {
				var menu = ((AnvilScreen) mc.gui.screen()).getMenu();
				for (int i = 3; i < menu.slots.size(); i++) {
					if (menu.getSlot(i).getItem().is(net.minecraft.world.item.Items.DIAMOND_SWORD)) {
						mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
						break;
					}
				}
			});
			ctx.waitTicks(10);
			ctx.clickScreenButton("vanillaexplained.anvil.button");
			ctx.waitTicks(10);
			shot(ctx, "gallery_7_anvil");
			ctx.setScreen(() -> null);

			ctx.runOnClient(mc -> {
				mc.options.languageCode = "ru_ru";
				mc.getLanguageManager().setSelected("ru_ru");
				mc.reloadResourcePacks();
			});
			ctx.waitTicks(100);
			server.runCommand("time set 18000");
			server.runCommand("gamerule spawn_mobs true");
			look(ctx, server, 16.5, y, 0.5, 44.5, y, 0.5);
			why(ctx);
			shot(ctx, "gallery_8_spawn_ru");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("time set 6000");
			CompanionScreen.selectTab(3);
			ctx.setScreen(CompanionScreen::new);
			ctx.waitTicks(10);
			shot(ctx, "gallery_9_secrets_ru");
			ctx.setScreen(() -> null);
			ctx.runOnClient(mc -> {
				mc.options.languageCode = "en_us";
				mc.getLanguageManager().setSelected("en_us");
				mc.reloadResourcePacks();
			});
			ctx.waitTicks(60);
			System.out.println("[VE-GALLERY] DONE");
		}
	}
}
