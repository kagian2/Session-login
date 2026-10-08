package dev.kagian.util;

import dev.kagian.SessionMod;
import dev.kagian.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class SessionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("Session-Login");

    // Creates the Session object
    public static Session createSession(String username, String uuidString, String token) {
        // Formats the UUID if it lacks dashes
        if (uuidString.length() == 32) {
            uuidString = uuidString.substring(0, 8) + "-" +
                    uuidString.substring(8, 12) + "-" +
                    uuidString.substring(12, 16) + "-" +
                    uuidString.substring(16, 20) + "-" +
                    uuidString.substring(20);
        }

        return new Session(
                username,
                UUID.fromString(uuidString),
                token,
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.MSA
        );
    }

    // Injects the session into the client
    public static void setSession(Session newSession) {
        if (newSession == null) {
            LOGGER.warn("Tried to apply a null session - ignoring");
            return;
        }
        try {
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).setSession(newSession);
        } catch (ClassCastException e) {
            LOGGER.error("MinecraftClientAccessor mixin isn't applied - can't swap the session", e);
        }
    }

    // Fetches the current session from the client
    public static Session getSession() {
        return MinecraftClient.getInstance().getSession();
    }

    // Restores the original session and updates the tracked state
    public static void restoreSession() {
        if (SessionMod.originalSession != null) {
            setSession(SessionMod.originalSession);
            SessionMod.currentSession = SessionMod.originalSession;
        }
    }
}
