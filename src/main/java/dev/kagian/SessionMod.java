package dev.kagian;

import dev.kagian.util.LibraryManager;
import dev.kagian.util.SessionUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionMod implements ModInitializer {
    public static final String MOD_ID = "Session-Login";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Session originalSession;
    public static Session currentSession;

    @Override
    public void onInitialize() {
        try {
            LibraryManager.load();
            LibraryManager.validateAllBackground();
        } catch (Exception e) {
            // A library-load failure should never take the whole mod (or the game) down with it.
            LOGGER.error("Failed to load the account library - starting with an empty one", e);
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                if (originalSession == null) {
                    originalSession = SessionUtils.getSession();
                    currentSession = originalSession;

                    String uuid = originalSession.getUuidOrNull() != null
                            ? originalSession.getUuidOrNull().toString()
                            : null;
                    String token = originalSession.getAccessToken();

                    // Automatically save the original session to the Library Manager on boot
                    if (uuid != null && token != null && !token.isEmpty() && !token.equals("0")) {
                        LibraryManager.addOrUpdate(originalSession.getUsername(), uuid, token);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to capture the original session on startup", e);
            }
        });

        String version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        LOGGER.info("Session-Login v{} loaded", version);
    }
}
