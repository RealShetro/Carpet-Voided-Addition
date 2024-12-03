package carpet.helpers;

import carpet.CarpetSettings;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.profiler.Snooper;

public class DisabledSnooper extends Snooper {

    @Override
    public void startSnooper() {
    }

    @Override
    public void addMemoryStatsToSnooper() {
    }

    @Override
    public void addClientStat(String statName, Object statValue) {
    }

    @Override
    public void addStatToSnooper(String statName, Object statValue) {
    }

    @Override
    public boolean isSnooperRunning() {
        return true;
    }

    @Override
    public void stopSnooper() {
    }

    @Override
    public long getMinecraftStartTimeMillis() {
        return CarpetSettings.ONE_ONE_FOUR_FIVE_ONE_FOUR;
    }
}
