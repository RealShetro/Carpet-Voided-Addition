package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.CapturedDrops;
import carpet.worldedit.WorldEditBridge;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.*;
import net.minecraft.command.server.CommandSetBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class CommandExtendedSetBlockBase extends CommandCarpetBase {
    @Override
    public abstract String getName();

    @Override
    public abstract String getUsage(ICommandSender sender);

    public abstract boolean setBlockState(World world, BlockPos pos, IBlockState state, int flags);

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4)
        {
            throw new WrongUsageException("commands.setblock.usage", new Object[0]);
        }
        else
        {
            sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, 0);
            BlockPos blockpos = parseBlockPos(sender, args, 0, false);
            Block block = CommandBase.getBlockByText(sender, args[3]);
            IBlockState iblockstate;

            if (args.length >= 5)
            {
                iblockstate = convertArgToBlockState(block, args[4]);
            }
            else
            {
                iblockstate = block.getDefaultState();
            }

            World world = sender.getEntityWorld();

            if (!world.isBlockLoaded(blockpos))
            {
                throw new CommandException("commands.setblock.outOfWorld", new Object[0]);
            }
            else
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                boolean flag = false;

                if (args.length >= 7 && block.hasTileEntity())
                {
                    String s = buildString(args, 6);

                    try
                    {
                        nbttagcompound = JsonToNBT.getTagFromJson(s);
                        flag = true;
                    }
                    catch (NBTException nbtexception)
                    {
                        throw new CommandException("commands.setblock.tagError", new Object[] {nbtexception.getMessage()});
                    }
                }

                EntityPlayerMP worldEditPlayer = sender instanceof EntityPlayerMP ? (EntityPlayerMP) sender : null;
                NBTTagCompound worldEditTag = flag ? nbttagcompound : null;

                if (args.length >= 6)
                {
                    if ("destroy".equals(args[5]))
                    {
                        WorldEditBridge.recordBlockEdit(worldEditPlayer, world, blockpos, Blocks.AIR.getDefaultState(), worldEditTag);
                        CapturedDrops.setCapturingDrops(true);
                        world.destroyBlock(blockpos, true);
                        CapturedDrops.setCapturingDrops(false);
                        for (EntityItem drop : CapturedDrops.getCapturedDrops())
                            WorldEditBridge.recordEntityCreation(worldEditPlayer, world, drop);
                        CapturedDrops.clearCapturedDrops();

                        if (block == Blocks.AIR)
                        {
                            notifyCommandListener(sender, this, "commands.setblock.success", new Object[0]);
                            return;
                        }
                    }
                    else if ("keep".equals(args[5]) && !world.isAirBlock(blockpos))
                    {
                        throw new CommandException("commands.setblock.noChange", new Object[0]);
                    }
                }

                WorldEditBridge.recordBlockEdit(worldEditPlayer, world, blockpos, iblockstate, worldEditTag);

                TileEntity tileentity1 = world.getTileEntity(blockpos);

                if (tileentity1 != null && tileentity1 instanceof IInventory)
                {
                    ((IInventory)tileentity1).clear();
                }

                if (!setBlockState(world, blockpos, iblockstate, 2 | (CarpetSettings.fillUpdates?0:128) )) // CM
                {
                    throw new CommandException("commands.setblock.noChange", new Object[0]);
                }
                else
                {
                    if (flag)
                    {
                        TileEntity tileentity = world.getTileEntity(blockpos);

                        if (tileentity != null)
                        {
                            nbttagcompound.setInteger("x", blockpos.getX());
                            nbttagcompound.setInteger("y", blockpos.getY());
                            nbttagcompound.setInteger("z", blockpos.getZ());
                            tileentity.readFromNBT(nbttagcompound);
                        }
                    }

                    // CM
                    if (CarpetSettings.fillUpdates)
                    {
                        world.notifyNeighborsRespectDebug(blockpos, iblockstate.getBlock(), false);
                    }
                    // CM end
                    sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, 1);
                    notifyCommandListener(sender, this, "commands.setblock.success", new Object[0]);
                }
            }
        }
    }
}
