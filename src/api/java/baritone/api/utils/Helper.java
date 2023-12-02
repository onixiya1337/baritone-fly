package baritone.api.utils;

import baritone.api.BaritoneAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.Arrays;
import java.util.stream.Stream;

public interface Helper {

    Helper HELPER = new Helper() {};

    static IChatComponent getPrefix() {
        IChatComponent baritone = new ChatComponentText(BaritoneAPI.getSettings().shortBaritonePrefix.value ? "B" : "Baritone");
        baritone.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);

        IChatComponent prefix = new ChatComponentText("");
        prefix.getChatStyle().setColor(EnumChatFormatting.DARK_PURPLE);
        prefix.appendText("[");
        prefix.appendSibling(baritone);
        prefix.appendText("]");

        return prefix;
    }

    default void logNotification(String message) {
        logNotification(message, false);
    }

    default void logNotification(String message, boolean error) {
        if (BaritoneAPI.getSettings().desktopNotifications.value) {
            logNotificationDirect(message, error);
        }
    }

    default void logNotificationDirect(String message) {
        logNotificationDirect(message, false);
    }

    default void logNotificationDirect(String message, boolean error) {
        Minecraft.getMinecraft().addScheduledTask(() -> BaritoneAPI.getSettings().notifier.value.accept(message, error));
    }

    default void logDebug(String message) {
        if (!BaritoneAPI.getSettings().chatDebug.value) {
            return;
        }
        logDirect(message);
    }

    default void logDirect(IChatComponent... components) {
        IChatComponent component = new ChatComponentText("");
        component.appendSibling(getPrefix());
        component.appendSibling(new ChatComponentText(" "));

        Arrays.asList(components).forEach(component::appendSibling);
        Minecraft.getMinecraft().addScheduledTask(() -> BaritoneAPI.getSettings().logger.value.accept(component));
    }

    default void logDirect(String message, EnumChatFormatting color) {
        Stream.of(message.split("\n")).forEach(line -> {
            IChatComponent component = new ChatComponentText(line.replace("\t", "    "));
            component.getChatStyle().setColor(color);
            logDirect(component);
        });
    }

    default void logDirect(String message) {
//        logDirect(message, EnumChatFormatting.GRAY);
    }
}