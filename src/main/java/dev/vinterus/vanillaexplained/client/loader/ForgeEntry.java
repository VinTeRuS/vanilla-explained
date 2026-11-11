//? if forge {
/*package dev.vinterus.vanillaexplained.client.loader;

import dev.vinterus.vanillaexplained.client.VanillaExplainedClient;
import dev.vinterus.vanillaexplained.client.core.Gfx;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("vanillaexplained")
public final class ForgeEntry {
	public ForgeEntry() {
		if (FMLEnvironment.dist != Dist.CLIENT) {
			return; // client-only mod: nothing to do on a dedicated server
		}
		var modBus = FMLJavaModLoadingContext.get().getModEventBus();
		KeyMapping[] keys = VanillaExplainedClient.createKeys();
		modBus.addListener((RegisterKeyMappingsEvent e) -> {
			for (KeyMapping key : keys) {
				e.register(key);
			}
		});
		modBus.addListener((RegisterGuiOverlaysEvent e) -> e.registerAboveAll("hud",
				(gui, g, partialTick, w, h) -> VanillaExplainedClient.renderHud(new Gfx(g), partialTick)));

		var bus = MinecraftForge.EVENT_BUS;
		bus.addListener((TickEvent.ClientTickEvent e) -> {
			if (e.phase == TickEvent.Phase.END) {
				VanillaExplainedClient.tick(Minecraft.getInstance());
			}
		});
		bus.addListener((ChunkEvent.Load e) -> {
			if (e.getLevel().isClientSide() && e.getChunk() instanceof LevelChunk chunk) {
				VanillaExplainedClient.onChunkLoad(chunk.getLevel(), chunk);
			}
		});
		bus.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> VanillaExplainedClient.onDisconnect());
		bus.addListener((ScreenEvent.Init.Post e) -> VanillaExplainedClient.onScreenInit(e.getScreen(), e.getScreen().width,
				e.getScreen().height, e::addListener));
		bus.addListener((ScreenEvent.Render.Post e) -> VanillaExplainedClient.onScreenRender(e.getScreen(), new Gfx(e.getGuiGraphics())));
	}
}
*///?}
