package redstone.multimeter.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.world.DimensionType;

import redstone.multimeter.common.network.RSMMPacket;
import redstone.multimeter.server.meter.ServerMeterGroup;

public class PlayerList {

	private final MultimeterServer server;

	private final Map<UUID, EntityPlayerMP> playersByUuid;
	private final Map<String, EntityPlayerMP> playersByName;
	private final Map<UUID, String> nameCache;

	public PlayerList(MultimeterServer server) {
		this.server = server;

		this.playersByUuid = new HashMap<>();
		this.playersByName = new HashMap<>();
		this.nameCache = new HashMap<>();
	}

	public MultimeterServer getServer() {
		return server;
	}

	public void tick() {
		if (server.getTicks() % 72000 == 0) {
			cleanNameCache();
		}
	}

	private void cleanNameCache() {
		Collection<ServerMeterGroup> meterGroups = server.getMultimeter().getMeterGroups();

		nameCache.keySet().removeIf(uuid -> {
			for (ServerMeterGroup meterGroup : meterGroups) {
				if (meterGroup.hasMember(uuid)) {
					return false;
				}
			}

			return true;
		});
	}

	public void add(EntityPlayerMP player) {
		if (!has(player.getUniqueID())) {
			playersByUuid.put(player.getUniqueID(), player);
			playersByName.put(player.getName(), player);
			nameCache.remove(player.getUniqueID());

			server.onPlayerJoin(player);
		}
	}

	public void remove(EntityPlayerMP player) {
		if (has(player.getUniqueID())) {
			playersByUuid.remove(player.getUniqueID());
			playersByName.remove(player.getName());
			nameCache.put(player.getUniqueID(), player.getName());

			server.onPlayerLeave(player);
		}
	}

	public void respawn(EntityPlayerMP player) {
		if (has(player.getUniqueID())) {
			playersByUuid.put(player.getUniqueID(), player);
			playersByName.put(player.getName(), player);
		}
	}

	public Collection<EntityPlayerMP> get() {
		return playersByUuid.values();
	}

	public EntityPlayerMP get(UUID uuid) {
		return playersByUuid.get(uuid);
	}

	public EntityPlayerMP get(String name) {
		return playersByName.get(name);
	}

	public boolean has(UUID uuid) {
		return playersByUuid.containsKey(uuid);
	}

	public boolean has(String name) {
		return playersByName.containsKey(name);
	}

	public String getName(UUID uuid) {
		EntityPlayerMP player = get(uuid);
		return player == null ? nameCache.get(uuid) : player.getName();
	}

	public void send(RSMMPacket packet) {
		send(packet, player -> true);
	}

	public void send(RSMMPacket packet, ServerMeterGroup meterGroup) {
		send(packet, player -> meterGroup.hasSubscriber(player));
	}

	public void send(RSMMPacket packet, DimensionType dimension) {
		send(packet, player -> player.world.provider.getDimensionType() == dimension);
	}

	public void send(RSMMPacket packet, Predicate<EntityPlayerMP> predicate) {
		Packet<?> mcPacket = server.getPacketHandler().encode(packet);

		for (EntityPlayerMP player : playersByUuid.values()) {
			if (predicate.test(player)) {
				player.connection.sendPacket(mcPacket);
			}
		}
	}

	public void send(RSMMPacket packet, EntityPlayerMP player) {
		Packet<?> mcPacket = server.getPacketHandler().encode(packet);
		player.connection.sendPacket(mcPacket);
	}

	public void updatePermissions(EntityPlayerMP player) {
		server.getMinecraftServer().getPlayerList().updatePermissionLevel(player);
	}
}
