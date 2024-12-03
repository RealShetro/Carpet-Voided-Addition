package carpet.commands;

import carpet.CarpetServer;
import carpet.helpers.ClusterHelper;
import carpet.utils.Messenger;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class CommandCluster extends CommandCarpetBase {
    private ClusterHelper clusterHelper = new ClusterHelper();

    @Override
    public String getName() {
        return "cluster";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "cluster peek/read <NBT as parameters>/compute/loadCluster/loadGrid/constructLoader";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 0) {
            return getListOfStringsMatchingLastWord(args, "peek", "read", "compute", "loadCluster", "loadGrid", "constructLoader");
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandCluster", sender)) return;
        if (args.length < 1) return;
        switch (args[0]) {
            case "peek":
                Messenger.s(sender, clusterHelper.writeToNBT(new NBTTagCompound()).toString());
                break;
            case "read":
                if (args.length < 2) return;
                try {
                    NBTTagCompound compound = JsonToNBT.getTagFromJson(args[1]);
                    clusterHelper.readFromNBT(compound);
                    Messenger.s(sender, "Data successfully read! ");
                } catch (NBTException e) {
                    throw new CommandException("Malformed NBT");
                }
                break;
            case "compute":
                HttpUtil.DOWNLOADER_EXECUTOR.submit(() -> {
                    long startMs = System.currentTimeMillis();
                    int value = clusterHelper.compute();
                    CarpetServer.minecraft_server.callFromMainThread(Executors.callable(() -> {
                        Messenger.s(sender, "Computation of optimal cluster chunks completed in " +
                                (System.currentTimeMillis() - startMs) + " milliseconds, and the grid has " + value + " chunks");
                        Messenger.s(sender, "The optimal target chunk is " + clusterHelper.optimalTargetChunk);
                        Messenger.s(sender, "The optimal 4 chunks to exclude is " + clusterHelper.optimalExcludedRelative);
                    }));
                });
                Messenger.s(sender, "Cluster computation started on an async thread. Please wait patiently. ");
                break;
            case "loadCluster":
                clusterHelper.loadCluster(sender.getEntityWorld());
                Messenger.s(sender, "Cluster chunks loaded! ");
                break;
            case "loadGrid":
                clusterHelper.loadGrid(sender.getEntityWorld());
                Messenger.s(sender, "Loading grid loaded! ");
                break;
            case "constructLoader":
                int y = 64;
                if (args.length < 3) {
                    Messenger.s(sender, "Simulating the construction of the loader");
                    int disconnections = clusterHelper.checkClusterLoader();
                    Messenger.s(sender, "A total of " + disconnections + " chunks are disconnected! ");
                    return;
                }
                Block block = CommandBase.getBlockByText(sender, args[1]);
                int metadata = Integer.parseInt(args[2]);
                IBlockState buildingBlock = block.getStateFromMeta(metadata);
                if (args.length >= 4) y = Integer.parseInt(args[3]);
                clusterHelper.buildClusterLoader(sender.getEntityWorld(), y, buildingBlock);
                Messenger.s(sender, "Loader for the cluster grid has been constructed!!! ");
                break;
        }
    }
}
