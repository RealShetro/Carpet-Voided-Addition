package bjoels;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.File;

public class CustomWhitelistMessage
{
    //static constructor thing
    public static CustomWhitelistMessage g_Instance = new CustomWhitelistMessage();

    public String m_WhitelistMessage = "You are not white-listed on this server!";

    private CustomWhitelistMessage()
    {
        reloadConfig();
    }

    public void reloadConfig()
    {
        try
        {
            m_WhitelistMessage = readOrCreateFile("whitelist_message.txt", StandardCharsets.UTF_8);
        }
        catch(Exception exc)
        {
            System.out.println("Could not reload whitelist message config");
            exc.printStackTrace();
        }
    }

    private String readOrCreateFile(String path, Charset encoding) throws IOException
    {
        File file = new File(path);
        if(!file.exists())
        {
            file.createNewFile();
        }

        byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileBytes, encoding);
    }
}