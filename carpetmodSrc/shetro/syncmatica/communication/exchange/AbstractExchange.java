package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.communication.CommunicationManager;
import shetro.syncmatica.communication.ExchangeTarget;
import net.minecraft.network.PacketBuffer;

import java.util.UUID;

public abstract class AbstractExchange implements Exchange {
    private boolean success = false;
    private boolean finished = false;
    private final ExchangeTarget partner;
    private final Context context;

    protected AbstractExchange(final ExchangeTarget partner, final Context con) {
        this.partner = partner;
        context = con;
    }

    @Override
    public ExchangeTarget getPartner() {
        return partner;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isSuccessful() {
        return success;
    }

    @Override
    public void close(final boolean notifyPartner) {
        finished = true;
        success = false;
        onClose();
        if (notifyPartner) {
            sendCancelPacket();
        }
    }

    public CommunicationManager getManager() {
        return context.getCommunicationManager();
    }

    protected void sendCancelPacket() {
    }

    protected void onClose() {
    }

    protected void succeed() {
        finished = true;
        success = true;
        // Ctrl+C Ctrl+V and forget to adapt the success state - typical
        onClose();
    }

    protected static boolean checkUUID(final PacketBuffer sourceBuf, final UUID targetId) {
        final int r = sourceBuf.readerIndex();
        final UUID sourceId = sourceBuf.readUniqueId();
        sourceBuf.readerIndex(r);
        return sourceId.equals(targetId);
    }
}