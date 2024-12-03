package bjoels;

import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;

public class ChatTweaks
{
    private static String getBlockPosStr(BlockPos pos)
    {
        return pos.getX() + " "
             + pos.getY() + " "
             + pos.getZ();
    }

    private static String replaceCharacter(String str, int replaceBegin, int replaceEnd, String toReplaceWith)
    {
        return str.substring(0, replaceBegin)+toReplaceWith+str.substring(replaceEnd);
    }

    public static String applyTweaksToStr(String str, Entity player)
    {
        String toReturn = str;
        for(int i = toReturn.indexOf('!'); i != -1; i = toReturn.indexOf('!', i+1))
        {
            if(toReturn.indexOf("!cords", i) == i)
                toReturn = replaceCharacter(toReturn, i, i+6, getBlockPosStr(player.getPosition()));
            else if(toReturn.indexOf("!ncords", i) == i)
            {
                BlockPos playerPos = player.getPosition();
                if(player.dimension != -1) //Not Nether
                {
                    BlockPos correctedBlockPos = new BlockPos(playerPos.getX()/8, playerPos.getY(), playerPos.getZ()/8);
                    toReturn = replaceCharacter(toReturn, i, i+7, getBlockPosStr(correctedBlockPos));
                } //Nether
                else toReturn = replaceCharacter(toReturn, i, i+7, getBlockPosStr(playerPos));
            }
            else if(toReturn.indexOf("!ocords", i) == i)
            {
                BlockPos playerPos = player.getPosition();
                if(player.dimension == -1) //Nether
                {
                    BlockPos correctedBlockPos = new BlockPos(playerPos.getX()*8, playerPos.getY(), playerPos.getZ()*8);
                    toReturn = replaceCharacter(toReturn, i, i+7, getBlockPosStr(correctedBlockPos));
                } //Not Nether
                else toReturn = replaceCharacter(toReturn, i, i+7, getBlockPosStr(playerPos));
            }
            else if(toReturn.indexOf("!seed", i) == i) {
            	toReturn = replaceCharacter(toReturn, i, i+5, Long.toString(player.world.getSeed()));
                player.sendMessage(new TextComponentTranslation("commands.seed.success", new Object[] {player.world.getSeed()}));
            }
        }
        return toReturn;
    }
}