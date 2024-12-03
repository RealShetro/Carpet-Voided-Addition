package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.BlockStateFlatteningMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

public class CommandSetBlock13 extends CommandCarpetBase {
    @Override
    public String getName() {
        return "setblock13";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/setblock13 <x> <y> <z> block[states]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length <= 3) return getTabCompletionCoordinate(args, 0, targetPos);
        return super.getTabCompletions(server, sender, args, targetPos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandSetblock13", sender)) return;
        if (!sender.canUseCommand(2, "setblock")) {
            notifyCommandListener(sender, this,
                    "You don't have permission to use /setblock! ");
            return;
        }
        if (args.length < 4) throw new CommandException(getUsage(sender));
        BlockPos pos = parseBlockPos(sender, args, 0, false);
        IBlockState state = BlockStateFlatteningMap.deflattenBrigadierBlockStateToState(args[3]);
        if (state == null) throw new CommandException("Unable to resolve blockstate " + args[3]);
        int flags = CarpetSettings.fillUpdates ? 2 : 130;
        sender.getEntityWorld().setBlockState(pos, state, flags);
    }
}
