package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.BlockStateFlatteningMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommandFill13 extends CommandCarpetBase {
    @Override
    public String getName() {
        return "fill13";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/fill13 <x1> <y1> <z1> <x2> <y2> <z2> block[states]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandFill13", sender)) return;
        if (!sender.canUseCommand(2, "fill")) {
            notifyCommandListener(sender, this,
                    "You don't have permission to use /fill! ");
            return;
        }
        if (args.length < 4) throw new CommandException(getUsage(sender));
        BlockPos pos1 = parseBlockPos(sender, args, 0, false);
        BlockPos pos2 = parseBlockPos(sender, args, 3, false);
        IBlockState state = BlockStateFlatteningMap.deflattenBrigadierBlockStateToState(args[6]);
        if (state == null) throw new CommandException("Unable to resolve blockstate " + args[6]);
        int flags = CarpetSettings.fillUpdates ? 2 : 130;
        int x1, x2, y1, y2, z1, z2;
        World world = sender.getEntityWorld();
        x1 = Math.min(pos1.getX(), pos2.getX()); x2 = Math.max(pos1.getX(), pos2.getX());
        y1 = Math.min(pos1.getY(), pos2.getY()); y2 = Math.max(pos1.getY(), pos2.getY());
        z1 = Math.min(pos1.getZ(), pos2.getZ()); z2 = Math.max(pos1.getZ(), pos2.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x1; x <= x2; x ++) {
            for (int y = y1; y <= y2; y ++) {
                for (int z = z1; z <= z2; z ++) {
                    pos.setPos(x, y, z);
                    world.setBlockState(pos, state, flags);
                }
            }
        }
    }
}
