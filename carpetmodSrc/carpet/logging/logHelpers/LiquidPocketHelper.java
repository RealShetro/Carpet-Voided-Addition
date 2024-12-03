package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

public class LiquidPocketHelper {
    private static final LiquidPocketHelper INSTANCE = new LiquidPocketHelper();

    private LiquidPocketHelper() {
    }

    public static LiquidPocketHelper getInstance() {
        return INSTANCE;
    }

    public void onLiquidPocketPlacement(BlockPos pos) {
        LoggerRegistry.getLogger("liquidPocket").logNoCommand(() -> new ITextComponent[]
                {Messenger.m(null, TextFormatting.WHITE + "- Attempt to place a liquid pocket at position " + pos)});
    }
}
