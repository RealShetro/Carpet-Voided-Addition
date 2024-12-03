package carpet.helpers;

import carpet.CarpetSettings;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BlockRotator
{
    public static boolean flipBlockWithCactus(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!playerIn.capabilities.allowEdit || !CarpetSettings.flippinCactus || !player_holds_cactus_mainhand(playerIn))
        {
            return false;
        }
        return flip_block(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }
    public static IBlockState placeBlockWithCactus(Block block, World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        if (block instanceof BlockObserver)
        {
            return block.getDefaultState()
                .withProperty(BlockDirectional.FACING, EnumFacing.byIndex((int)hitX - 2))
                .withProperty(BlockObserver.POWERED, CarpetSettings.observersDoNonUpdate);
        }
        return null;
    }

    public static IBlockState alternativeBlockPlacement(Block block, World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        //actual alternative block placement code
        //
        if (block instanceof BlockGlazedTerracotta)
        {
            facing = EnumFacing.byIndex((int)hitX - 2);
            if(facing == EnumFacing.UP || facing == EnumFacing.DOWN)
            {
                facing = placer.getHorizontalFacing().getOpposite();
            }
            return block.getDefaultState().withProperty(BlockHorizontal.FACING, facing);
        }
        else if (block instanceof BlockObserver)
        {
            return block.getDefaultState()
                    .withProperty(BlockDirectional.FACING, EnumFacing.byIndex((int)hitX - 2))
                    .withProperty(BlockObserver.POWERED, CarpetSettings.observersDoNonUpdate);
        }
        else if (block instanceof BlockRedstoneRepeater)
        {
            facing = EnumFacing.byIndex((((int)hitX) % 10) - 2);
            if(facing == EnumFacing.UP || facing == EnumFacing.DOWN)
            {
                facing = placer.getHorizontalFacing().getOpposite();
            }
            return block.getDefaultState()
                    .withProperty(BlockHorizontal.FACING, facing)
                    .withProperty(BlockRedstoneRepeater.DELAY, MathHelper.clamp((((int) hitX) / 10) + 1, 1, 4))
                    .withProperty(BlockRedstoneRepeater.LOCKED, Boolean.FALSE);
        }
        else if (block instanceof BlockTrapDoor)
        {
            return block.getDefaultState()
                    .withProperty(BlockTrapDoor.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2))
                    .withProperty(BlockTrapDoor.OPEN, Boolean.FALSE)
                    .withProperty(BlockTrapDoor.HALF, (hitX > 10) ? BlockTrapDoor.DoorHalf.TOP : BlockTrapDoor.DoorHalf.BOTTOM)
                    .withProperty(BlockTrapDoor.OPEN, worldIn.isBlockPowered(pos));
        }
        else if (block instanceof BlockRedstoneComparator)
        {
            facing = EnumFacing.byIndex((((int)hitX) % 10) - 2);
            if((facing == EnumFacing.UP) || (facing == EnumFacing.DOWN))
            {
                facing = placer.getHorizontalFacing().getOpposite();
            }
            BlockRedstoneComparator.Mode m = (hitX > 10)?BlockRedstoneComparator.Mode.SUBTRACT: BlockRedstoneComparator.Mode.COMPARE;
            return block.getDefaultState()
                    .withProperty(BlockHorizontal.FACING, facing)
                    .withProperty(BlockRedstoneComparator.POWERED, Boolean.FALSE)
                    .withProperty(BlockRedstoneComparator.MODE, m);
        }
        else if (block instanceof BlockDispenser)
        {
            return block.getDefaultState()
                    .withProperty(BlockDispenser.FACING, EnumFacing.byIndex((int)hitX - 2))
                    .withProperty(BlockDispenser.TRIGGERED, Boolean.FALSE);
        }
        else if (block instanceof BlockPistonBase)
        {
            return block.getDefaultState()
                    .withProperty(BlockDirectional.FACING,EnumFacing.byIndex((int)hitX - 2) )
                    .withProperty(BlockPistonBase.EXTENDED, Boolean.FALSE);
        }
        else if (block instanceof BlockStairs)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockStairs.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2))
                    .withProperty(BlockStairs.HALF, ( hitX > 10)?BlockStairs.EnumHalf.TOP : BlockStairs.EnumHalf.BOTTOM);
        }
        else if (block instanceof BlockStairs)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockStairs.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2))
                    .withProperty(BlockStairs.HALF, ( hitX > 10)?BlockStairs.EnumHalf.TOP : BlockStairs.EnumHalf.BOTTOM);
        }
        else if (block instanceof BlockFenceGate)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockFenceGate.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2))
                    .withProperty(BlockFenceGate.OPEN, Boolean.valueOf(hitX > 10));
        }
        else if (block instanceof BlockPumpkin)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockFenceGate.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2));
        }
        else if (block instanceof BlockChest)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockFenceGate.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2));
        }
        else if (block instanceof BlockEnderChest)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockFenceGate.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2));
        }
        else if (block instanceof BlockDoor)
        {
            return block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .withProperty(BlockDoor.FACING, EnumFacing.byIndex((((int)hitX) % 10) - 2))
                    .withProperty(BlockDoor.HINGE, hitX % 100 < 10 ? BlockDoor.EnumHingePosition.LEFT : BlockDoor.EnumHingePosition.RIGHT)
                    .withProperty(BlockDoor.OPEN, Boolean.valueOf(hitX > 100));
        }
        return null;
    }




    public static boolean flip_block(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        Block block = state.getBlock();
        IBlockState newState = null;
        // You are not supposed to flip rails using this in survival
        // If a rail configuration requires you to learn rail mechanics, go learn it
        // If a rail configuration requires an update suppressor, go build one
        //  - avoiding an entire contraption with a carpet rule is against the spirits of TMC
        if (block instanceof BlockRailBase && !playerIn.capabilities.isCreativeMode) {
            return false;
        }
        if ( (block instanceof BlockGlazedTerracotta) || (block instanceof BlockRedstoneDiode) || (block instanceof BlockRailBase) ||
             (block instanceof BlockTrapDoor)         || (block instanceof BlockLever)         || (block instanceof BlockFenceGate))
        {
            newState = block.withRotation(state, Rotation.CLOCKWISE_90);
        }
        else if ((block instanceof BlockObserver) || (block instanceof BlockEndRod))
        {
            newState = state.withProperty(BlockDirectional.FACING, (EnumFacing)state.getValue(BlockDirectional.FACING).getOpposite());
        }
        else if (block instanceof BlockDispenser)
        {
            newState = state.withProperty(BlockDispenser.FACING, (EnumFacing)state.getValue(BlockDispenser.FACING).getOpposite());
        }
        else if (block instanceof BlockPistonBase)
        {
            if (!(((Boolean)state.getValue(BlockPistonBase.EXTENDED)).booleanValue()))
                newState = state.withProperty(BlockDirectional.FACING, (EnumFacing)state.getValue(BlockDirectional.FACING).getOpposite());
        }
        else if (block instanceof BlockSlab)
        {
            if (!((BlockSlab) block).isDouble())
            {
                if (state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.TOP)
                {
                    newState = state.withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM);
                }
                else
                {
                    newState = state.withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP);
                }
            }
        }
        else if (block instanceof BlockHopper)
        {
            if ((EnumFacing)state.getValue(BlockHopper.FACING) != EnumFacing.DOWN)
            {
                newState = state.withProperty(BlockHopper.FACING, (EnumFacing) state.getValue(BlockHopper.FACING).rotateY());
            }
        }
        else if (block instanceof BlockStairs)
        {
            //LOG.error(String.format("hit with facing: %s, at side %.1fX, X %.1fY, Y %.1fZ",facing, hitX, hitY, hitZ));
            if ((facing == EnumFacing.UP && hitY == 1.0f) || (facing == EnumFacing.DOWN && hitY == 0.0f))
            {
                if (state.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP)
                {
                    newState = state.withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.BOTTOM);
                }
                else
                {
                    newState = state.withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP);
                }
            }
            else
            {
                boolean turn_right = true;
                if (facing == EnumFacing.NORTH)
                {
                    turn_right = (hitX <= 0.5);
                }
                else if (facing == EnumFacing.SOUTH)
                {
                    turn_right = !(hitX <= 0.5);
                }
                else if (facing == EnumFacing.EAST)
                {
                    turn_right = (hitZ <= 0.5);
                }
                else if (facing == EnumFacing.WEST)
                {
                    turn_right = !(hitZ <= 0.5);
                }
                else
                {
                    return false;
                }
                if (turn_right)
                {
                    newState = block.withRotation(state, Rotation.COUNTERCLOCKWISE_90);
                }
                else
                {
                    newState = block.withRotation(state, Rotation.CLOCKWISE_90);
                }
            }
        }
        else
        {
            return false;
        }
        if (newState == null) return false;
        // Allow creative players to use the old inconsistent behavior, skipping all the updates for consistency
        if (CarpetSettings.creativeInconsistentCactus && playerIn.capabilities.isCreativeMode) {
            worldIn.setBlockState(pos, newState, 130); // Rotate the block
            worldIn.markBlockRangeForRenderUpdate(pos, pos);
            return true;
        }
        Throwable catchedThrowable = null;
        try {
            // This step is to invoke onBlockAdded() and breakBlock() for more consistency
            // You can't just rotate a stupid repeater and bud redstone wire for free
            worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        } catch (Throwable throwable) { catchedThrowable = throwable; } // Disrespect update suppression
        worldIn.setBlockState(pos, newState, 130); // Rotate the block
        worldIn.markBlockRangeForRenderUpdate(pos, pos);
        // Update the newly placed block
        // This is to prevent stuff like floating levers
        // Stop using floating levers from flipping cactus as temporary bud wires for update suppression
        // The carpet implementation is not something that you should deconstruct
        try {
            worldIn.neighborChanged(pos, block, pos);
        } catch (Throwable throwable) { catchedThrowable = throwable; }
        // This step prevents levers attached to the incomplete side of a rotated stair for instance
        try {
            worldIn.notifyNeighborsOfStateChange(pos, block, true);
        } catch (Throwable throwable) { catchedThrowable = throwable; }
        if (catchedThrowable == null) return true;
        if (catchedThrowable instanceof RuntimeException) throw (RuntimeException) catchedThrowable;
        if (catchedThrowable instanceof Error) throw (Error) catchedThrowable;
        throw new RuntimeException(catchedThrowable);
    }
    private static boolean player_holds_cactus_mainhand(EntityPlayer playerIn)
    {
        return (!playerIn.getHeldItemMainhand().isEmpty()
                && playerIn.getHeldItemMainhand().getItem() instanceof ItemBlock &&
                ((ItemBlock) (playerIn.getHeldItemMainhand().getItem())).getBlock() == Blocks.CACTUS);
    }
    public static boolean flippinEligibility(Entity entity)
    {
        if (CarpetSettings.flippinCactus
                && (entity instanceof EntityPlayer))
        {
            EntityPlayer player = (EntityPlayer)entity;
            return (!player.getHeldItemOffhand().isEmpty()
                    && player.getHeldItemOffhand().getItem() instanceof ItemBlock &&
                    ((ItemBlock) (player.getHeldItemOffhand().getItem())).getBlock() == Blocks.CACTUS);
        }
        return false;
    }
}
