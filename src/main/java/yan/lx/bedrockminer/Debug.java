package yan.lx.bedrockminer;

import net.minecraft.text.Text;
import org.slf4j.Logger;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.MessageUtils;

public class Debug {
    public static void info(String msg) {
        BedrockMinerMod.LOGGER.info(msg);
    }

    public static void info() {
        info("");
    }

    public static void info(String msgFormat, Object... args) {
        info(String.format(msgFormat, args));
    }

    public static void info(Object obj) {
        info(obj.toString());
    }
}
