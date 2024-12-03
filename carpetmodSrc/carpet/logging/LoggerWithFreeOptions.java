package carpet.logging;

import carpet.logging.LogHandler;
import carpet.logging.Logger;
import net.minecraft.server.MinecraftServer;

public class LoggerWithFreeOptions extends Logger {

    public LoggerWithFreeOptions(MinecraftServer server, String logName, LogHandler defaultHandler) {
        super(server, logName, "", new String[0], defaultHandler);
    }

    @Override
    public String getAcceptedOption(String arg) {
        return arg;
    }
}
