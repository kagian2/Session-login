package dev.kagian.gui;

import dev.kagian.api.APIUtils;
import dev.kagian.util.UiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class EditAccountScreen extends Screen {
    private TextFieldWidget nameField;
    private TextFieldWidget skinUrlField;
    private ButtonWidget nameButton;
    private ButtonWidget skinButton;
    private Text statusText;
    private boolean busy = false;

    public EditAccountScreen() {
        super(Text.literal("Edit Account"));
        this.statusText = UiTheme.obfuscatedTitle(UiTheme.info("Change the signed-in account's name or skin"), 3);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 38, 200, 20, Text.literal("Name"));
        nameField.setPlaceholder(Text.literal("New username").formatted(Formatting.DARK_GRAY));

        skinUrlField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY + 2, 200, 20, Text.literal("Skin URL"));
        skinUrlField.setMaxLength(255);
        skinUrlField.setPlaceholder(Text.literal("https://...png").formatted(Formatting.DARK_GRAY));

        this.addDrawableChild(nameField);
        this.addDrawableChild(skinUrlField);

        String token = this.client != null ? this.client.getSession().getAccessToken() : null;

        nameButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Change Name"), b -> changeName(token))
                .dimensions(centerX - 100, centerY + 27, 97, 20).build());

        skinButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Change Skin"), b -> changeSkin(token))
                .dimensions(centerX + 3, centerY + 27, 97, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.close())
                .dimensions(centerX - 100, centerY + 52, 200, 20).build());
    }

    private void changeName(String token) {
        String newName = nameField.getText().trim();
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
        String url = skinUrlField.getText().trim();
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

    // Runs a blocking HTTP call off the render thread, disabling the buttons while it's in flight
    private void runBusy(IntSupplier call, IntConsumer onResult) {
        busy = true;
        nameButton.active = false;
        skinButton.active = false;
        statusText = UiTheme.obfuscatedTitle(UiTheme.info("Working..."), 3);

        CompletableFuture.supplyAsync(call::getAsInt).whenComplete((code, error) -> MinecraftClient.getInstance().execute(() -> {
            busy = false;
            nameButton.active = true;
            skinButton.active = true;
            onResult.accept(error != null ? -1 : code);
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.fill(centerX - 112, centerY - 76, centerX + 112, centerY + 80, UiTheme.PANEL_BG);
        context.drawBorder(centerX - 112, centerY - 76, 224, 156, UiTheme.PANEL_BORDER);

        super.render(context, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.statusText);
        context.drawText(this.textRenderer, this.statusText, (this.width - titleWidth) / 2, centerY - 95, 0xFFFFFF, true);
    }
}
