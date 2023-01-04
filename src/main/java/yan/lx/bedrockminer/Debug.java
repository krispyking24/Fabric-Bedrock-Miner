package yan.lx.bedrockminer;

import org.slf4j.Logger;
import yan.lx.bedrockminer.utils.Messager;

public class Debug {
    private static final Logger LOGGER = BedrockMinerMod.LOGGER;

    public static OutputType outputType = OutputType.LOGGER_INFO;

    public static boolean enable = true;

    public static void info(String msg) {
        if (enable) {
            switch (outputType) {
                case CHAT_MESSAGE -> Messager.rawChat(msg);
                case OVERLAY_MESSAGE -> Messager.rawactionBar(msg);
                case LOGGER_INFO -> LOGGER.info(msg);
            }
        }
    }

    public static void info() {
        info("");
    }

    public static void info(String msgFormat, Object... args) {
        info(String.format(msgFormat, args));
    }

    public enum OutputType {
        CHAT_MESSAGE,

        OVERLAY_MESSAGE,

        LOGGER_INFO,
    }
}
