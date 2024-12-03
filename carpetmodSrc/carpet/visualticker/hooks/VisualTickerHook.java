package carpet.visualticker.hooks;


import com.sk89q.worldedit.util.Direction;
import net.minecraft.block.BlockEventData;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public interface VisualTickerHook {
    void initialize(World world, StructureBoundingBox boundingBox);

    void setGameTime(long gameTime);

    void setTileTickPhase(NextTickListEntry tileTick);
    void setChunkTickPhase(ChunkPos chunkPos);
    void setBlockEventPhase(BlockEventData blockEvent, int depth, int index);
    void setEntityPhase();
    void setTileEntityPhase(BlockPos pos);
    void setPlayerActionPhase();

    void onSetBlockState(BlockPos pos, IBlockState newState, int flags);
    void onNCUpdateSent(BlockPos pos, Direction excluding);
    void onPPUpdateSent(BlockPos pos);
    void onComparatorUpdateSent(BlockPos pos);

    void scheduleTileTick(NextTickListEntry tileTick);
    void scheduleBlockEvent(BlockEventData blockEvent, int depth, int index);

    void setTileEntity(BlockPos pos, TileEntity tileEntity);
    void removeTileEntity(BlockPos pos, TileEntity tileEntity);
    void changeTileEntity(BlockPos pos, TileEntity tileEntity);

    void conclude();

    byte SET_GAME_TIME = 0x00;
    byte SET_TT_PHASE = 0x01;
    byte SET_CT_PHASE = 0x08;
    byte SET_BE_PHASE = 0x02;
    byte SET_EU_PHASE = 0x03;
    byte SET_TE_PHASE = 0x04;
    byte SET_NU_PHASE = 0x05;

    byte SET_BLOCK_STATE = 0x10;
    byte NC_UPDATE_SENT = 0x11;
    byte PP_UPDATE_SENT = 0x12;
    byte COMPARATOR_UPDATE_SENT = 0x13;

    byte TT_SCHEDULING = 0x20;
    byte BE_SCHEDULING = 0x21;

    byte TE_SET = 0x30;
    byte TE_REMOVED = 0x31;
    byte TE_CHANGED = 0x32;
}
