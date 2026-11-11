package dev.vinterus.vanillaexplained.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.vinterus.vanillaexplained.client.VanillaExplainedClient;
import dev.vinterus.vanillaexplained.client.anvil.AnvilHelper;
import dev.vinterus.vanillaexplained.client.core.Report;
import dev.vinterus.vanillaexplained.client.core.ReportHud;
import dev.vinterus.vanillaexplained.client.inspect.CropInspector;
import dev.vinterus.vanillaexplained.client.inspect.SpawnInspector;
import dev.vinterus.vanillaexplained.client.portal.PortalInspector;
import dev.vinterus.vanillaexplained.client.redstone.RedstoneInspector;
import dev.vinterus.vanillaexplained.client.screen.CompanionScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class InGameTest implements FabricClientGameTest {
	private static final String P = "vanillaexplained.";

	/** key -> args of every translatable line in a report */
	private static List<Object[]> find(Report r, String key) {
		List<Object[]> out = new ArrayList<>();
		if (r.verdict() != null && r.verdict().getContents() instanceof TranslatableContents vc && vc.getKey().equals(P + key)) {
			out.add(vc.getArgs());
		}
		for (Report.Line l : r.lines()) {
			if (l.text().getContents() instanceof TranslatableContents tc && tc.getKey().equals(P + key)) {
				out.add(tc.getArgs());
			}
		}
		return out;
	}

	private static String dump(Report r) {
		StringBuilder sb = new StringBuilder(r.title().getString());
		if (r.verdict() != null) {
			sb.append("\n  VERDICT ").append(r.verdictKind()).append(' ').append(r.verdict().getString());
		}
		for (Report.Line l : r.lines()) {
			sb.append("\n  ").append(l.kind()).append(' ').append(l.text().getString());
		}
		return sb.toString();
	}

	private static Object[] expect(Report r, String key) {
		List<Object[]> f = find(r, key);
		if (f.isEmpty()) {
			throw new AssertionError("expected line '" + key + "' in report:\n" + dump(r));
		}
		System.out.println("[VE-TEST] OK " + key + " " + Arrays.toString(f.get(0)));
		return f.get(0);
	}

	private static void expectArg(Report r, String key, int index, Object value) {
		Object[] args = expect(r, key);
		String got = args[index] instanceof Component c ? c.getString() : String.valueOf(args[index]);
		if (!got.equals(String.valueOf(value))) {
			throw new AssertionError(key + " arg " + index + " expected " + value + " got " + got + " in:\n" + dump(r));
		}
	}

	private static void log(Report r) {
		System.out.println("[VE-TEST] " + dump(r));
	}

	/** Teleports the player and points the camera at a world point (client-side rotation). */
	private static void lookAt(ClientGameTestContext ctx, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server,
			double px, double py, double pz, double tx, double ty, double tz) {
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

	/** Clicks the n-th tab of the open planner panel (same geometry as CompanionScreen). */
	private static void clickTab(net.minecraft.client.Minecraft mc, int index) {
		var screen = mc.gui.screen();
		int pw = Math.min(Math.max(236, Math.min(280, (int) (screen.width * 0.42))), screen.width - 8);
		int ph = Math.min(260, screen.height - 16);
		int left = screen.width - pw - 8, top = (screen.height - ph) / 2;
		double x = left + 10 + index * 28 + 12, yy = top + 6 + 12;
		screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, yy, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			var server = sp.getServer();
			int y = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
			System.out.println("[VE-TEST] surface y=" + y);
			server.runCommand("difficulty hard");
			server.runCommand("time set midnight");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("tp @p 0 " + y + " 0 -90 20");
			String Y = Integer.toString(y), Ym1 = Integer.toString(y - 1);

			// 1. dark box 31 blocks away
			server.runCommand("fill 28 " + Ym1 + " -3 34 " + (y + 3) + " 3 stone hollow");
			// 2. crops with both-axis neighbours
			server.runCommand("fill -12 " + Ym1 + " -1 -8 " + Ym1 + " 1 farmland[moisture=7]");
			server.runCommand("setblock -10 " + Ym1 + " 2 water");
			server.runCommand("setblock -10 " + Y + " 0 wheat[age=2]");
			server.runCommand("setblock -11 " + Y + " 0 wheat");
			server.runCommand("setblock -10 " + Y + " 1 wheat");
			// 3. redstone line
			server.runCommand("setblock 0 " + Y + " -10 redstone_block");
			server.runCommand("fill 1 " + Y + " -10 6 " + Y + " -10 redstone_wire");
			// pistons
			server.runCommand("setblock 0 " + Y + " -15 piston[facing=east]");
			server.runCommand("fill 1 " + Y + " -15 13 " + Y + " -15 stone");
			server.runCommand("setblock 0 " + Y + " -18 piston[facing=east]");
			server.runCommand("setblock 1 " + Y + " -18 stone");
			server.runCommand("setblock 2 " + Y + " -18 obsidian");
			server.runCommand("setblock 0 " + Y + " -26 piston[facing=east]");
			server.runCommand("setblock 0 " + (y + 2) + " -26 redstone_block");
			// chest + comparator
			server.runCommand("setblock 0 " + Y + " -22 chest");
			server.runCommand("item replace block 0 " + Y + " -22 container.0 with stone 64");
			server.runCommand("item replace block 0 " + Y + " -22 container.1 with stone 64");
			server.runCommand("setblock 1 " + Y + " -22 comparator[facing=west]");
			// enchanting table
			server.runCommand("fill -22 " + Y + " -22 -18 " + (y + 1) + " -18 bookshelf");
			server.runCommand("fill -21 " + Y + " -21 -19 " + (y + 1) + " -19 air");
			server.runCommand("setblock -20 " + Y + " -20 enchanting_table");
			// beacon
			server.runCommand("fill -30 " + Ym1 + " -1 -28 " + Ym1 + " 1 iron_block");
			server.runCommand("setblock -29 " + Y + " 0 beacon");
			// portal
			server.runCommand("fill 20 " + Ym1 + " -30 23 " + (y + 3) + " -30 obsidian");
			server.runCommand("fill 21 " + Y + " -30 22 " + (y + 2) + " -30 nether_portal[axis=x]");
			// anvil books
			server.runCommand("clear @p");
			server.runCommand("give @p enchanted_book[stored_enchantments={sharpness:5}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={unbreaking:3}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={mending:1}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={looting:3}]");
			server.runCommand("give @p enchanted_book[stored_enchantments={protection:4}]");
			ctx.waitTicks(40);

			// ---- spawn (server-side exact) ----
			BlockPos box = new BlockPos(31, y, 0);
			Vec3 player = ctx.computeOnClient(mc -> mc.player.position());
			Report spawn = server.computeOnServer(s -> SpawnInspector.inspectServer(s.overworld(), box, player));
			log(spawn);
			expectArg(spawn, "spawn.light", 2, "100%");
			expect(spawn, "spawn.distance.ok");
			expect(spawn, "spawn.group.can");
			server.runCommand("setblock 32 " + Y + " 0 glowstone");
			ctx.waitTicks(10);
			Report lit = server.computeOnServer(s -> SpawnInspector.inspectServer(s.overworld(), box, player));
			log(lit);
			expect(lit, "spawn.light.block_limit");
			Report close = ctx.computeOnClient(mc -> SpawnInspector.inspectClient(mc.level, new BlockPos(3, y, 0), player));
			log(close);
			expect(close, "spawn.distance.too_close");

			// ---- crops ----
			Report crop = ctx.computeOnClient(mc -> CropInspector.inspect(mc.level, new BlockPos(-10, y, 0), 3));
			log(crop);
			expect(crop, "crop.soil.wet");
			expect(crop, "crop.penalty.both");
			expectArg(crop, "crop.speed", 1, 6);
			expect(crop, "crop.eta");

			// ---- redstone ----
			Report wire = ctx.computeOnClient(mc -> RedstoneInspector.inspect(mc.level, new BlockPos(4, y, -10), false));
			log(wire);
			expectArg(wire, "rs.wire.power", 0, 12);
			Report tooMany = ctx.computeOnClient(mc -> RedstoneInspector.inspect(mc.level, new BlockPos(0, y, -15), false));
			log(tooMany);
			expect(tooMany, "rs.piston.cant_push");
			Report obsidian = ctx.computeOnClient(mc -> RedstoneInspector.inspect(mc.level, new BlockPos(0, y, -18), false));
			log(obsidian);
			Object why = expect(obsidian, "rs.piston.cant_push")[0];
			if (!(why instanceof Component c && c.getContents() instanceof TranslatableContents tc && tc.getKey().equals(P + "rs.piston.immovable"))) {
				throw new AssertionError("expected obsidian to be named as the blocker: " + dump(obsidian));
			}
			Report qc = ctx.computeOnClient(mc -> RedstoneInspector.inspect(mc.level, new BlockPos(0, y, -26), false));
			log(qc);
			expect(qc, "rs.piston.qc");
			Report chest = server.computeOnServer(s -> RedstoneInspector.inspect(s.overworld(), new BlockPos(0, y, -22), true));
			log(chest);
			expectArg(chest, "rs.analog.reads", 0, 2);
			Report comp = server.computeOnServer(s -> RedstoneInspector.inspect(s.overworld(), new BlockPos(1, y, -22), true));
			log(comp);
			expectArg(comp, "rs.diode.input", 0, 2);
			expectArg(comp, "rs.comparator.out_cmp", 0, 2);
			Report ench = ctx.computeOnClient(mc -> RedstoneInspector.inspect(mc.level, new BlockPos(-20, y, -20), false));
			log(ench);
			expectArg(ench, "ench.top", 0, "30");
			Report beacon = ctx.computeOnClient(mc -> RedstoneInspector.inspect(mc.level, new BlockPos(-29, y, 0), false));
			log(beacon);
			expectArg(beacon, "beacon.levels", 0, 1);
			expect(beacon, "beacon.sky");

			// ---- portal ----
			Report portal = ctx.computeOnClient(mc -> PortalInspector.inspect(mc.level, new BlockPos(21, y, -30)));
			log(portal);
			expect(portal, "portal.dest.nether");

			// ---- anvil planner ----
			List<Component> plan = ctx.computeOnClient(mc -> AnvilHelper.plan(new ItemStack(Items.DIAMOND_SWORD)));
			plan.forEach(line -> System.out.println("[VE-TEST] anvil: " + line.getString()));
			if (!(plan.get(0).getContents() instanceof TranslatableContents tc0 && tc0.getKey().equals(P + "anvil.total"))) {
				throw new AssertionError("anvil plan failed: " + plan.get(0).getString());
			}

			// ---- real key press + HUD screenshots ----
			server.runCommand("gamerule spawn_mobs true");
			lookAt(ctx, server, -7, y, 0.5, -9.5, y + 0.1, 0.5);
			ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
			ctx.waitTicks(5);
			if (ReportHud.current() == null) {
				throw new AssertionError("G key did not show a report");
			}
			ctx.takeScreenshot("ve_hud_crop");
			lookAt(ctx, server, 4.5, y, -7.5, 4.5, y + 0.02, -9.5);
			ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
			ctx.waitTicks(10);
			ctx.takeScreenshot("ve_hud_redstone");
			server.runCommand("setblock 32 " + Y + " 0 air");
			server.runCommand("setblock 28 " + Y + " 0 air");
			server.runCommand("setblock 28 " + (y + 1) + " 0 air");
			lookAt(ctx, server, 27.5, y, 0.5, 31.5, y - 0.5, 0.5);
			ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
			ctx.waitTicks(10);
			ctx.takeScreenshot("ve_hud_spawn");

			ctx.setScreen(CompanionScreen::new);
			ctx.waitTicks(5);
			ctx.takeScreenshot("ve_screen_elytra");
			ctx.runOnClient(mc -> clickTab(mc, 2));
			ctx.waitTicks(5);
			ctx.takeScreenshot("ve_screen_redstone");
			ctx.runOnClient(mc -> clickTab(mc, 1));
			ctx.waitTicks(5);
			ctx.takeScreenshot("ve_screen_portals");
			ctx.runOnClient(mc -> clickTab(mc, 3));
			ctx.waitTicks(5);
			ctx.takeScreenshot("ve_screen_tips");
			ctx.runOnClient(mc -> clickTab(mc, 4));
			ctx.waitTicks(5);
			ctx.takeScreenshot("ve_screen_help");
			ctx.runOnClient(mc -> clickTab(mc, 0));
			ctx.waitTicks(2);
			ctx.setScreen(() -> null);
			lookAt(ctx, server, 0.5, y, -13.5, 0.5, y + 0.5, -15.5);
			ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
			ctx.waitTicks(10);
			ctx.takeScreenshot("ve_hud_piston");
			lookAt(ctx, server, -20.5, y, -18.5, -19.5, y + 0.5, -19.5);
			ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
			ctx.waitTicks(10);
			ctx.takeScreenshot("ve_hud_enchanting");
			ctx.setScreen(() -> null);

			// Russian rendering check
			ctx.runOnClient(mc -> {
				mc.options.languageCode = "ru_ru";
				mc.getLanguageManager().setSelected("ru_ru");
				mc.reloadResourcePacks();
			});
			ctx.waitTicks(100);
			lookAt(ctx, server, 27.5, y, 0.5, 31.5, y - 0.5, 0.5);
			ctx.getInput().pressKey(VanillaExplainedClient.whyKey());
			ctx.waitTicks(10);
			ctx.takeScreenshot("ve_hud_spawn_ru");
			ctx.setScreen(CompanionScreen::new);
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> clickTab(mc, 3));
			ctx.waitTicks(5);
			ctx.takeScreenshot("ve_screen_ru");
			ctx.setScreen(() -> null);
			ctx.runOnClient(mc -> {
				mc.options.languageCode = "en_us";
				mc.getLanguageManager().setSelected("en_us");
				mc.reloadResourcePacks();
			});
			ctx.waitTicks(60);
			System.out.println("[VE-TEST] ALL PASSED");
		}
	}
}
