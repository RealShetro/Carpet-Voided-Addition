package carpet.visualticker.display;

import carpet.visualticker.hooks.VisualTickerHook;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.*;

import static net.minecraft.util.text.TextFormatting.*;

/**
 * Implementation of visualticker displaying recorded data
 * The display region is a region that is as large as the recorded region
 * Blocks inside are displayed with actual blocks, but b36 are displayed by a falling glass block with a smaller falling
 *  block that is the moved block inside. Progress of b36 are displayed as the positioning of those falling blocks.
 * To highlight a block, a falling block is summoned at that position, that is set invisible and glowing.
 * All tile ticks, block events and tile entities are not processed in the display region.
 *
 * Highlights of different colors may be made by adding the falling blocks to different teams maybe...
 *
 */
public class VisualTickerDisplayRegion {
    private StructureBoundingBox boundingBox;

    private World world;

    private Map<BlockPos, EntityFallingBlock> b36GlassRenderers = new LinkedHashMap<>();
    private Map<BlockPos, EntityFallingBlock> b36MiniatureRenderers = new LinkedHashMap<>();
    private Map<BlockPos, EntityFallingBlock> highlightRenderers = new LinkedHashMap<>();

    public VisualTickerDisplayRegion() {
        for (TextFormatting color: TextFormatting.values()) {
            if (!color.isColor()) continue;
            String teamName = getTeamName(color);
            if (world.getScoreboard().getTeam(teamName) == null) {
                ScorePlayerTeam team = world.getScoreboard().createTeam(teamName);
                team.setColor(color);
            }
        }
    }

    public static String getTeamName(TextFormatting color) {
        return "VisualTickerDisplayTeam-" + color.getColorIndex();
    }

    public static BlockPos getAbsolutePos(StructureBoundingBox box, BlockPos relativePos) {
        return new BlockPos(relativePos.getX() + box.minX, relativePos.getY() + box.minY, relativePos.getZ() + box.minZ);
    }

    private BlockPos getAbsolutePos(BlockPos relativePos) {
        return getAbsolutePos(this.boundingBox, relativePos);
    }

    private static EntityFallingBlock createDummyFB(World world, BlockPos pos) {
        EntityFallingBlock fallingBlock = new EntityFallingBlock(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Blocks.SAND.getDefaultState());
        fallingBlock.setNoGravity(true);
        world.spawnEntity(fallingBlock);
        return fallingBlock;
    }

    private void highlightBlockPos(BlockPos pos, TextFormatting color) {
        if (color == null) {
            EntityFallingBlock renderer = highlightRenderers.remove(pos);
            if (renderer != null) renderer.setDead();
        } else {
            EntityFallingBlock renderer = highlightRenderers.computeIfAbsent(pos, __ -> createDummyFB(world, pos));
            world.getScoreboard().addPlayerToTeam(renderer.getCachedUniqueIdString(), getTeamName(color));
            renderer.setGlowing(true);
            renderer.setInvisible(true);
        }
    }

    public void setBlockState(BlockPos relativePos, IBlockState blockState, int flags, IBlockState fallback) {
        IBlockState actualBlockState = fallback == null ? blockState : fallback;
        BlockPos actualBlockPos = getAbsolutePos(relativePos);
        world.setBlockState(actualBlockPos, actualBlockState, 128 | 16 | 2);
        this.handleB36RenderingAt(actualBlockPos);
    }

    private void handleB36RenderingAt(BlockPos actualPos) {
        IBlockState blockState = world.getBlockState(actualPos);
        Block block = blockState.getBlock();
        if (block instanceof BlockPistonMoving) {
            IBlockState wrappedState = null;
            float b36Progress = 0.0f;
            TileEntity b36TE = world.getChunk(actualPos).getTileEntity(actualPos, Chunk.EnumCreateEntityType.CHECK);
            EnumFacing facing = null;
            if (b36TE instanceof TileEntityPiston) {
                wrappedState = ((TileEntityPiston) b36TE).getPistonState();
                b36Progress = ((TileEntityPiston) b36TE).progress;
                facing = ((TileEntityPiston) b36TE).getFacing();
            }
            IBlockState glassState = Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR,
                    wrappedState.getBlock() == Blocks.SLIME_BLOCK ? EnumDyeColor.LIME : EnumDyeColor.GRAY);

        } else {
            b36GlassRenderers.remove(actualPos);
            b36MiniatureRenderers.remove(actualPos);
        }
    }
}
