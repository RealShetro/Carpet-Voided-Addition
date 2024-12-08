package redstone.multimeter.common.network.packets;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import redstone.multimeter.common.network.RSMMPacket;
import redstone.multimeter.server.MultimeterServer;

public class MeterIndexPacket implements RSMMPacket {

	private long id;
	private int index;

	public MeterIndexPacket() {
	}

	public MeterIndexPacket(long id, int index) {
		this.id = id;
		this.index = index;
	}

	@Override
	public void encode(NBTTagCompound data) {
		data.setLong("id", id);
		data.setInteger("index", index);
	}

	@Override
	public void decode(NBTTagCompound data) {
		id = data.getLong("id");
		index = data.getInteger("index");
	}

	@Override
	public void handle(MultimeterServer server, EntityPlayerMP player) {
		server.getMultimeter().setMeterIndex(player, id, index);
	}
}
