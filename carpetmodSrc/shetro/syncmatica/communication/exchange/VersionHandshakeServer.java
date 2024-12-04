package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.Syncmatica;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.FeatureSet;
import shetro.syncmatica.communication.PacketType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;

import java.util.Collection;

public class VersionHandshakeServer extends FeatureExchange {
    private String partnerVersion;

    public VersionHandshakeServer(final ExchangeTarget partner, final Context con) {
        super(partner, con);
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        return id.equals(PacketType.REGISTER_VERSION.identifier)
                || super.checkPacket(id, packetBuf);
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {
        if (id.equals(PacketType.REGISTER_VERSION.identifier)) {
            partnerVersion = packetBuf.readString(32767);
            if (!getContext().checkPartnerVersion(partnerVersion)) {
                LogManager.getLogger(VersionHandshakeServer.class).info("Denying syncmatica join due to outdated client with local version {} and client version {}", Syncmatica.VERSION, partnerVersion);
                // same as client - avoid further packets
                close(false);
                return;
            }
            final FeatureSet fs = FeatureSet.fromVersionString(partnerVersion);
            if (fs == null) {
                requestFeatureSet();
            } else {
                getPartner().setFeatureSet(fs);
                onFeatureSetReceive();
            }
        } else {
            super.handle(id, packetBuf);
        }

    }

    @Override
    public void onFeatureSetReceive() {
        LogManager.getLogger(VersionHandshakeServer.class).info("Syncmatica client joining with local version {} and client version {}", Syncmatica.VERSION, partnerVersion);
        final PacketBuffer newBuf = new PacketBuffer(Unpooled.buffer());
        final Collection<ServerPlacement> l = getContext().getSyncmaticManager().getAll();
        newBuf.writeInt(l.size());
        for (final ServerPlacement p : l) {
            getManager().putMetaData(p, newBuf, getPartner());
        }
        getPartner().sendPacket(PacketType.CONFIRM_USER.identifier, newBuf, getContext());
        succeed();
    }

    @Override
    public void init() {
        final PacketBuffer newBuf = new PacketBuffer(Unpooled.buffer());
        newBuf.writeString(Syncmatica.VERSION);
        getPartner().sendPacket(PacketType.REGISTER_VERSION.identifier, newBuf, getContext());
    }
}