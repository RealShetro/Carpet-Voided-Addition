package redstone.multimeter.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import redstone.multimeter.RedstoneMultimeter;
import redstone.multimeter.common.meter.MeterGroup;
import redstone.multimeter.server.Multimeter;
import redstone.multimeter.server.MultimeterServer;
import redstone.multimeter.server.meter.ServerMeterGroup;

public class MeterGroupCommand extends CommandBase {

	private static final String COMMAND_NAME = "metergroup";
	
	private static final String USAGE_LIST              = singleUsage("list");

	private static final String USAGE_SUBSCRIBE_DEFAULT = singleUsage("subscribe");
	private static final String USAGE_SUBSCRIBE_NAME    = singleUsage("subscribe <name>");
	private static final String USAGE_SUBSCRIBE         = buildUsage(USAGE_SUBSCRIBE_DEFAULT, USAGE_SUBSCRIBE_NAME);

	private static final String USAGE_UNSUBSCRIBE       = singleUsage("unsubscribe");

	private static final String USAGE_PRIVATE_QUERY     = singleUsage("private");
	private static final String USAGE_PRIVATE_SET       = singleUsage("private <private true|false>");
	private static final String USAGE_PRIVATE           = buildUsage(USAGE_PRIVATE_QUERY, USAGE_PRIVATE_SET);

	private static final String USAGE_MEMBERS_LIST      = singleUsage("members list");
	private static final String USAGE_MEMBERS_ADD       = singleUsage("members add <player>");
	private static final String USAGE_MEMBERS_REMOVE    = singleUsage("members remove <player>");
	private static final String USAGE_MEMBERS_CLEAR     = singleUsage("members clear");
	private static final String USAGE_MEMBERS           = buildUsage(USAGE_MEMBERS_LIST, USAGE_MEMBERS_ADD, USAGE_MEMBERS_REMOVE, USAGE_MEMBERS_CLEAR);

	private static final String USAGE_CLEAR             = singleUsage("clear");

	private static final String TOTAL_USAGE_MEMBER      = buildUsage(USAGE_LIST, USAGE_SUBSCRIBE, USAGE_UNSUBSCRIBE, USAGE_CLEAR);
	private static final String TOTAL_USAGE_OWNER       = buildUsage(USAGE_LIST, USAGE_SUBSCRIBE, USAGE_UNSUBSCRIBE, USAGE_PRIVATE, USAGE_MEMBERS, USAGE_CLEAR);

	private static String singleUsage(String usage) {
		return String.format("/%s %s", COMMAND_NAME, usage);
	}

	private static String buildUsage(String... usages) {
		return String.join(" OR ", usages);
	}

	private final MultimeterServer server;
	private final Multimeter multimeter;

	public MeterGroupCommand(MultimeterServer server) {
		this.server = server;
		this.multimeter = this.server.getMultimeter();
	}

	@Override
	public String getName() {
		return COMMAND_NAME;
	}

	@Override
	public String getUsage(ICommandSender source) {
		return isOwnerOfSubscription(source) ? TOTAL_USAGE_OWNER : TOTAL_USAGE_MEMBER;
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender source, String[] args, BlockPos pos) {
		boolean isOwner = isOwnerOfSubscription(source);

		switch (args.length) {
		case 1:
			if (isOwner) {
				return getListOfStringsMatchingLastWord(args, "clear", "subscribe", "unsubscribe", "private", "members", "list");
			} else {
				return getListOfStringsMatchingLastWord(args, "clear", "subscribe", "unsubscribe", "list");
			}
		case 2:
			switch (args[0]) {
			case "subscribe":
				return getListOfStringsMatchingLastWord(args, listMeterGroups(source));
			case "private":
				if (isOwner) {
					return getListOfStringsMatchingLastWord(args, "true", "false");
				}

				break;
			case "members":
				if (isOwner) {
					return getListOfStringsMatchingLastWord(args, "clear", "add", "remove", "list");
				}

				break;
			}

			break;
		case 3:
			if (isOwner && args[0].equals("members")) {
				switch (args[1]) {
				case "add":
					return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
				case "remove":
					return getListOfStringsMatchingLastWord(args, listMembers(source).keySet());
				}
			}

			break;
		}

		return Collections.emptyList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender source, String[] args) throws CommandException {
		if (!isMultimeterClient(source)) {
			throw new CommandNotFoundException();
		}

		if (args.length > 0) {
			switch (args[0]) {
			case "list":
				if (args.length == 1) {
					list(source);
					return;
				}

				throw new WrongUsageException(USAGE_LIST);
			case "subscribe":
				if (args.length == 1) {
					subscribe(source, null);
					return;
				}

				String name = "";

				for (int index = 1; index < args.length; index++) {
					name += args[index] + " ";
				}

				subscribe(source, name);
				return;
			case "unsubscribe":
				if (args.length == 1) {
					unsubscribe(source);
					return;
				}

				throw new WrongUsageException(USAGE_UNSUBSCRIBE);
			case "private":
				if (!isOwnerOfSubscription(source)) {
					break;
				}

				switch (args.length) {
				case 1:
					queryPrivate(source);
					return;
				case 2:
					switch (args[1]) {
					case "true":
						setPrivate(source, true);
						return;
					case "false":
						setPrivate(source, false);
						return;
					}

					throw new WrongUsageException(USAGE_PRIVATE_SET);
				}

				throw new WrongUsageException(USAGE_PRIVATE);
			case "members":
				if (!isOwnerOfSubscription(source)) {
					break;
				}

				if (args.length > 1) {
					switch (args[1]) {
					case "list":
						if (args.length == 2) {
							membersList(source);
							return;
						}

						throw new WrongUsageException(USAGE_MEMBERS_LIST);
					case "add":
						if (args.length == 3) {
							membersAdd(source, getPlayers(server, source, args[2]));
							return;
						}

						throw new WrongUsageException(USAGE_MEMBERS_ADD);
					case "remove":
						if (args.length == 3) {
							membersRemovePlayer(source, args[2]);
							return;
						}

						throw new WrongUsageException(USAGE_MEMBERS_REMOVE);
					case "clear":
						if (args.length == 2) {
							membersClear(source);
							return;
						}

						throw new WrongUsageException(USAGE_MEMBERS_CLEAR);
					}
				}

				throw new WrongUsageException(USAGE_MEMBERS);
			case "clear":
				if (args.length == 1) {
					clear(source);
					return;
				}

				throw new WrongUsageException(USAGE_CLEAR);
			}
		}

		throw new WrongUsageException(getUsage(source));
	}

