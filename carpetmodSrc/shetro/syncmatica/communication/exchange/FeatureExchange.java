package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.FeatureSet;
import shetro.syncmatica.communication.PacketType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

public abstract class FeatureExchange extends AbstractExchange {
    protected FeatureExchange(final ExchangeTarget partner, final Context con) {
        super(partner, con);
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        return id.equals(PacketType.FEATURE_REQUEST.identifier)
                || id.equals(PacketType.FEATURE.identifier);
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.FEATURE_REQUEST.identifier)) {
            sendFeatures();
        } else if (id.equals(PacketType.FEATURE.identifier)) {
            final FeatureSet fs = FeatureSet.fromString(packetBuf.readString(32767));
            getPartner().setFeatureSet(fs);
            onFeatureSetReceive();
        }
    }

    protected void onFeatureSetReceive() {
        succeed();
    }

    public void requestFeatureSet() {
        getPartner().sendPacket(PacketType.FEATURE_REQUEST.identifier, new PacketBuffer(Unpooled.buffer()), getContext());
    }

    private void sendFeatures() {
        final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        final FeatureSet fs = getContext().getFeatureSet();
        buf.writeString(fs.toString());
        getPartner().sendPacket(PacketType.FEATURE.identifier, buf, getContext());
    }
}