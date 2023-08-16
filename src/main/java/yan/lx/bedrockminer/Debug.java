package yan.lx.bedrockminer;

public class Debug {
    public static void info(String msg) {
        BedrockMinerMod.LOGGER.info(msg);
    }

    public static void info() {
        info("");
    }

    public static void info(String msgFormat, Object... args) {
        try {
            info(String.format(msgFormat, args));
        } catch (Exception ignored) {
            Debug.info("LOGGER错误");
        }
    }

    public static void info(Object obj) {
        info(obj.toString());
    }
}
