package shetro.syncmatica.communication;

import shetro.syncmatica.Context;
import shetro.syncmatica.Feature;
import shetro.syncmatica.ServerPlacement;
import shetro.syncmatica.communication.exchange.DownloadExchange;
import shetro.syncmatica.communication.exchange.Exchange;
import shetro.syncmatica.extended_core.PlayerIdentifier;
import shetro.syncmatica.extended_core.PlayerIdentifierProvider;
import shetro.syncmatica.extended_core.SubRegionData;
import shetro.syncmatica.extended_core.SubRegionPlacementModification;
import shetro.syncmatica.util.SyncmaticaUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class CommunicationManager {
    protected final Collection<ExchangeTarget> broadcastTargets;

    // TODO: Refactor this bs
    protected final Map<UUID, Boolean> downloadState;
    protected final Map<UUID, Exchange> modifyState;

    protected Context context;

    protected static final Rotation[] rotOrdinals = Rotation.values();
    protected static final Mirror[] mirOrdinals = Mirror.values();

    protected CommunicationManager() {
        broadcastTargets = new ArrayList<>();
        downloadState = new HashMap<>();
        modifyState = new HashMap<>();
    }

    public boolean handlePacket(final String id) {
        return PacketType.containsIdentifier(id);
    }

    public void onPacket(final ExchangeTarget source, final String id, final PacketBuffer packetBuf) {
        context.getDebugService().logReceivePacket(id);
        Exchange handler = null;
        final Collection<Exchange> potentialMessageTarget = source.getExchanges();
        if (potentialMessageTarget != null) {
            for (final Exchange target : potentialMessageTarget) {
                if (target.checkPacket(id, packetBuf)) {
                    target.handle(id, packetBuf);
                    handler = target;
                    break;
                }
            }
        }
        if (handler == null) {
            handle(source, id, packetBuf);
        } else if (handler.isFinished()) {
            notifyClose(handler);
        }
    }

    // will get called for every packet not handled by an exchange
    protected abstract void handle(ExchangeTarget source, String id, PacketBuffer packetBuf);

    // will get called for every finished exchange (successful or not)
    protected abstract void handleExchange(Exchange exchange);

    public void sendMetaData(final ServerPlacement metaData, final ExchangeTarget target) {
        final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        putMetaData(metaData, buf, target);
        target.sendPacket(PacketType.REGISTER_METADATA.identifier, buf, context);
    }

    public void putMetaData(final ServerPlacement metaData, final PacketBuffer buf, final ExchangeTarget exchangeTarget) {
        buf.writeUniqueId(metaData.getId());

        buf.writeString(SyncmaticaUtil.sanitizeFileName(metaData.getName()));
        buf.writeUniqueId(metaData.getHash());

        if (exchangeTarget.getFeatureSet().hasFeature(Feature.CORE_EX)) {
            buf.writeUniqueId(metaData.getOwner().uuid);
            buf.writeString(metaData.getOwner().getName());
            buf.writeUniqueId(metaData.getLastModifiedBy().uuid);
            buf.writeString(metaData.getLastModifiedBy().getName());
        }

        putPositionData(metaData, buf, exchangeTarget);
    }

    public void putPositionData(final ServerPlacement metaData, final PacketBuffer buf, final ExchangeTarget exchangeTarget) {
        buf.writeBlockPos(metaData.getPosition());
        buf.writeString(metaData.getDimension());
        // one of the rare use cases for ordinal
        // transmitting the information of a non modifying enum to another
        // instance of this application with no regard to the persistence
        // of the ordinal values over time
        buf.writeInt(metaData.getRotation().ordinal());
        buf.writeInt(metaData.getMirror().ordinal());

        if (exchangeTarget.getFeatureSet().hasFeature(Feature.CORE_EX)) {
            if (metaData.getSubRegionData().getModificationData() == null) {
                buf.writeInt(0);

                return;
            }

            final Collection<SubRegionPlacementModification> regionData = metaData.getSubRegionData().getModificationData().values();
            buf.writeInt(regionData.size());

            for (final SubRegionPlacementModification subPlacement : regionData) {
                buf.writeString(subPlacement.name);
                buf.writeBlockPos(subPlacement.position);
                buf.writeInt(subPlacement.rotation.ordinal());
                buf.writeInt(subPlacement.mirror.ordinal());
            }
        }
    }

    public ServerPlacement receiveMetaData(final PacketBuffer buf, final ExchangeTarget exchangeTarget) {
        final UUID id = buf.readUniqueId();

        final String fileName = SyncmaticaUtil.sanitizeFileName(buf.readString(32767));
        final UUID hash = buf.readUniqueId();

        PlayerIdentifier owner = PlayerIdentifier.MISSING_PLAYER;
        PlayerIdentifier lastModifiedBy = PlayerIdentifier.MISSING_PLAYER;

        if (exchangeTarget.getFeatureSet().hasFeature(Feature.CORE_EX)) {
            final PlayerIdentifierProvider provider = context.getPlayerIdentifierProvider();
            owner = provider.createOrGet(
                    buf.readUniqueId(),
                    buf.readString(32767)
            );
            lastModifiedBy = provider.createOrGet(
                    buf.readUniqueId(),
                    buf.readString(32767)
            );
        }

        final ServerPlacement placement = new ServerPlacement(id, fileName, hash, owner);
        placement.setLastModifiedBy(lastModifiedBy);

        receivePositionData(placement, buf, exchangeTarget);

        return placement;
    }

    public void receivePositionData(final ServerPlacement placement, final PacketBuffer buf, final ExchangeTarget exchangeTarget) {
        final BlockPos pos = buf.readBlockPos();
        final String dimensionId = buf.readString(32767);
        final Rotation rot = rotOrdinals[buf.readInt()];
        final Mirror mir = mirOrdinals[buf.readInt()];
        placement.move(dimensionId, pos, rot, mir);

        if (exchangeTarget.getFeatureSet().hasFeature(Feature.CORE_EX)) {
            final SubRegionData subRegionData = placement.getSubRegionData();
            subRegionData.reset();
            final int limit = buf.readInt();
            for (int i = 0; i < limit; i++) {
                subRegionData.modify(
                        buf.readString(32767),
                        buf.readBlockPos(),
                        rotOrdinals[buf.readInt()],
                        mirOrdinals[buf.readInt()]
                );
            }
        }
    }

    public void download(final ServerPlacement syncmatic, final ExchangeTarget source) throws NoSuchAlgorithmException, IOException {
        if (!context.getFileStorage().getLocalState(syncmatic).isReadyForDownload()) {
            // forgot a negation here
            throw new IllegalArgumentException(syncmatic.toString() + " is not ready for download local state is: " + context.getFileStorage().getLocalState(syncmatic).toString());
        }
        final File toDownload = context.getFileStorage().createLocalLitematic(syncmatic);
        final Exchange downloadExchange = new DownloadExchange(syncmatic, toDownload, source, context);
        setDownloadState(syncmatic, true);
        startExchange(downloadExchange);
    }

    public void setDownloadState(final ServerPlacement syncmatic, final boolean b) {
        downloadState.put(syncmatic.getHash(), b);
    }

    public boolean getDownloadState(final ServerPlacement syncmatic) {
        return downloadState.getOrDefault(syncmatic.getHash(), false);
    }

    public void setModifier(final ServerPlacement syncmatic, final Exchange exchange) {
        modifyState.put(syncmatic.getHash(), exchange);
    }

    public Exchange getModifier(final ServerPlacement syncmatic) {
        return modifyState.get(syncmatic.getHash());
    }

    public void startExchange(final Exchange newExchange) {
        if (!broadcastTargets.contains(newExchange.getPartner())) {
            throw new IllegalArgumentException(newExchange.getPartner().toString() + " is not a valid ExchangeTarget");
        }
        startExchangeUnchecked(newExchange);
    }

    protected void startExchangeUnchecked(final Exchange newExchange) {
        newExchange.getPartner().getExchanges().add(newExchange);
        newExchange.init();
        if (newExchange.isFinished()) {
            notifyClose(newExchange);
        }
    }

    public void setContext(final Context con) {
        if (context == null) {
            context = con;
        } else {
            throw new Context.DuplicateContextAssignmentException("Duplicate Context Assignment");
        }
    }

    public void notifyClose(final Exchange e) {
        e.getPartner().getExchanges().remove(e);
        handleExchange(e);
    }
}