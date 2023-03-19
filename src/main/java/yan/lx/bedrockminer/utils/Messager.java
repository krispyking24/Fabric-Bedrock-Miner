package yan.lx.bedrockminer.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class Messager {
    public static void actionBar(String message) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.translatable(message), false);
    }

    public static void rawactionBar(String message) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal(message), false);
    }

    public static void chat(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable(message));
    }

    public static void chat(Text message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }

    public static void rawChat(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(message));
    }
}
