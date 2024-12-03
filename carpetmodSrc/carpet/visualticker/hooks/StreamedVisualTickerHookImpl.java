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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StreamedVisualTickerHookImpl implements VisualTickerHook {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
    private final BufferedOutputStream outputStream;

    public StreamedVisualTickerHookImpl(OutputStream outputStream) {
        this.outputStream = new BufferedOutputStream(outputStream);
    }


    @Override
    public void initialize(World world, StructureBoundingBox boundingBox) {

    }

    @Override
    public void setGameTime(long gameTime) {
        try {
            outputStream.write(SET_GAME_TIME);
        } catch (IOException e) {
            LOGGER.catching(e);
        }
    }

    @Override
    public void setTileTickPhase(NextTickListEntry tileTick) {

    }

    @Override
    public void setChunkTickPhase(ChunkPos chunkPos) {

    }

    @Override
    public void setBlockEventPhase(BlockEventData blockEvent, int depth, int index) {

    }

    @Override
    public void setEntityPhase() {

    }

    @Override
    public void setTileEntityPhase(BlockPos pos) {

    }

    @Override
    public void setPlayerActionPhase() {

    }

    @Override
    public void onSetBlockState(BlockPos pos, IBlockState newState, int flags) {

    }

    @Override
    public void onNCUpdateSent(BlockPos pos, Direction excluding) {

    }

    @Override
    public void onPPUpdateSent(BlockPos pos) {

    }

    @Override
    public void onComparatorUpdateSent(BlockPos pos) {

    }

    @Override
    public void scheduleTileTick(NextTickListEntry tileTick) {

    }

    @Override
    public void scheduleBlockEvent(BlockEventData blockEvent, int depth, int index) {

    }

    @Override
    public void setTileEntity(BlockPos pos, TileEntity tileEntity) {

    }

    @Override
    public void removeTileEntity(BlockPos pos, TileEntity tileEntity) {

    }

    @Override
    public void changeTileEntity(BlockPos pos, TileEntity tileEntity) {

    }

    @Override
    public void conclude() {
        try {
            this.outputStream.close();
        } catch (IOException e) {
            LOGGER.catching(e);
        }
    }
}
