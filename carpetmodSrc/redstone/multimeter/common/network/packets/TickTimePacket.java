package redstone.multimeter.common.network.packets;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import redstone.multimeter.common.network.RSMMPacket;
import redstone.multimeter.server.MultimeterServer;

public class TickTimePacket implements RSMMPacket {

	private long gameTime;

	public TickTimePacket() {
	}

	public TickTimePacket(long serverTime) {
		this.gameTime = serverTime;
	}

	@Override
	public void encode(NBTTagCompound data) {
		data.setLong("game time", gameTime);
	}

	@Override
	public void decode(NBTTagCompound data) {
		gameTime = data.getLong("game time");
	}

	@Override
	public void handle(MultimeterServer server, EntityPlayerMP player) {
	}
}
