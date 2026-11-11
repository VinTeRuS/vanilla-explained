package dev.vinterus.vanillaexplained.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only access to the vanilla repeater/comparator input logic. */
@Mixin(DiodeBlock.class)
public interface DiodeBlockInvoker {
	@Invoker("getInputSignal")
	int vanillaexplained$getInputSignal(Level level, BlockPos pos, BlockState state);

	@Invoker("getAlternateSignal")
	int vanillaexplained$getAlternateSignal(SignalGetter level, BlockPos pos, BlockState state);
}
