package dev.vinterus.vanillaexplained.client.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vinterus.vanillaexplained.client.core.Store;
import dev.vinterus.vanillaexplained.rules.PortalLinker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Remembers Nether portal blocks the client has seen, per world and dimension, so portal links
 * can be predicted even when the other side isn't loaded.
 */
public final class PortalMemory {
	/** dimension id -> chunk key -> packed block positions */
	public static final class Data {
		public Map<String, Map<Long, long[]>> dims = new HashMap<>();
	}

	private static String loadedKey;
	private static Data data = new Data();
	private static boolean dirty;

	private PortalMemory() {
	}

	private static String file() {
		return "portals_" + Store.worldKey() + ".json";
	}

	public static void ensureLoaded() {
		String key = file();
		if (!key.equals(loadedKey)) {
			flush();
			loadedKey = key;
			data = Store.load(key, Data.class, new Data());
		}
	}

	public static void flush() {
		if (dirty && loadedKey != null) {
			Store.save(loadedKey, data);
			dirty = false;
		}
	}

	public static void onChunkLoad(String dim, LevelChunk chunk) {
		ensureLoaded();
		List<Long> found = new ArrayList<>();
		LevelChunkSection[] sections = chunk.getSections();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection s = sections[i];
			if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> st.is(Blocks.NETHER_PORTAL))) {
				continue;
			}
			int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (s.getBlockState(x, y, z).is(Blocks.NETHER_PORTAL)) {
							found.add(BlockPos.asLong(baseX + x, baseY + y, baseZ + z));
						}
					}
				}
			}
		}
		Map<Long, long[]> chunks = data.dims.computeIfAbsent(dim, d -> new HashMap<>());
		long ck = dev.vinterus.vanillaexplained.client.core.Mc.chunkKey(chunk.getPos());
		if (found.isEmpty()) {
			if (chunks.remove(ck) != null) {
				dirty = true;
			}
		} else {
			chunks.put(ck, found.stream().mapToLong(Long::longValue).toArray());
			dirty = true;
		}
	}

	public static List<PortalLinker.Pos> portals(String dim) {
		ensureLoaded();
		List<PortalLinker.Pos> out = new ArrayList<>();
		Map<Long, long[]> chunks = data.dims.get(dim);
		if (chunks == null) {
			return out;
		}
		for (long[] arr : chunks.values()) {
			for (long p : arr) {
				out.add(new PortalLinker.Pos(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p)));
			}
		}
		return out;
	}
}
