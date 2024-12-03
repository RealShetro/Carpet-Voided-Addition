package carpet.commands;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDragonEgg;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public class CommandRandomCheck {
    private static class Holder {
        static {
            updateTickMethodName = reflectUpdateTickMethodName();
        }
        public static final String updateTickMethodName;
        
        private static String reflectUpdateTickMethodName() {
            Class<? extends Block> dragonEggClass = BlockDragonEgg.class;
            Class[] updateTickParamTypes = new Class[] {World.class, BlockPos.class, IBlockState.class, Random.class};
            String name = null;
            for (Method method: dragonEggClass.getDeclaredMethods()) {
                if (Arrays.equals(method.getParameterTypes(), updateTickParamTypes)) {
                    return method.getName();
                }
            }
            throw new RuntimeException("updateTick(World, BlockPos, IBlockState, Random) method not found");
        }

        public static boolean hasRandomTick(Block block) {
            if (!block.getTickRandomly()) return false;
            Class<?> clazz = block.getClass();
            while (clazz != Block.class) {
                for (Method m: clazz.getDeclaredMethods()) {
                    if (updateTickMethodName.equals(m.getName())) return true;
                }
                clazz = clazz.getSuperclass();
            }
            return false;
        }
    }
}