	private boolean isMultimeterClient(ICommandSender source) {
		return run(source, player -> server.isMultimeterClient(player));
	}

	private boolean isOwnerOfSubscription(ICommandSender source) {
		return run(source, player -> multimeter.isOwnerOfSubscription(player));
	}

	private Collection<String> listMeterGroups(ICommandSender source) {
		List<String> names = new ArrayList<>();

		command(source, player -> {
			for (ServerMeterGroup meterGroup : multimeter.getMeterGroups()) {
				if (!meterGroup.isPrivate() || meterGroup.hasMember(player) || meterGroup.isOwnedBy(player)) {
					names.add(meterGroup.getName());
				}
			}
		});

		return names;
	}

	private Map<String, UUID> listMembers(ICommandSender source) {
		Map<String, UUID> names = new HashMap<>();
		
		command(source, player -> {
			ServerMeterGroup meterGroup = multimeter.getSubscription(player);

			if (meterGroup != null && meterGroup.isOwnedBy(player)) {
				for (UUID playerUUID : meterGroup.getMembers()) {
					String playerName = multimeter.getServer().getPlayerList().getName(playerUUID);

					if (playerName != null) {
						names.put(playerName, playerUUID);
					}
				}
			}
		});

		return names;
	}

	private void list(ICommandSender source) {
		Collection<String> names = listMeterGroups(source);

		if (names.isEmpty()) {
			source.sendMessage(new TextComponentString("There are no meter groups yet!"));
		} else {
			String message = "Meter groups:\n  " + String.join("\n  ", names);
			source.sendMessage(new TextComponentString(message));
		}
	}

	private void subscribe(ICommandSender source, String name) {
		command(source, player -> {
			if (name == null) {
				multimeter.subscribeToDefaultMeterGroup(player);
				source.sendMessage(new TextComponentString("Subscribed to default meter group"));
			} else if (multimeter.hasMeterGroup(name)) {
				ServerMeterGroup meterGroup = multimeter.getMeterGroup(name);

				if (!meterGroup.isPrivate() || meterGroup.hasMember(player) || meterGroup.isOwnedBy(player)) {
					multimeter.subscribeToMeterGroup(meterGroup, player);
					source.sendMessage(new TextComponentString(String.format("Subscribed to meter group \'%s\'", name)));
				} else {
					source.sendMessage(new TextComponentString("That meter group is private!"));
				}
			} else {
				if (MeterGroup.isValidName(name)) {
					multimeter.createMeterGroup(player, name);
					source.sendMessage(new TextComponentString(String.format("Created meter group \'%s\'", name)));
				} else {
					source.sendMessage(new TextComponentString(String.format("\'%s\' is not a valid meter group name!", name)));
				}
			}
		});
	}

	private void unsubscribe(ICommandSender source) {
		command(source, (meterGroup, player) -> {
			multimeter.unsubscribeFromMeterGroup(meterGroup, player);
			source.sendMessage(new TextComponentString(String.format("Unsubscribed from meter group \'%s\'", meterGroup.getName())));
		});
	}

	private void queryPrivate(ICommandSender source) {
		command(source, (meterGroup, player) -> {
			String status = meterGroup.isPrivate() ? "private" : "public";
			source.sendMessage(new TextComponentString(String.format("Meter group \'%s\' is %s", meterGroup.getName(), status)));
		});
	}

