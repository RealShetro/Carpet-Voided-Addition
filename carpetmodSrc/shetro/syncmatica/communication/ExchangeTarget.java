package shetro.syncmatica.communication;

import shetro.syncmatica.Context;
import shetro.syncmatica.communication.exchange.Exchange;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.NetHandlerPlayServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// since Client/Server PlayNetworkHandler are 2 different classes, but I want to use exchanges
// on both without having to recode them individually I have an adapter class here

public class ExchangeTarget {
    private NetHandlerPlayServer client = null;
    private final String persistentName;

    private FeatureSet features;
    private final List<Exchange> ongoingExchanges = new ArrayList<>(); // implicitly relies on priority

    public ExchangeTarget(final NetHandlerPlayServer client) {
        this.client = client;
        persistentName = client.player.getCachedUniqueIdString();
    }

    // this application exclusively communicates in CustomPayLoad packets
    // this class handles the sending of either S2C or C2S packets
    public void sendPacket(final String id, final PacketBuffer packetBuf, final Context context) {
        context.getDebugService().logSendPacket(id, persistentName);
        final SPacketCustomPayload packet = new SPacketCustomPayload(id, packetBuf);
        client.sendPacket(packet);
    }

    // removed equals code due to issues with Collection.contains

    public FeatureSet getFeatureSet() {
        return features;
    }

    public void setFeatureSet(final FeatureSet f) {
        features = f;
    }

    public Collection<Exchange> getExchanges() {
        return ongoingExchanges;
    }

    public String getPersistentName() {
        return persistentName;
    }
}