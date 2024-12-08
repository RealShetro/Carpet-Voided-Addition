package redstone.multimeter.common.network.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;

import redstone.multimeter.common.meter.MeterProperties;
import redstone.multimeter.common.network.RSMMPacket;
import redstone.multimeter.server.MultimeterServer;
import redstone.multimeter.util.NbtUtils;

public class MeterUpdatesPacket implements RSMMPacket {
	
	private List<Long> removedMeters;
	private Long2ObjectMap<MeterProperties> meterUpdates;
	private List<Long> meters;
	
	public MeterUpdatesPacket() {
		this.removedMeters = new ArrayList<>();
		this.meterUpdates = new Long2ObjectOpenHashMap<>();
		this.meters = new ArrayList<>();
	}
	
	public MeterUpdatesPacket(List<Long> removedMeters, Map<Long, MeterProperties> updates, List<Long> meters) {
		this.removedMeters = new ArrayList<>(removedMeters);
		this.meterUpdates = new Long2ObjectOpenHashMap<>(updates);
		this.meters = new ArrayList<>(meters);
	}
	
	@Override
	public void encode(NBTTagCompound data) {
		if (!removedMeters.isEmpty()) {
			NBTTagList list = new NBTTagList();

			for (int i = 0; i < removedMeters.size(); i++) {
				list.appendTag(new NBTTagLong(removedMeters.get(i)));
			}

			data.setTag("removed", list);
		}
		if (!meterUpdates.isEmpty()) {
			NBTTagList list = new NBTTagList();

			for (Entry<MeterProperties> entry : meterUpdates.long2ObjectEntrySet()) {
				long id = entry.getLongKey();
				MeterProperties update = entry.getValue();

				NBTTagCompound nbt = update.toNbt();
				nbt.setLong("id", id);
				list.appendTag(nbt);
			}

			data.setTag("updates", list);
		}
		if (!meters.isEmpty()) {
			NBTTagList list = new NBTTagList();
			for (int i = 0; i < meters.size(); i++) {
				list.appendTag(new NBTTagLong(meters.get(i)));
			}
			data.setTag("meters", list);
		}
	}
	
	@Override
	public void decode(NBTTagCompound data) {
		if (data.hasKey("removed")) {
			NBTTagList ids = data.getTagList("removed", NbtUtils.TYPE_LONG);
			for (int i = 0; i < ids.tagCount(); i++) {
				NBTTagLong nbt = (NBTTagLong)ids.get(i);
				long id = nbt.getLong();
				removedMeters.add(id);
			}
		}
		if (data.hasKey("updates")) {
			NBTTagList updates = data.getTagList("updates", NbtUtils.TYPE_COMPOUND);
			for (int i = 0; i < updates.tagCount(); i++) {
				NBTTagCompound nbt = updates.getCompoundTagAt(i);
				long id = nbt.getLong("id");
				MeterProperties update = MeterProperties.fromNbt(nbt);
				meterUpdates.put(id, update);
			}
		}
		if (data.hasKey("meters")) {
			NBTTagList ids = data.getTagList("meters", NbtUtils.TYPE_LONG);
			for (int i = 0; i < ids.tagCount(); i++) {
				NBTTagLong nbt = (NBTTagLong)ids.get(i);
				long id = nbt.getLong();
				meters.add(id);
			}
		}
	}
	
	@Override
	public void handle(MultimeterServer server, EntityPlayerMP player) {
		
	}
}
