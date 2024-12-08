package redstone.multimeter.server;

import java.io.File;
import java.util.UUID;

import carpet.helpers.TickSpeed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import redstone.multimeter.RedstoneMultimeter;
import redstone.multimeter.common.DimPos;
import redstone.multimeter.common.TickPhase;
import redstone.multimeter.common.TickPhaseTree;
import redstone.multimeter.common.TickTask;
import redstone.multimeter.common.network.packets.HandshakePacket;
import redstone.multimeter.common.network.packets.TickTimePacket;
import redstone.multimeter.common.network.packets.TickPhaseTreePacket;

public class MultimeterServer {

	private final MinecraftServer server;
	private final ServerPacketHandler packetHandler;
	private final PlayerList playerList;
	private final Multimeter multimeter;
	private final TickPhaseTree tickPhaseTree;

    private boolean loaded;
	private TickPhase tickPhase;

	public MultimeterServer(MinecraftServer server) {
		this.server = server;
		this.packetHandler = new ServerPacketHandler(this);
		this.playerList = new PlayerList(this);
		this.multimeter = new Multimeter(this);
		this.tickPhaseTree = new TickPhaseTree();

		this.tickPhase = TickPhase.UNKNOWN;
	}

	public MinecraftServer getMinecraftServer() {
		return server;
	}

	public ServerPacketHandler getPacketHandler() {
		return packetHandler;
	}

	public Multimeter getMultimeter() {
		return multimeter;
	}

	public TickPhaseTree getTickPhaseTree() {
		return tickPhaseTree;
	}

    public long getTicks() {
		return server.getTickCounter();
	}

	public boolean isDedicated() {
		return server.isDedicatedServer();
	}

	public File getConfigDirectory() {
		return new File(server.getDataDirectory(), RedstoneMultimeter.CONFIG_PATH);
	}

	public TickPhase getTickPhase() {
		return tickPhase;
	}

	public void worldLoaded() {
		loaded = true;
	}

	public void startTickTask(TickTask task, String... args) {
		tickPhase = tickPhase.startTask(task);
		if (tickPhaseTree.isBuilding()) {
			tickPhaseTree.startTask(task, args);
		}
	}

	public void endTickTask() {
		tickPhase = tickPhase.endTask();
		if (tickPhaseTree.isBuilding()) {
			tickPhaseTree.endTask();
		}
	}

	public void swapTickTask(TickTask task, String... args) {
		tickPhase = tickPhase.swapTask(task);
		if (tickPhaseTree.isBuilding()) {
			tickPhaseTree.swapTask(task, args);
		}
	}

	public TickTask getCurrentTickTask() {
		return tickPhase.peekTask();
	}

	public boolean isPaused() {
		return false; // integrated servers only
	}

	public boolean isPausedOrFrozen() {
		return isPaused() || !TickSpeed.process_entities;
	}

	public void tickStart() {
		boolean paused = isPaused();

		if (!paused) {
			if (shouldBuildTickPhaseTree()) {
				tickPhaseTree.start();
			}

			playerList.tick();
		}

		tickPhase = TickPhase.UNKNOWN;
		multimeter.tickStart(paused);
	}

	private boolean shouldBuildTickPhaseTree() {
		return loaded && !tickPhaseTree.isComplete() && !tickPhaseTree.isBuilding() && !isPausedOrFrozen() && !playerList.get().isEmpty();
	}

	public void tickEnd() {
		boolean paused = isPaused();

		if (tickPhaseTree.isBuilding()) {
			tickPhaseTree.end();
		}

		tickPhase = TickPhase.UNKNOWN;
		multimeter.tickEnd(paused);
	}

    public void tickTime(World world) {
		TickTimePacket packet = new TickTimePacket(world.getWorldTime());
		playerList.send(packet, world.provider.getDimensionType());
	}

	public void onHandshake(EntityPlayerMP player, String modVersion) {
		if (!playerList.has(player.getUniqueID())) {
			playerList.add(player);
			HandshakePacket packet = new HandshakePacket();
			playerList.send(packet, player);
		}
	}

	public void onPlayerJoin(EntityPlayerMP player) {
		multimeter.onPlayerJoin(player);
	}

	public void onPlayerLeave(EntityPlayerMP player) {
		multimeter.onPlayerLeave(player);
	}

	public void refreshTickPhaseTree(EntityPlayerMP player) {
		if (tickPhaseTree.isComplete()) {
			TickPhaseTreePacket packet = new TickPhaseTreePacket(tickPhaseTree.toNbt());
			playerList.send(packet, player);
		}
	}

	public void rebuildTickPhaseTree(EntityPlayerMP player) {
		if (tickPhaseTree.isComplete()) {
			tickPhaseTree.reset();
		}
	}

	public WorldServer[] getWorlds() {
		return server.worlds;
	}

	public WorldServer getWorld(String key) {
		return server.getWorld(DimensionType.byName(key).getId());
	}
	public WorldServer getWorld(DimPos pos) {
		return getWorld(pos.getDimension());
	}

	public IBlockState getIBlockState(DimPos pos) {
		World world = getWorld(pos);
		if (world == null) {
			return null;
		}
		return world.getBlockState(pos.getBlockPos());
	}

	public PlayerList getPlayerList() {
		return playerList;
	}

	public boolean isMultimeterClient(UUID uuid) {
		return playerList.has(uuid);
	}

	public boolean isMultimeterClient(EntityPlayerMP player) {
		return playerList.has(player.getUniqueID());
	}

	public void sendMessage(EntityPlayerMP player, ITextComponent message, boolean actionBar) {
		player.sendStatusMessage(message, actionBar);
	}
}