	private void setPrivate(ICommandSender source, boolean isPrivate) {
		command(source, (meterGroup, player) -> {
			if (meterGroup.isOwnedBy(player)) {
				meterGroup.setPrivate(isPrivate);
				source.sendMessage(new TextComponentString(String.format("Meter group \'%s\' is now %s", meterGroup.getName(), (isPrivate ? "private" : "public"))));
			} else {
				source.sendMessage(new TextComponentString("Only the owner of a meter group can change its privacy!"));
			}
		});
	}

	private void membersList(ICommandSender source) {
		Map<String, UUID> members = listMembers(source);

		commandMembers(source, (meterGroup, owner) -> {
			if (members.isEmpty()) {
				source.sendMessage(new TextComponentString(String.format("Meter group \'%s\' has no members yet!", meterGroup.getName())));
			} else {
				String message = String.format("Members of meter group \'%s\':\n  ", meterGroup.getName()) + String.join("\n  ", members.keySet());
				source.sendMessage(new TextComponentString(message));
			}
		});
	}

	private void membersAdd(ICommandSender source, Collection<EntityPlayerMP> players) {
		commandMembers(source, (meterGroup, owner) -> {
			for (EntityPlayerMP player : players) {
				if (player == owner) {
					source.sendMessage(new TextComponentString("You cannot add yourself as a member!"));
				} else if (meterGroup.hasMember(player)) {
					source.sendMessage(new TextComponentString(String.format("Player \'%s\' is already a member of meter group \'%s\'!", player.getName(), meterGroup.getName())));
				} else if (!multimeter.getServer().isMultimeterClient(player)) {
					source.sendMessage(new TextComponentString(String.format("You cannot add player \'%s\' as a member; they do not have %s installed!", player.getName(), RedstoneMultimeter.MOD_NAME)));
				} else {
					multimeter.addMemberToMeterGroup(meterGroup, player.getUniqueID());
					source.sendMessage(new TextComponentString(String.format("Player \'%s\' is now a member of meter group \'%s\'", player.getName(), meterGroup.getName())));
				}
			}
		});
	}

	private void membersRemovePlayer(ICommandSender source, String playerName) {
		commandMembers(source, (meterGroup, owner) -> {
			Entry<String, UUID> member = findMember(listMembers(source), playerName);

			if (member == null) {
				EntityPlayerMP player = multimeter.getServer().getPlayerList().get(playerName);

				if (player == owner) {
					source.sendMessage(new TextComponentString("You cannot remove yourself as a member!"));
				} else {
					source.sendMessage(new TextComponentString(String.format("Meter group \'%s\' has no member with the name \'%s\'!", meterGroup.getName(), playerName)));
				}
			} else {
				multimeter.removeMemberFromMeterGroup(meterGroup, member.getValue());
				source.sendMessage(new TextComponentString(String.format("Player \'%s\' is no longer a member of meter group \'%s\'", member.getKey(), meterGroup.getName())));
			}
		});
	}

	private Entry<String, UUID> findMember(Map<String, UUID> members, String playerName) {
		String key = playerName.toLowerCase();

		for (Entry<String, UUID> member : members.entrySet()) {
			if (member.getKey().toLowerCase().equals(key)) {
				return member;
			}
		}

		return null;
	}

	private void membersClear(ICommandSender source) {
		commandMembers(source, (meterGroup, owner) -> {
			multimeter.clearMembersOfMeterGroup(meterGroup);
			source.sendMessage(new TextComponentString(String.format("Removed all members from meter group \'%s\'", meterGroup.getName())));
		});
	}

	private void commandMembers(ICommandSender source, MeterGroupCommandExecutor command) {
		command(source, (meterGroup, player) -> {
			if (meterGroup.isOwnedBy(player)) {
				command.run(meterGroup, player);

				if (!meterGroup.isPrivate()) {
					source.sendMessage(new TextComponentString("NOTE: this meter group is public; adding/removing members will not have any effect until you make it private!"));
				}
			}
		});
	}

	private void clear(ICommandSender source) {
		command(source, (meterGroup, player) -> {
			multimeter.clearMeterGroup(meterGroup);
			source.sendMessage(new TextComponentString(String.format("Removed all meters in meter group \'%s\'", meterGroup.getName())));
		});
	}

	private void command(ICommandSender source, MeterGroupCommandExecutor command) {
		command(source, player -> {
			ServerMeterGroup meterGroup = multimeter.getSubscription(player);

			if (meterGroup == null) {
				source.sendMessage(new TextComponentString("Please subscribe to a meter group first!"));
			} else {
				command.run(meterGroup, player);
			}
		});
	}

	private void command(ICommandSender source, MultimeterCommandExecutor command) {
		run(source, p -> { command.run(p); return true; });
	}

	private boolean run(ICommandSender source, Function<EntityPlayerMP, Boolean> command) {
		try {
			return command.apply(getCommandSenderAsPlayer(source));
		} catch (CommandException e) {
			return false;
		}
	}

	@FunctionalInterface
	private static interface MultimeterCommandExecutor {
		public void run(EntityPlayerMP player);
    }

	@FunctionalInterface
	private static interface MeterGroupCommandExecutor {
		public void run(ServerMeterGroup meterGroup, EntityPlayerMP player);
    }
}
