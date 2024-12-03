package carpet.commands;

import carpet.utils.Messenger;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

public class CommandIngameTest extends CommandCarpetBase {
    @Override
    public String getName() {
        return "/ingameTest";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "yeet";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int i = Integer.parseInt(args[0]);
        switch (i) {
            case 0:
                EntityVillager.ListEnchantedItemForEmeralds trade = new EntityVillager.ListEnchantedItemForEmeralds(Items.DIAMOND_PICKAXE, new EntityVillager.PriceInfo(12, 15));
                int successes = 0;
                for (int j = 0; j < 10000000; j ++) {
                    MerchantRecipeList list = new MerchantRecipeList();
                    trade.addMerchantRecipe(null, list, new Random());
                    NBTTagList tagList = list.get(0).getItemToSell().getEnchantmentTagList();
                    int tagCount = tagList.tagCount();
                    if (tagCount < 3) continue;
                    int progress = 0;
                    for (int k = 0; k < tagCount; k ++) {
                        NBTTagCompound compound = tagList.getCompoundTagAt(k);
                        if (compound.getInteger("id") == 32 && compound.getInteger("lvl") == 3) {
                            progress ++;
                        }
                        if (compound.getInteger("id") == 33 && compound.getInteger("lvl") == 1) {
                            progress ++;
                        }
                        if (compound.getInteger("id") == 34 && compound.getInteger("lvl") == 3) {
                            progress ++;
                        }
                    }
                    if (progress == 3) successes ++;
                }
                Messenger.s(sender, "There are a total number of " + successes + " 331 trades in 10000000 attempts");
            default:
                break;
        }
    }
}
