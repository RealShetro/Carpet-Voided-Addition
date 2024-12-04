package shetro.syncmatica.service;

import shetro.syncmatica.Syncmatica;
import org.apache.logging.log4j.LogManager;

public class DebugService extends AbstractService {
    private boolean doPacketLogging = false;

    public void logReceivePacket(final String packageType) {
        if (doPacketLogging) {
            LogManager.getLogger(Syncmatica.class).info("Syncmatica - received packet:[type={}]", packageType);
        }
    }

    public void logSendPacket(final String packetType, final String targetIdentifier) {
        if (doPacketLogging) {
            LogManager.getLogger(Syncmatica.class).info(
                    "Sending packet[type={}] to ExchangeTarget[id={}]",
                    packetType,
                    targetIdentifier
            );
        }
    }

    @Override
    public void getDefaultConfiguration(final IServiceConfiguration configuration) {
        configuration.saveBoolean("doPackageLogging", false);
    }

    @Override
    public String getConfigKey() {
        return "debug";
    }

    @Override
    public void configure(final IServiceConfiguration configuration) {
        configuration.loadBoolean("doPackageLogging", b -> doPacketLogging = b);
    }

    @Override
    public void startup() { //NOSONAR
    }

    @Override
    public void shutdown() { //NOSONAR
    }
}