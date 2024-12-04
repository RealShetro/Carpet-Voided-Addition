package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.PacketType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

import java.io.*;

// uploading part of transmit data exchange
// pairs with Download Exchange

public class UploadExchange extends AbstractExchange {
    // The maximum buffer size for CustomPayloadPackets is actually 32767
    // so 32768 is a bad value to send - thus adjusted it to 16384 - exactly halved
    private static final int BUFFER_SIZE = 16384;

    private final ServerPlacement toUpload;
    private final InputStream inputStream;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    public UploadExchange(final ServerPlacement syncmatic, final File uploadFile, final ExchangeTarget partner, final Context con) throws FileNotFoundException {
        super(partner, con);
        toUpload = syncmatic;
        inputStream = new FileInputStream(uploadFile);
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.RECEIVED_LITEMATIC.identifier)
                || id.equals(PacketType.CANCEL_LITEMATIC.identifier)) {
            return checkUUID(packetBuf, toUpload.getId());
        }
        return false;
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {

        packetBuf.readUniqueId(); // uncertain if the data has to be consumed
        if (id.equals(PacketType.RECEIVED_LITEMATIC.identifier)) {
            send();
        }
        if (id.equals(PacketType.CANCEL_LITEMATIC.identifier)) {
            close(false);
        }
    }

    private void send() {
        // might fail when an empty file is attempted to be transmitted
        final int bytesRead;
        try {
            bytesRead = inputStream.read(buffer);
        } catch (final IOException e) {
            close(true);
            e.printStackTrace();
            return;
        }
        if (bytesRead == -1) {
            sendFinish();
        } else {
            sendData(bytesRead);
        }
    }

    private void sendData(final int bytesRead) {
        final PacketBuffer packetByteBuf = new PacketBuffer(Unpooled.buffer());
        packetByteBuf.writeUniqueId(toUpload.getId());
        packetByteBuf.writeInt(bytesRead);
        packetByteBuf.writeBytes(buffer, 0, bytesRead);
        getPartner().sendPacket(PacketType.SEND_LITEMATIC.identifier, packetByteBuf, getContext());
    }

    private void sendFinish() {
        final PacketBuffer packetByteBuf = new PacketBuffer(Unpooled.buffer());
        packetByteBuf.writeUniqueId(toUpload.getId());
        getPartner().sendPacket(PacketType.FINISHED_LITEMATIC.identifier, packetByteBuf, getContext());
        succeed();
    }

    @Override
    public void init() {
        send();
    }

    @Override
    protected void onClose() {
        try {
            inputStream.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void sendCancelPacket() {
        final PacketBuffer packetByteBuf = new PacketBuffer(Unpooled.buffer());
        packetByteBuf.writeUniqueId(toUpload.getId());
        getPartner().sendPacket(PacketType.CANCEL_LITEMATIC.identifier, packetByteBuf, getContext());
    }
}