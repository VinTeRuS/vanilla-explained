package dev.vinterus.vanillaexplained.client;

import static dev.vinterus.vanillaexplained.client.core.Report.t;

import com.mojang.blaze3d.platform.InputConstants;

import dev.vinterus.vanillaexplained.client.anvil.AnvilHelper;
import dev.vinterus.vanillaexplained.client.core.Gfx;
import dev.vinterus.vanillaexplained.client.core.Mc;
import dev.vinterus.vanillaexplained.client.core.Report;
import dev.vinterus.vanillaexplained.client.core.ReportHud;
import dev.vinterus.vanillaexplained.client.elytra.ElytraHud;
import dev.vinterus.vanillaexplained.client.elytra.ElytraTracker;
import dev.vinterus.vanillaexplained.client.inspect.CropInspector;
import dev.vinterus.vanillaexplained.client.inspect.SpawnInspector;
import dev.vinterus.vanillaexplained.client.portal.PortalInspector;
import dev.vinterus.vanillaexplained.client.portal.PortalMemory;
import dev.vinterus.vanillaexplained.client.redstone.RedstoneInspector;
import dev.vinterus.vanillaexplained.client.screen.CompanionScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Loader-independent core. Fabric and Forge entry points forward their events here. */
public final class VanillaExplainedClient {
	public static final String MOD_ID = "vanillaexplained";

	private static KeyMapping whyKey;
	private static KeyMapping companionKey;
	private static int flushCountdown;

	public static KeyMapping whyKey() {
		return whyKey;
	}

	/** Creates the two key mappings; the loader registers them. */
	public static KeyMapping[] createKeys() {
		whyKey = Mc.keyMapping("key.vanillaexplained.why", InputConstants.KEY_G);
		companionKey = Mc.keyMapping("key.vanillaexplained.companion", InputConstants.KEY_J);
		return new KeyMapping[] { whyKey, companionKey };
	}

	public static void renderHud(Gfx g, float partialTick) {
		ReportHud.render(g, partialTick);
		ElytraHud.render(g, partialTick);
	}

	public static void onChunkLoad(net.minecraft.world.level.Level level, net.minecraft.world.level.chunk.LevelChunk chunk) {
		PortalMemory.onChunkLoad(Mc.dimId(level), chunk);
	}

	public static void onDisconnect() {
		PortalMemory.flush();
		ReportHud.hide();
		ElytraTracker.setTarget(null);
	}

	public static void onScreenInit(net.minecraft.client.gui.screens.Screen screen, int w, int h,
			java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> addWidget) {
		AnvilHelper.onInit(screen, w, h, addWidget);
	}

	public static void onScreenRender(net.minecraft.client.gui.screens.Screen screen, Gfx g) {
		AnvilHelper.render(screen, g);
		dev.vinterus.vanillaexplained.client.core.SmokeTest.renderOverScreen(g);
	}

	public static void tick(Minecraft mc) {
		dev.vinterus.vanillaexplained.client.core.SmokeTest.tick(mc);
		ReportHud.tick();
		ElytraTracker.tick(mc);
		while (whyKey.consumeClick()) {
			inspect(mc);
		}
		while (companionKey.consumeClick()) {
			if (mc.player != null) {
				Mc.setScreen(new CompanionScreen());
			}
		}
		if (++flushCountdown >= 20 * 60 * 2) {
			flushCountdown = 0;
			PortalMemory.flush();
		}
	}

	private static void inspect(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		HitResult hit = Mc.lookTarget(mc);
		if (!(hit instanceof BlockHitResult bhr) || hit.getType() == HitResult.Type.MISS) {
			if (ReportHud.visible()) {
				ReportHud.hide();
			} else {
				ReportHud.show(new Report(t("title")).icon(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPYGLASS))
						.verdict(Report.Kind.INFO, t("look_at_block")));
			}
			return;
		}
		BlockPos pos = bhr.getBlockPos();
		var level = mc.level;
		if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
			ReportHud.show(PortalInspector.inspect(level, pos).target(pos));
			return;
		}
		BlockPos crop = CropInspector.cropAt(level, pos);
		var server = mc.getSingleplayerServer();
		if (crop != null) {
			int rts = -1;
			if (server != null) {
				var sl = server.getLevel(level.dimension());
				if (sl != null) {
					rts = Mc.randomTickSpeed(sl);
				}
			}
			ReportHud.show(CropInspector.inspect(level, crop, rts).target(pos));
			return;
		}
		if (RedstoneInspector.handles(level.getBlockState(pos))) {
			if (server != null) {
				var dim = level.dimension();
				server.submit(() -> {
					var sl = server.getLevel(dim);
					return sl == null ? null : RedstoneInspector.inspect(sl, pos, true);
				}).thenAccept(report -> {
					if (report != null) {
						mc.execute(() -> ReportHud.show(report.target(pos)));
					}
				});
			} else {
				ReportHud.show(RedstoneInspector.inspect(level, pos, false).target(pos));
			}
			return;
		}
		BlockPos spawn = bhr.getDirection() == net.minecraft.core.Direction.UP ? pos.above() : pos.relative(bhr.getDirection());
		for (int i = 0; i < 4 && level.getBlockState(spawn.below()).getCollisionShape(level, spawn.below()).isEmpty(); i++) {
			spawn = spawn.below(); // stand on the floor below a wall/ceiling spot
		}
		final BlockPos spawnPos = spawn;
		var playerPos = mc.player.position();
		if (server != null) {
			var dim = level.dimension();
			server.submit(() -> {
				var sl = server.getLevel(dim);
				return sl == null ? null : SpawnInspector.inspectServer(sl, spawnPos, playerPos);
			}).thenAccept(report -> {
				if (report != null) {
					mc.execute(() -> ReportHud.show(report.target(pos)));
				}
			});
		} else {
			ReportHud.show(SpawnInspector.inspectClient(level, spawnPos, playerPos).target(pos));
		}
	}
}
