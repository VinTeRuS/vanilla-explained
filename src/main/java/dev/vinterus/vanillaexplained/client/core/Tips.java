package dev.vinterus.vanillaexplained.client.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.Component;

/**
 * Little-known mechanics, each checked against the 26.3 code. Inspectors attach one tip per
 * report and rotate through them, so pressing G again shows the next one.
 */
public final class Tips {
	public static final Map<String, List<String>> BY_TOPIC = new java.util.LinkedHashMap<>();
	static {
		BY_TOPIC.put("spawn", List.of("worldspawn", "sky", "thunder", "cap"));
		BY_TOPIC.put("crop", List.of("night", "neighbours", "trample", "maintain", "beetroot"));
		BY_TOPIC.put("rs", List.of("torch", "qc", "slimehoney", "repeaterlock", "hopper"));
		BY_TOPIC.put("ench", List.of("seed", "blockers", "levels"));
		BY_TOPIC.put("anvil", List.of("rename", "books"));
		BY_TOPIC.put("portal", List.of("square", "position", "lower"));
		BY_TOPIC.put("beacon", List.of("bedrock", "range"));
		BY_TOPIC.put("elytra", List.of("unbreaking", "time", "duration"));
	}

	private static final Map<String, Integer> NEXT = new HashMap<>();

	private Tips() {
	}

	public static Component key(String topic, String id) {
		return Report.t("tip." + topic + "." + id);
	}

	/** The next tip of a topic (rotates). */
	public static Component next(String topic) {
		List<String> ids = BY_TOPIC.get(topic);
		int i = NEXT.merge(topic, 1, Integer::sum) - 1;
		return key(topic, ids.get(Math.floorMod(i, ids.size())));
	}

	public static int count() {
		return BY_TOPIC.values().stream().mapToInt(List::size).sum();
	}
}
