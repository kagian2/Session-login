package dev.kagian.mixin;

import dev.kagian.gui.EditAccountScreen;
import dev.kagian.gui.LibraryScreen;
import dev.kagian.gui.LoginScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {

    protected MultiplayerScreenMixin(Component title) { super(title); }

    @Inject(at = @At("TAIL"), method = "init")
    private void addCustomButtons(CallbackInfo info) {

        // PORT-TODO (26.2-specific): "setScreen" is documented as renamed to "setScreenAndShow"
        // in 26.2 - if the compiler says setScreenAndShow doesn't exist, this may only apply to
        // a later 26.2 build than the one you're compiling against; fall back to setScreen(...).
        this.addRenderableWidget(Button.builder(Component.literal("Token Login"), button ->
                        Minecraft.getInstance().setScreenAndShow(new LoginScreen()))
                .tooltip(Tooltip.create(Component.literal("Log in with a Minecraft access token")))
                .bounds(5, 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Edit Account"), button ->
                        Minecraft.getInstance().setScreenAndShow(new EditAccountScreen()))
                .tooltip(Tooltip.create(Component.literal("Change the current account's name or skin")))
                .bounds(90, 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Library"), button ->
                        Minecraft.getInstance().setScreenAndShow(new LibraryScreen()))
                .tooltip(Tooltip.create(Component.literal("Switch between your saved accounts")))
                .bounds(175, 5, 80, 20).build());
    }
}
