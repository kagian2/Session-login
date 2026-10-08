package dev.kagian.gui;

import dev.kagian.SessionMod;
import dev.kagian.api.ApiException;
import dev.kagian.api.APIUtils;
import dev.kagian.util.LibraryManager;
import dev.kagian.util.SessionUtils;
import dev.kagian.util.UiTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LoginScreen extends Screen {
    private TextFieldWidget tokenField;
    private ButtonWidget loginButton;
    private ButtonWidget restoreButton;
    private Text statusText;
    private boolean busy = false;

    public LoginScreen() {
        super(Text.literal("Session Login"));
        this.statusText = UiTheme.obfuscatedTitle(UiTheme.info("Paste your access token below"), 3);
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

        tokenField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY, 200, 20, Text.literal("Token"));
        tokenField.setMaxLength(32767);
        tokenField.setPlaceholder(Text.literal("Access token...").formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(tokenField);
        this.setInitialFocus(tokenField);

        loginButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"), button -> attemptLogin())
                .dimensions(centerX - 100, centerY + 25, 97, 20).build());

        restoreButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Restore"), button -> {
            SessionUtils.restoreSession();
            statusText = UiTheme.obfuscatedTitle(UiTheme.warning("Restored original account"), 4);
            restoreButton.active = false;
        }).dimensions(centerX + 3, centerY + 25, 97, 20).build());

        restoreButton.active = SessionMod.currentSession != null && SessionMod.originalSession != null
                && !SessionMod.currentSession.equals(SessionMod.originalSession);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.close())
                .dimensions(centerX - 100, centerY + 50, 200, 20).build());
    }

    private void attemptLogin() {
        if (busy) return;
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            statusText = UiTheme.obfuscatedTitle(UiTheme.error("Don't leave it empty!"), 5);
            return;
        }

        busy = true;
        loginButton.active = false;
        statusText = UiTheme.obfuscatedTitle(UiTheme.info("Logging in..."), 3);

        CompletableFuture.supplyAsync(() -> {
            try {
                return APIUtils.getProfileInfo(token);
            } catch (ApiException e) {
                throw new CompletionException(e);
            }
        }).whenComplete((info, error) -> MinecraftClient.getInstance().execute(() -> {
            busy = false;
            loginButton.active = true;

            if (error != null) {
                Throwable cause = error.getCause();
                String message = cause instanceof ApiException ae ? ae.getMessage() : "Something went wrong logging in";
                statusText = UiTheme.obfuscatedTitle(UiTheme.error(message), 5);
                return;
            }

            try {
                Session newSession = SessionUtils.createSession(info.username(), info.uuid(), token);
                SessionUtils.setSession(newSession);
                SessionMod.currentSession = newSession;
                LibraryManager.addOrUpdate(info.username(), info.uuid(), token);

                statusText = UiTheme.obfuscatedTitle(UiTheme.success("Logged in: " + info.username()), 5);
                restoreButton.active = true;
            } catch (Exception e) {
                statusText = UiTheme.obfuscatedTitle(UiTheme.error("Fetched the profile, but couldn't apply it"), 4);
            }
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        context.fill(centerX - 112, centerY - 38, centerX + 112, centerY + 78, UiTheme.PANEL_BG);
        context.drawStrokedRectangle(centerX - 112, centerY - 38, 224, 116, UiTheme.PANEL_BORDER);

        super.render(context, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.statusText);
        context.drawText(this.textRenderer, this.statusText, (this.width - titleWidth) / 2, centerY - 58, 0xFFFFFF, true);
    }
}
