package carpet.helpers;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ITTCrasher {
    boolean wouldCrash(World world, BlockPos pos, IBlockState state);
}
