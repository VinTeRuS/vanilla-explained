package dev.vinterus.vanillaexplained.client.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/** Tiny JSON persistence under config/vanillaexplained/. */
public final class Store {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Store() {
	}

	public static Path dir() {
		return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("vanillaexplained");
	}

	public static <T> T load(String name, Class<T> type, T fallback) {
		Path p = dir().resolve(name);
		if (!Files.exists(p)) {
			return fallback;
		}
		try (Reader r = Files.newBufferedReader(p)) {
			T v = GSON.fromJson(r, type);
			return v != null ? v : fallback;
		} catch (IOException | RuntimeException e) {
			return fallback;
		}
	}

	public static void save(String name, Object value) {
		try {
			Files.createDirectories(dir());
			Path tmp = dir().resolve(name + ".tmp");
			try (Writer w = Files.newBufferedWriter(tmp)) {
				GSON.toJson(value, w);
			}
			Files.move(tmp, dir().resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
					java.nio.file.StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException ignored) {
			// best effort
		}
	}

	/** A file-name-safe key for the current world or server. */
	public static String worldKey() {
		var mc = net.minecraft.client.Minecraft.getInstance();
		String raw;
		if (mc.getSingleplayerServer() != null) {
			raw = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
		} else if (mc.getCurrentServer() != null) {
			raw = "mp_" + mc.getCurrentServer().ip;
		} else {
			raw = "unknown";
		}
		return raw.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
