package redstone.multimeter.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;

import redstone.multimeter.common.network.PacketHandler;

public class ServerPacketHandler extends PacketHandler {

	private final MultimeterServer server;

	public ServerPacketHandler(MultimeterServer server) {
		this.server = server;
	}

	@Override
	protected Packet<?> toCustomPayload(String channel, PacketBuffer data) {
		return new SPacketCustomPayload(channel, data);
	}

	public void handlePacket(PacketBuffer data, EntityPlayerMP player) {
		try {
			decode(data).handle(server, player);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
