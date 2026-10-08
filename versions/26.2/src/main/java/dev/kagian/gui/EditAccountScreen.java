package dev.kagian.gui;

import dev.kagian.api.APIUtils;
import dev.kagian.util.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class EditAccountScreen extends Screen {
    private EditBox nameField;
    private EditBox skinUrlField;
    private Button nameButton;
    private Button skinButton;
    private Component statusText;
    private boolean busy = false;

    public EditAccountScreen() {
        super(Component.literal("Edit Account"));
        this.statusText = UiTheme.obfuscatedTitle(UiTheme.info("Change the signed-in account's name or skin"), 3);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new JoinMultiplayerScreen(new TitleScreen()));
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new EditBox(this.font, centerX - 100, centerY - 38, 200, 20, Component.literal("Name"));
        nameField.setHint(Component.literal("New username").withStyle(ChatFormatting.DARK_GRAY));

        skinUrlField = new EditBox(this.font, centerX - 100, centerY + 2, 200, 20, Component.literal("Skin URL"));
        skinUrlField.setMaxLength(255);
        skinUrlField.setHint(Component.literal("https://...png").withStyle(ChatFormatting.DARK_GRAY));

        this.addRenderableWidget(nameField);
        this.addRenderableWidget(skinUrlField);

        String token = this.minecraft != null ? this.minecraft.getUser().getAccessToken() : null;

        nameButton = this.addRenderableWidget(Button.builder(Component.literal("Change Name"), b -> changeName(token))
                .bounds(centerX - 100, centerY + 27, 97, 20).build());

        skinButton = this.addRenderableWidget(Button.builder(Component.literal("Change Skin"), b -> changeSkin(token))
                .bounds(centerX + 3, centerY + 27, 97, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.onClose())
                .bounds(centerX - 100, centerY + 52, 200, 20).build());
    }

    private void changeName(String token) {
        String newName = nameField.getValue().trim();
        if (newName.isEmpty()) {
            statusText = UiTheme.obfuscatedTitle(UiTheme.error("Enter a name first"), 4);
            return;
        }
        if (token == null || busy) return;

        runBusy(() -> APIUtils.changeName(newName, token), code -> statusText =
                (code == 200 || code == 204)
                        ? UiTheme.obfuscatedTitle(UiTheme.success("Name changed!"), 4)
                        : UiTheme.obfuscatedTitle(UiTheme.error("Failed (HTTP " + code + ")"), 5));
    }

    private void changeSkin(String token) {
        String url = skinUrlField.getValue().trim();
        if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
            statusText = UiTheme.obfuscatedTitle(UiTheme.error("Enter a valid skin URL"), 4);
            return;
        }
        if (token == null || busy) return;

        runBusy(() -> APIUtils.changeSkin(url, token), code -> statusText =
                (code == 200 || code == 204)
                        ? UiTheme.obfuscatedTitle(UiTheme.success("Skin updated!"), 4)
                        : UiTheme.obfuscatedTitle(UiTheme.error("Failed (HTTP " + code + ")"), 5));
    }

    private void runBusy(IntSupplier call, IntConsumer onResult) {
        busy = true;
        nameButton.active = false;
        skinButton.active = false;
        statusText = UiTheme.obfuscatedTitle(UiTheme.info("Working..."), 3);

        CompletableFuture.supplyAsync(call::getAsInt).whenComplete((code, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            nameButton.active = true;
            skinButton.active = true;
            onResult.accept(error != null ? -1 : code);
        }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.fill(centerX - 112, centerY - 76, centerX + 112, centerY + 80, UiTheme.PANEL_BG);
        graphics.outline(centerX - 112, centerY - 76, 224, 156, UiTheme.PANEL_BORDER);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.statusText);
        graphics.text(this.font, this.statusText, (this.width - titleWidth) / 2, centerY - 95, 0xFFFFFFFF, true);
    }
}
