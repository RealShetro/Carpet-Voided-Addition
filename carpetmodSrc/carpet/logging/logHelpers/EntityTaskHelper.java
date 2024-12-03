package carpet.logging.logHelpers;

import carpet.CarpetServer;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityTaskHelper {
    private static Logger logger() {
        return LoggerRegistry.getLogger("entityTask");
    }

    private static long tickCounter() {
        return CarpetServer.minecraft_server.getWorld(0).getTotalWorldTime();
    }

    private static void log(Entity entity, Consumer<List<ITextComponent>> message) {
        if (!LoggerRegistry.__entityTask) return;
        Logger logger = logger();
        logger.log((option, player) -> {
            if (!entity.getCachedUniqueIdString().equals(option)) return new ITextComponent[0];
            List<ITextComponent> parts = new ArrayList<>();
            message.accept(parts);
            return parts.toArray(new ITextComponent[0]);
        });
    }

    public static void onTaskStart(Entity entity, EntityAIBase task) {
        log(entity, list -> list.add(Messenger.s(null, "Task started on tick " + tickCounter() + ": " + task.getTask())));
    }

    public static void onTaskInterrupted(Entity entity, EntityAIBase task, int collidedMutex) {
        log(entity, list -> list.add(Messenger.s(null, "Task interrupted on tick " + tickCounter() + " due to mutex collision"
            + collidedMutex + ": " + task.getTask())));
    }

    public static void onTaskTerminated(Entity entity, EntityAIBase task) {
        log(entity, list -> list.add(Messenger.s(null, "Task naturally stopped on tick " + tickCounter() + ": " + task.getTask())));
    }
}
