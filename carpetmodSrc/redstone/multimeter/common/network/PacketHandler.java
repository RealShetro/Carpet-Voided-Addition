package redstone.multimeter.common.network;

import java.io.IOException;

import io.netty.buffer.Unpooled;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public abstract class PacketHandler {

	public Packet<?> encode(RSMMPacket packet) {
		String key = Packets.getKey(packet);

		if (key == null) {
			throw new IllegalStateException("Unable to encode packet: " + packet.getClass());
		}

		NBTTagCompound data = new NBTTagCompound();
		packet.encode(data);

		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());

		buffer.writeString(key);
		buffer.writeCompoundTag(data);

		return toCustomPayload(Packets.getChannel(), buffer);
	}

	protected abstract Packet<?> toCustomPayload(String channel, PacketBuffer data);

	protected RSMMPacket decode(PacketBuffer buffer) throws IOException {
		String key = buffer.readString(32767);
		RSMMPacket packet = Packets.create(key);

		if (packet == null) {
			throw new IllegalStateException("Unable to decode packet: " + key);
		}

		NBTTagCompound data = buffer.readCompoundTag();
		packet.decode(data);

		return packet;
	}
}
