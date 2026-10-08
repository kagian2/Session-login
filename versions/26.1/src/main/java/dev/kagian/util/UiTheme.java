package dev.kagian.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class UiTheme {
    private UiTheme() {
    }

    public static final int PANEL_BG = 0xC0101018;
    public static final int PANEL_BORDER = 0x40FFFFFF;
    public static final int CARD_BG = 0x70000000;
    public static final int CARD_BG_HOVER = 0x90FFFFFF;
    public static final int SUCCESS = 0x55FF55;
    public static final int ERROR = 0xFF5555;
    public static final int WARNING = 0xFFFF55;

    public static Component obfuscatedTitle(Component baseText, int padding) {
        Style baseStyle = baseText.getStyle().withObfuscated(false);
        Style obfStyle = baseStyle.withObfuscated(true);
        String pad = "@".repeat(Math.max(padding, 0));

        return Component.empty()
                .append(Component.literal(pad + " ").setStyle(obfStyle))
                .append(baseText.copy().setStyle(baseStyle))
                .append(Component.literal(" " + pad).setStyle(obfStyle));
    }

    public static Component success(String msg) {
        return Component.literal(msg).withStyle(ChatFormatting.GREEN);
    }

    public static Component error(String msg) {
        return Component.literal(msg).withStyle(ChatFormatting.RED);
    }

    public static Component warning(String msg) {
        return Component.literal(msg).withStyle(ChatFormatting.YELLOW);
    }

    public static Component info(String msg) {
        return Component.literal(msg).withStyle(ChatFormatting.AQUA);
    }
}
