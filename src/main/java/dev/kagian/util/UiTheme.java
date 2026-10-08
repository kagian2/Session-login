package dev.kagian.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Shared colors and small text helpers used across every Session-Login
 * screen, so the UI stays visually consistent instead of each screen
 * carrying its own copy of the same constants and obfuscated-title code.
 */
public final class UiTheme {
    private UiTheme() {
    }

    // Palette
    public static final int PANEL_BG = 0xC0101018;
    public static final int PANEL_BORDER = 0x40FFFFFF;
    public static final int CARD_BG = 0x70000000;
    public static final int CARD_BG_HOVER = 0x90FFFFFF;
    public static final int SUCCESS = 0x55FF55;
    public static final int ERROR = 0xFF5555;
    public static final int WARNING = 0xFFFF55;

    // The "@@@ Title @@@" obfuscated-padding effect used on every screen title/status line.
    public static Text obfuscatedTitle(Text baseText, int padding) {
        Style baseStyle = baseText.getStyle().withObfuscated(false);
        Style obfStyle = baseStyle.withObfuscated(true);
        String pad = "@".repeat(Math.max(padding, 0));

        return Text.empty()
                .append(Text.literal(pad + " ").setStyle(obfStyle))
                .append(baseText.copy().setStyle(baseStyle))
                .append(Text.literal(" " + pad).setStyle(obfStyle));
    }

    public static Text success(String msg) {
        return Text.literal(msg).formatted(Formatting.GREEN);
    }

    public static Text error(String msg) {
        return Text.literal(msg).formatted(Formatting.RED);
    }

    public static Text warning(String msg) {
        return Text.literal(msg).formatted(Formatting.YELLOW);
    }

    public static Text info(String msg) {
        return Text.literal(msg).formatted(Formatting.AQUA);
    }
}
