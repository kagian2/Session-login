package dev.kagian.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

// PORT-TODO: the field name is the single biggest unknown in this whole port. Yarn's
// "session" field on MinecraftClient has historically mapped to "user" on Minecraft in
// Mojang mappings - this will compile fine even if wrong (Mixin resolves string targets
// at runtime, not compile time), but will throw a MixinApplyError on launch if the name is
// off. If that happens, check the decompiled Minecraft class for the User-typed field name.
@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Accessor("user")
    @Mutable
    void setSession(User user);
}
