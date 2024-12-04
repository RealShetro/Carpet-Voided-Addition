package shetro.syncmatica.communication.exchange;

import shetro.syncmatica.Context;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.ExchangeTarget;
import shetro.syncmatica.communication.PacketType;
import shetro.syncmatica.extended_core.PlayerIdentifier;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

import java.util.UUID;

public class ModifyExchangeServer extends AbstractExchange {
    private final ServerPlacement placement;
    UUID placementId;

    public ModifyExchangeServer(final UUID placeId, final ExchangeTarget partner, final Context con) {
        super(partner, con);
        placementId = placeId;
        placement = con.getSyncmaticManager().getPlacement(placementId);
    }

    @Override
    public boolean checkPacket(final String id, final PacketBuffer packetBuf) {
        return id.equals(PacketType.MODIFY_FINISH.identifier) && checkUUID(packetBuf, placement.getId());
    }

    @Override
    public void handle(final String id, final PacketBuffer packetBuf) {
        packetBuf.readUniqueId(); // consume uuid
        if (id.equals(PacketType.MODIFY_FINISH.identifier)) {
            getContext().getCommunicationManager().receivePositionData(placement, packetBuf, getPartner());

            final PlayerIdentifier identifier = getContext().getPlayerIdentifierProvider().createOrGet(
                    getPartner()
            );
            placement.setLastModifiedBy(identifier);
            getContext().getSyncmaticManager().updateServerPlacement(placement);
            succeed();
        }
    }

    @Override
    public void init() {
        if (getPlacement() == null || getContext().getCommunicationManager().getModifier(placement) != null) {
            close(true); // equivalent to deny
        } else {
            accept();
        }
    }

    private void accept() {
        final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeUniqueId(placement.getId());
        getPartner().sendPacket(PacketType.MODIFY_REQUEST_ACCEPT.identifier, buf, getContext());
        getContext().getCommunicationManager().setModifier(placement, this);
    }

    @Override
    protected void sendCancelPacket() {
        final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeUniqueId(placementId);
        getPartner().sendPacket(PacketType.MODIFY_REQUEST_DENY.identifier, buf, getContext());
    }

    public ServerPlacement getPlacement() {
        return placement;
    }

    @Override
    protected void onClose() {
        if (getContext().getCommunicationManager().getModifier(placement) == this) {
            getContext().getCommunicationManager().setModifier(placement, null);
        }
    }
}