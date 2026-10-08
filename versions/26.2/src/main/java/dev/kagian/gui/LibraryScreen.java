package dev.kagian.gui;

import dev.kagian.api.ApiException;
import dev.kagian.api.APIUtils;
import dev.kagian.util.LibraryManager;
import dev.kagian.util.SessionUtils;
import dev.kagian.util.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
// PORT-TODO: ResourceLocation was renamed to Identifier in 1.21.11 and that carries into 26.x -
// confirmed via an external source, not the compiler yet for 26.2 specifically.
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LibraryScreen extends Screen {
    private AccountListWidget listWidget;
    private Button loginButton;
    private Button removeButton;
    private Component statusText;
    private boolean busy = false;
    private static final Set<Identifier> LOADED_AVATARS = new HashSet<>();

    public LibraryScreen() {
        super(Component.literal("Account Library"));
        this.statusText = UiTheme.obfuscatedTitle(UiTheme.info("Select an account"), 3);
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
        int buttonY = this.height - 40;
        int listHeight = this.height - 100;

        this.listWidget = new AccountListWidget(this.minecraft, this.width, listHeight, 40, 60);
        this.addRenderableWidget(this.listWidget);

        for (LibraryManager.SavedAccount acc : LibraryManager.ACCOUNTS) {
            this.listWidget.addAccountEntry(new AccountEntry(acc));
        }

        this.loginButton = this.addRenderableWidget(Button.builder(Component.literal("Login"), button -> attemptLogin())
                .bounds(centerX - 160, buttonY, 100, 20).build());
        this.loginButton.active = false;

        this.removeButton = this.addRenderableWidget(Button.builder(Component.literal("Remove"), button -> removeSelected())
                .bounds(centerX - 55, buttonY, 100, 20).build());
        this.removeButton.active = false;

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
                .bounds(centerX + 50, buttonY, 100, 20).build());
    }

    private void attemptLogin() {
        AccountEntry selected = this.listWidget.getSelected();
        if (selected == null || busy) return;

        busy = true;
        loginButton.active = false;
        statusText = UiTheme.obfuscatedTitle(UiTheme.info("Logging in..."), 3);

        LibraryManager.SavedAccount account = selected.account;
        CompletableFuture.supplyAsync(() -> {
            try {
                return APIUtils.getProfileInfo(account.token);
            } catch (ApiException e) {
                throw new CompletionException(e);
            }
        }).whenComplete((info, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;

            if (error != null) {
                LibraryManager.remove(account);
                this.listWidget.removeAccountEntry(selected);
                statusText = UiTheme.obfuscatedTitle(UiTheme.error("Token expired - removed"), 5);
                updateSelection();
                return;
            }

            User newSession = SessionUtils.createSession(info.username(), info.uuid(), account.token);
            SessionUtils.setSession(newSession);
            statusText = UiTheme.obfuscatedTitle(UiTheme.success("Logged in: " + info.username()), 4);
            loginButton.active = true;
        }));
    }

    private void removeSelected() {
        AccountEntry selected = this.listWidget.getSelected();
        if (selected == null) return;
        LibraryManager.remove(selected.account);
        this.listWidget.removeAccountEntry(selected);
        statusText = UiTheme.obfuscatedTitle(UiTheme.warning("Removed " + selected.account.username), 4);
        updateSelection();
    }

    public void updateSelection() {
        boolean hasSelection = this.listWidget.getSelected() != null;
        this.loginButton.active = hasSelection && !busy;
        this.removeButton.active = hasSelection;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int titleWidth = this.font.width(this.statusText);
        graphics.text(this.font, this.statusText, (this.width - titleWidth) / 2, 15, 0xFFFFFFFF, true);

        if (LibraryManager.ACCOUNTS.isEmpty()) {
            Component empty = Component.literal("No saved accounts yet - log in with Token Login to add one").withStyle(ChatFormatting.GRAY);
            int w = this.font.width(empty);
            graphics.text(this.font, empty, (this.width - w) / 2, this.height / 2, 0xFFAAAAAA, false);
        }
    }

    class AccountListWidget extends ObjectSelectionList<AccountEntry> {

        public AccountListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public void addAccountEntry(AccountEntry entry) {
            this.addEntry(entry);
        }

        public void removeAccountEntry(AccountEntry entry) {
            this.removeEntry(entry);
        }

        @Override
        public void setSelected(AccountEntry entry) {
            super.setSelected(entry);
            LibraryScreen.this.updateSelection();
        }
    }

    class AccountEntry extends ObjectSelectionList.Entry<AccountEntry> {
        public final LibraryManager.SavedAccount account;
        private final Identifier apiTextureId;

        public AccountEntry(LibraryManager.SavedAccount account) {
            this.account = account;

            String safeName = account.username.toLowerCase().replaceAll("[^a-z0-9_.-]", "");
            this.apiTextureId = Identifier.fromNamespaceAndPath("sessionmod", "avatar_" + safeName);

            if (!LOADED_AVATARS.contains(this.apiTextureId)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        NativeImage image = APIUtils.getFaceByUsername(account.username);
                        Minecraft.getInstance().execute(() -> {
                            Minecraft.getInstance().getTextureManager().register(this.apiTextureId,
                                    new DynamicTexture(() -> "session-login/" + account.username, image));
                            LOADED_AVATARS.add(this.apiTextureId);
                        });
                    } catch (Exception e) {
                        // No avatar available for this username - the placeholder box below covers it.
                    }
                });
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getX();
            int y = this.getY();
            int entryWidth = this.getWidth();
            int entryHeight = this.getHeight();

            int cardColor = hovered ? UiTheme.CARD_BG_HOVER : UiTheme.CARD_BG;
            graphics.fill(x - 2, y - 2, x + entryWidth + 2, y + entryHeight - 2, cardColor);

            if (LOADED_AVATARS.contains(this.apiTextureId)) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, this.apiTextureId, x + 5, y + 1, 0.0F, 0.0F, 48, 48, 48, 48);
            } else {
                graphics.fill(x + 5, y + 6, x + 53, y + 54, 0xFF555555);
            }

            int textX = x + 60;
            int textY = y + (entryHeight / 2) - 4;

            graphics.text(LibraryScreen.this.font, Component.literal(account.username), textX, textY, 0xFFFFFFFF, true);

            String statusLabel = account.isValid ? "Active" : "Checking...";
            int statusColor = account.isValid ? UiTheme.SUCCESS : UiTheme.WARNING;

            int textWidth = LibraryScreen.this.font.width(statusLabel);
            graphics.text(LibraryScreen.this.font, statusLabel, x + entryWidth - textWidth - 5, textY, statusColor, true);
        }

        // PORT-TODO: getNarration() no longer overrides anything in 26.x - the NarrationSupplier
        // interface method was likely renamed, but I don't have a confirmed name yet. Left as a
        // plain (currently unused) method rather than guess again; screen-reader narration for
        // library entries just won't fire until this is wired back up correctly.
        public Component getNarration() {
            return Component.literal(account.username);
        }
    }
}
