package redstone.multimeter.common.network.packets;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import redstone.multimeter.common.network.RSMMPacket;
import redstone.multimeter.server.MultimeterServer;

public class RebuildTickPhaseTreePacket implements RSMMPacket {

	public RebuildTickPhaseTreePacket() {
	}

	@Override
	public void encode(NBTTagCompound data) {
	}

	@Override
	public void decode(NBTTagCompound data) {
	}

	@Override
	public void handle(MultimeterServer server, EntityPlayerMP player) {
		server.rebuildTickPhaseTree(player);
	}
}
