package dev.kagian;

import dev.kagian.util.LibraryManager;
import dev.kagian.util.SessionUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionMod implements ModInitializer {
    public static final String MOD_ID = "Session-Login";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static User originalSession;
    public static User currentSession;

    @Override
    public void onInitialize() {
        try {
            LibraryManager.load();
            LibraryManager.validateAllBackground();
        } catch (Exception e) {
            LOGGER.error("Failed to load the account library - starting with an empty one", e);
        }

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                if (originalSession == null) {
                    originalSession = SessionUtils.getSession();
                    currentSession = originalSession;

                    String uuid = originalSession.getProfileId() != null
                            ? originalSession.getProfileId().toString()
                            : null;
                    String token = originalSession.getAccessToken();

                    if (uuid != null && token != null && !token.isEmpty() && !token.equals("0")) {
                        LibraryManager.addOrUpdate(originalSession.getName(), uuid, token);
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
