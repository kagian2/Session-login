package dev.kagian.mixin;

import dev.kagian.gui.EditAccountScreen;
import dev.kagian.gui.LibraryScreen;
import dev.kagian.gui.LoginScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {

    protected MultiplayerScreenMixin(Text title) { super(title); }

    @Inject(at = @At("TAIL"), method = "init")
    private void addCustomButtons(CallbackInfo info) {

        // Token Login Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Token Login"), button ->
                        MinecraftClient.getInstance().setScreen(new LoginScreen()))
                .tooltip(Tooltip.of(Text.literal("Log in with a Minecraft access token")))
                .dimensions(5, 5, 80, 20).build());

        // Edit Account Button placed next to the Login button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit Account"), button ->
                        MinecraftClient.getInstance().setScreen(new EditAccountScreen()))
                .tooltip(Tooltip.of(Text.literal("Change the current account's name or skin")))
                .dimensions(90, 5, 80, 20).build());

        // Library Button placed next to the Edit Account button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Library"), button ->
                        MinecraftClient.getInstance().setScreen(new LibraryScreen()))
                .tooltip(Tooltip.of(Text.literal("Switch between your saved accounts")))
                .dimensions(175, 5, 80, 20).build());
    }
}
