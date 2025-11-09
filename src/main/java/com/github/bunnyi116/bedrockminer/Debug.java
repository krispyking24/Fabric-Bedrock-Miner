package com.github.bunnyi116.bedrockminer;

public class Debug {
    public static void alwaysWrite(String var1, Object... var2) {
        BedrockMiner.LOGGER.info(var1, var2);
    }

    public static void alwaysWrite(Object obj) {
        BedrockMiner.LOGGER.info(obj.toString());
    }

    public static void alwaysWrite() {
        alwaysWrite("");
    }


    public static void write(String var1, Object... var2) {
        if (APIs.getInstance().getConfig().debug) {
            BedrockMiner.LOGGER.info(var1, var2);
        }
    }

    public static void write(Object obj) {
        if (APIs.getInstance().getConfig().debug) {
            BedrockMiner.LOGGER.info(obj.toString());
        }
    }

    public static void write() {
        write("");
    }
}
