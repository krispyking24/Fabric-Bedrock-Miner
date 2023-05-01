package yan.lx.bedrockminer.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class MessageUtils {
    public static void setOverlayMessageKey(String translatableKey) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.translatable(translatableKey), false);
    }

    public static void setOverlayMessage(String message) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal(message), false);
    }

    public static void addMessageKey(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable(message));
    }

    public static void addMessage(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(message));
    }
}
