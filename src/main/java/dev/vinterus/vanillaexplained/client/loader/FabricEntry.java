//? if fabric {
package dev.vinterus.vanillaexplained.client.loader;

import dev.vinterus.vanillaexplained.client.VanillaExplainedClient;
import dev.vinterus.vanillaexplained.client.core.Gfx;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;

public final class FabricEntry implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (KeyMapping key : VanillaExplainedClient.createKeys()) {
			//? if >=26.1 {
			net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(key);
			//?} else {
			/*net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(key);
			*///?}
		}

		//? if >=26.1 {
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				net.minecraft.resources.Identifier.fromNamespaceAndPath("vanillaexplained", "hud"),
				(g, delta) -> VanillaExplainedClient.renderHud(new Gfx(g), delta.getGameTimeDeltaPartialTick(false)));
		//?} else if >=1.21.6 {
		/*net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
				net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vanillaexplained", "hud"),
				(g, delta) -> VanillaExplainedClient.renderHud(new Gfx(g), delta.getGameTimeDeltaPartialTick(false)));
		*///?} else if >=1.21 {
		/*net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(g, delta) -> VanillaExplainedClient.renderHud(new Gfx(g), delta.getGameTimeDeltaPartialTick(false)));
		*///?} else {
		/*net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				(g, delta) -> VanillaExplainedClient.renderHud(new Gfx(g), delta));
		*///?}

		ClientTickEvents.END_CLIENT_TICK.register(VanillaExplainedClient::tick);
		ClientChunkEvents.CHUNK_LOAD.register(VanillaExplainedClient::onChunkLoad);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> VanillaExplainedClient.onDisconnect());
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			//? if >=26.1 {
			VanillaExplainedClient.onScreenInit(screen, w, h, Screens.getWidgets(screen)::add);
			//?} else {
			/*VanillaExplainedClient.onScreenInit(screen, w, h, Screens.getButtons(screen)::add);
			*///?}
			//? if >=26.1 {
			ScreenEvents.afterExtract(screen).register((s, g, mx, my, a) -> VanillaExplainedClient.onScreenRender(s, new Gfx(g)));
			//?} else {
			/*ScreenEvents.afterRender(screen).register((s, g, mx, my, a) -> VanillaExplainedClient.onScreenRender(s, new Gfx(g)));
			*///?}
		});
	}
}
//?}
