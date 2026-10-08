package dev.kagian.mixin;

import dev.kagian.gui.EditAccountScreen;
import dev.kagian.gui.LibraryScreen;
import dev.kagian.gui.LoginScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
// PORT-TODO: MultiplayerScreen is widely believed to be renamed "JoinMultiplayerScreen" in
// Mojang mappings, package net.minecraft.client.gui.screens.multiplayer - verify both the
// name and the package against the compiler.
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

        this.addRenderableWidget(Button.builder(Component.literal("Token Login"), button ->
                        Minecraft.getInstance().setScreen(new LoginScreen()))
                .tooltip(Tooltip.create(Component.literal("Log in with a Minecraft access token")))
                .bounds(5, 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Edit Account"), button ->
                        Minecraft.getInstance().setScreen(new EditAccountScreen()))
                .tooltip(Tooltip.create(Component.literal("Change the current account's name or skin")))
                .bounds(90, 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Library"), button ->
                        Minecraft.getInstance().setScreen(new LibraryScreen()))
                .tooltip(Tooltip.create(Component.literal("Switch between your saved accounts")))
                .bounds(175, 5, 80, 20).build());
    }
}
