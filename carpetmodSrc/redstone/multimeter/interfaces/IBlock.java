package redstone.multimeter.interfaces;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IBlock {
	
	default boolean rsmm$isMeterable() {
		return false;
	}
	
	default boolean rsmm$isPowerSource() {
		return false;
	}
	
	default boolean rsmm$logPoweredOnBlockUpdate() {
		return true;
	}
	
	default boolean rsmm$isPowered(World world, BlockPos pos, IBlockState state) {
		return world.isBlockPowered(pos);
	}
}
