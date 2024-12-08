package redstone.multimeter.common.network.packets;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import redstone.multimeter.common.network.RSMMPacket;
import redstone.multimeter.server.MultimeterServer;
import redstone.multimeter.util.NbtUtils;

public class MeterLogsPacket implements RSMMPacket {

	private NBTTagList logsData;

	public MeterLogsPacket() {
	}

	public MeterLogsPacket(NBTTagList logsData) {
		this.logsData = logsData;
	}

	@Override
	public void encode(NBTTagCompound data) {
		data.setTag("logs", logsData);
	}

	@Override
	public void decode(NBTTagCompound data) {
		logsData = data.getTagList("logs", NbtUtils.TYPE_COMPOUND);
	}

	@Override
	public void handle(MultimeterServer server, EntityPlayerMP player) {
	}
}
