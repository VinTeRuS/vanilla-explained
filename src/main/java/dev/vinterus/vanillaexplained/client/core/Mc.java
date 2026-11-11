package dev.vinterus.vanillaexplained.client.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;

/** Small wrappers around Minecraft APIs that differ between versions. */
public final class Mc {
	public static final String MOD_ID = "vanillaexplained";

	private Mc() {
	}

	/** Reach of the Why? key: far enough to inspect spawn spots 24+ blocks away. */
	public static final double WHY_REACH = 48.0;

	public static net.minecraft.world.phys.HitResult lookTarget(Minecraft mc) {
		return mc.player == null ? null : mc.player.pick(WHY_REACH, 1.0F, false);
	}

	public static Screen screen() {
		//? if >=26.2 {
		return Minecraft.getInstance().gui.screen();
		//?} else {
		/*return Minecraft.getInstance().screen;
		*///?}
	}

	public static void setScreen(Screen s) {
		//? if >=26.2 {
		Minecraft.getInstance().gui.setScreen(s);
		//?} else {
		/*Minecraft.getInstance().setScreen(s);
		*///?}
	}

	public static boolean shiftDown() {
		//? if >=1.21.9 {
		return Minecraft.getInstance().hasShiftDown();
		//?} else {
		/*return Screen.hasShiftDown();
		*///?}
	}

	public static String dimId(ResourceKey<Level> key) {
		//? if >=1.21.11 {
		return key.identifier().toString();
		//?} else {
		/*return key.location().toString();
		*///?}
	}

	public static String dimId(Level level) {
		return dimId(level.dimension());
	}

	//? if >=1.21.9 {
	private static KeyMapping.Category category;
	//?}

	public static KeyMapping keyMapping(String name, int key) {
		//? if >=1.21.9 {
		if (category == null) {
			category = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		}
		return new KeyMapping(name, key, category);
		//?} else {
		/*return new KeyMapping(name, key, "key.category.vanillaexplained.main");
		*///?}
	}

	public static int randomTickSpeed(ServerLevel level) {
		//? if >=1.21.11 {
		return level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED);
		//?} else {
		/*return level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING);
		*///?}
	}

	public static boolean monsterSpawningOn(ServerLevel level) {
		//? if >=1.21.11 {
		var rules = level.getGameRules();
		return rules.get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)
				&& rules.get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MONSTERS);
		//?} else {
		/*return level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
		*///?}
	}

	public static ItemStack spawnEgg(EntityType<?> type) {
		//? if >=26.1 {
		return SpawnEggItem.byId(type).map(h -> new ItemStack(h)).orElse(new ItemStack(Items.BARRIER));
		//?} else {
		/*SpawnEggItem egg = SpawnEggItem.byId(type);
		return egg == null ? new ItemStack(Items.BARRIER) : new ItemStack(egg);
		*///?}
	}

	public static boolean spawnPositionOk(EntityType<?> type, Level level, BlockPos pos) {
		//? if >=1.20.5 {
		return SpawnPlacements.isSpawnPositionOk(type, level, pos);
		//?} else {
		/*return NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type), level, pos, type);
		*///?}
	}

	public static boolean checkSpawnRules(EntityType<?> type, ServerLevel level, BlockPos pos, RandomSource random) {
		//? if >=1.21.2 {
		return SpawnPlacements.checkSpawnRules(type, level, net.minecraft.world.entity.EntitySpawnReason.NATURAL, pos, random);
		//?} else {
		/*return SpawnPlacements.checkSpawnRules(type, level, net.minecraft.world.entity.MobSpawnType.NATURAL, pos, random);
		*///?}
	}

	/** Monster types the game may pick at this position (biome list or structure override). */
	public static List<EntityType<?>> monstersAt(ServerLevel level, BlockPos pos) {
		var structures = level.structureManager();
		var generator = level.getChunkSource().getGenerator();
		List<EntityType<?>> out = new ArrayList<>();
		boolean fortress = NaturalSpawner.isInNetherFortressBounds(pos, level, MobCategory.MONSTER, structures);
		//? if >=26.3 {
		var list = fortress ? net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure.FORTRESS_ENEMIES
				: generator.getMobsAt(level, structures, MobCategory.MONSTER, pos);
		//?} else {
		/*var list = fortress ? net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure.FORTRESS_ENEMIES
				: generator.getMobsAt(level.getBiome(pos), structures, MobCategory.MONSTER, pos);
		*///?}
		//? if >=1.21.5 {
		for (var w : list.unwrap()) {
			out.add(w.value().type());
		}
		//?} else {
		/*for (var w : list.unwrap()) {
			out.add(w.type);
		}
		*///?}
		return out;
	}

	public static int farmlandMoisture(BlockState s) {
		var moisture = net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE;
		return s.hasProperty(moisture) ? s.getValue(moisture) : 0;
	}

	public static boolean isFarmland(BlockState s) {
		return s.is(net.minecraft.world.level.block.Blocks.FARMLAND);
	}

	public static int intMin(net.minecraft.util.valueproviders.IntProvider p) {
		//? if >=26.1 {
		return p.minInclusive();
		//?} else {
		/*return p.getMinValue();
		*///?}
	}

	public static int intMax(net.minecraft.util.valueproviders.IntProvider p) {
		//? if >=26.1 {
		return p.maxInclusive();
		//?} else {
		/*return p.getMaxValue();
		*///?}
	}

	public static int analogOutput(BlockState s, Level level, BlockPos pos) {
		//? if >=1.21.9 {
		return s.getAnalogOutputSignal(level, pos, net.minecraft.core.Direction.NORTH);
		//?} else {
		/*return s.getAnalogOutputSignal(level, pos);
		*///?}
	}

	public static int maxY(Level level) {
		//? if >=1.21.2 {
		return level.getMaxY();
		//?} else {
		/*return level.getMaxBuildHeight() - 1;
		*///?}
	}

	public static int lightDampening(BlockState s, Level level, BlockPos pos) {
		//? if >=26.1 {
		return s.getLightDampening();
		//?} else if >=1.21.2 {
		/*return s.getLightBlock();
		*///?} else {
		/*return s.getLightBlock(level, pos);
		*///?}
	}

	public static long chunkKey(ChunkPos pos) {
		//? if >=26.1 {
		return pos.pack();
		//?} else {
		/*return pos.toLong();
		*///?}
	}

	public static int fireworkDuration(ItemStack s) {
		//? if >=1.20.5 {
		var fw = s.get(net.minecraft.core.component.DataComponents.FIREWORKS);
		return fw != null ? fw.flightDuration() : 1;
		//?} else {
		/*var tag = s.getTagElement("Fireworks");
		return tag != null ? tag.getByte("Flight") : 1;
		*///?}
	}

	public static int unbreaking(Player p, ItemStack stack) {
		//? if >=1.21 {
		var holder = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.UNBREAKING);
		return holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
		//?} else {
		/*return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
		*///?}
	}

	public static boolean creative(Player p) {
		//? if >=1.20.5 {
		return p.hasInfiniteMaterials();
		//?} else {
		/*return p.getAbilities().instabuild;
		*///?}
	}
}
