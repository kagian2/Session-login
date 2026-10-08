package dev.kagian.gui;

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
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.session.Session;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LibraryScreen extends Screen {
    private AccountListWidget listWidget;
    private ButtonWidget loginButton;
    private ButtonWidget removeButton;
    private Text statusText;
    private boolean busy = false;
    private static final Set<Identifier> LOADED_AVATARS = new HashSet<>();

    public LibraryScreen() {
        super(Text.literal("Account Library"));
        this.statusText = UiTheme.obfuscatedTitle(UiTheme.info("Select an account"), 3);
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
        int buttonY = this.height - 40;
        int listHeight = this.height - 100;

        this.listWidget = new AccountListWidget(this.client, this.width, listHeight, 40, 60);
        this.addDrawableChild(this.listWidget);

        for (LibraryManager.SavedAccount acc : LibraryManager.ACCOUNTS) {
            this.listWidget.addAccountEntry(new AccountEntry(acc));
        }

        this.loginButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Login"), button -> attemptLogin())
                .dimensions(centerX - 160, buttonY, 100, 20).build());
        this.loginButton.active = false;

        this.removeButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), button -> removeSelected())
                .dimensions(centerX - 55, buttonY, 100, 20).build());
        this.removeButton.active = false;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.close())
                .dimensions(centerX + 50, buttonY, 100, 20).build());
    }

    private void attemptLogin() {
        AccountEntry selected = this.listWidget.getSelectedOrNull();
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
        }).whenComplete((info, error) -> MinecraftClient.getInstance().execute(() -> {
            busy = false;

            if (error != null) {
                LibraryManager.remove(account);
                this.listWidget.removeAccountEntry(selected);
                statusText = UiTheme.obfuscatedTitle(UiTheme.error("Token expired - removed"), 5);
                updateSelection();
                return;
            }

            Session newSession = SessionUtils.createSession(info.username(), info.uuid(), account.token);
            SessionUtils.setSession(newSession);
            statusText = UiTheme.obfuscatedTitle(UiTheme.success("Logged in: " + info.username()), 4);
            loginButton.active = true;
        }));
    }

    private void removeSelected() {
        AccountEntry selected = this.listWidget.getSelectedOrNull();
        if (selected == null) return;
        LibraryManager.remove(selected.account);
        this.listWidget.removeAccountEntry(selected);
        statusText = UiTheme.obfuscatedTitle(UiTheme.warning("Removed " + selected.account.username), 4);
        updateSelection();
    }

    public void updateSelection() {
        boolean hasSelection = this.listWidget.getSelectedOrNull() != null;
        this.loginButton.active = hasSelection && !busy;
        this.removeButton.active = hasSelection;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int titleWidth = this.textRenderer.getWidth(this.statusText);
        context.drawText(this.textRenderer, this.statusText, (this.width - titleWidth) / 2, 15, 0xFFFFFF, true);

        if (LibraryManager.ACCOUNTS.isEmpty()) {
            Text empty = Text.literal("No saved accounts yet - log in with Token Login to add one").formatted(Formatting.GRAY);
            int w = this.textRenderer.getWidth(empty);
            context.drawText(this.textRenderer, empty, (this.width - w) / 2, this.height / 2, 0xAAAAAA, false);
        }
    }

    class AccountListWidget extends AlwaysSelectedEntryListWidget<AccountEntry> {

        public AccountListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
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

    class AccountEntry extends AlwaysSelectedEntryListWidget.Entry<AccountEntry> {
        public final LibraryManager.SavedAccount account;
        private final Identifier apiTextureId;

        public AccountEntry(LibraryManager.SavedAccount account) {
            this.account = account;

            String safeName = account.username.toLowerCase().replaceAll("[^a-z0-9_.-]", "");
            this.apiTextureId = Identifier.of("sessionmod", "avatar_" + safeName);

            if (!LOADED_AVATARS.contains(this.apiTextureId)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        NativeImage image = APIUtils.getFaceByUsername(account.username);
                        MinecraftClient.getInstance().execute(() -> {
                            // NativeImageBackedTexture now takes a debug-label Supplier<String> as its first argument.
                            MinecraftClient.getInstance().getTextureManager().registerTexture(this.apiTextureId,
                                    new NativeImageBackedTexture(() -> "session-login/" + account.username, image));
                            LOADED_AVATARS.add(this.apiTextureId);
                        });
                    } catch (Exception e) {
                        // No avatar available for this username - the placeholder box below covers it.
                    }
                });
            }
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int cardColor = hovered ? UiTheme.CARD_BG_HOVER : UiTheme.CARD_BG;
            context.fill(x - 2, y - 2, x + entryWidth + 2, y + entryHeight - 2, cardColor);

            if (LOADED_AVATARS.contains(this.apiTextureId)) {
                context.drawTexture(RenderLayer::getGuiTextured, this.apiTextureId, x + 5, y + 1, 0.0F, 0.0F, 48, 48, 48, 48);
            } else {
                context.fill(x + 5, y + 6, x + 53, y + 54, 0xFF555555);
            }

            int textX = x + 60;
            int textY = y + (entryHeight / 2) - 4;

            context.drawText(LibraryScreen.this.textRenderer, Text.literal(account.username), textX, textY, 0xFFFFFF, true);

            String statusLabel = account.isValid ? "Active" : "Checking...";
            int statusColor = account.isValid ? UiTheme.SUCCESS : UiTheme.WARNING;

            int textWidth = LibraryScreen.this.textRenderer.getWidth(statusLabel);
            context.drawText(LibraryScreen.this.textRenderer, Text.literal(statusLabel), x + entryWidth - textWidth - 5, textY, statusColor, true);
        }

        @Override
        public Text getNarration() {
            return Text.literal(account.username);
        }
    }
}
