package carpet.commands;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class CommandWriteBlock extends CommandExtendedSetBlockBase {

    @Override
    public String getName() {
        return "writeblock";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.setblock.usage";
    }

    @Override
    public boolean setBlockState(World world, BlockPos pos, IBlockState state, int flags) {
        Chunk chunk = world.getChunk(pos);
        IBlockState state1 = chunk.getBlockState(pos);
        if (state == state1) return false;
        ExtendedBlockStorage storage = chunk.getBlockStorageArray()[pos.getY() >> 4];
        storage.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
        IBlockState state2 = chunk.getBlockState(pos);
        world.notifyBlockUpdate(pos, state1, state2, 2);
        return state1 != state2;
    }
}
