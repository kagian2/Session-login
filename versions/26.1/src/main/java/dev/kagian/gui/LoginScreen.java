package dev.kagian.gui;

import dev.kagian.SessionMod;
import dev.kagian.api.ApiException;
import dev.kagian.api.APIUtils;
import dev.kagian.util.LibraryManager;
import dev.kagian.util.SessionUtils;
import dev.kagian.util.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LoginScreen extends Screen {
    private EditBox tokenField;
    private Button loginButton;
    private Button restoreButton;
    private Component statusText;
    private boolean busy = false;

    public LoginScreen() {
        super(Component.literal("Session Login"));
        this.statusText = UiTheme.obfuscatedTitle(UiTheme.info("Paste your access token below"), 3);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        tokenField = new EditBox(this.font, centerX - 100, centerY, 200, 20, Component.literal("Token"));
        tokenField.setMaxLength(32767);
        tokenField.setHint(Component.literal("Access token...").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(tokenField);
        this.setInitialFocus(tokenField);

        loginButton = this.addRenderableWidget(Button.builder(Component.literal("Login"), button -> attemptLogin())
                .bounds(centerX - 100, centerY + 25, 97, 20).build());

        restoreButton = this.addRenderableWidget(Button.builder(Component.literal("Restore"), button -> {
            SessionUtils.restoreSession();
            statusText = UiTheme.obfuscatedTitle(UiTheme.warning("Restored original account"), 4);
            restoreButton.active = false;
        }).bounds(centerX + 3, centerY + 25, 97, 20).build());

        restoreButton.active = SessionMod.currentSession != null && SessionMod.originalSession != null
                && !SessionMod.currentSession.equals(SessionMod.originalSession);

        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.onClose())
                .bounds(centerX - 100, centerY + 50, 200, 20).build());
    }

    private void attemptLogin() {
        if (busy) return;
        String token = tokenField.getValue().trim();
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
        }).whenComplete((info, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            loginButton.active = true;

            if (error != null) {
                Throwable cause = error.getCause();
                String message = cause instanceof ApiException ae ? ae.getMessage() : "Something went wrong logging in";
                statusText = UiTheme.obfuscatedTitle(UiTheme.error(message), 5);
                return;
            }

            try {
                User newSession = SessionUtils.createSession(info.username(), info.uuid(), token);
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.fill(centerX - 112, centerY - 38, centerX + 112, centerY + 78, UiTheme.PANEL_BG);
        graphics.outline(centerX - 112, centerY - 38, 224, 116, UiTheme.PANEL_BORDER);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.statusText);
        graphics.text(this.font, this.statusText, (this.width - titleWidth) / 2, centerY - 58, 0xFFFFFFFF, true);
    }
}
