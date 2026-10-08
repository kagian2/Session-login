package dev.kagian.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.kagian.api.APIUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class LibraryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Session-Login");
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("session-login-library.json");
    private static final Path TEMP_FILE = FabricLoader.getInstance().getConfigDir().resolve("session-login-library.json.tmp");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final List<SavedAccount> ACCOUNTS = new CopyOnWriteArrayList<>();

    public static class SavedAccount {
        public String username;
        public String uuid;
        public String token;
        public transient volatile boolean isValid = false;

        public SavedAccount(String username, String uuid, String token) {
            this.username = username;
            this.uuid = uuid;
            this.token = token;
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            ACCOUNTS.clear();
            if (array != null) {
                for (JsonElement el : array) {
                    try {
                        JsonObject obj = el.getAsJsonObject();
                        ACCOUNTS.add(new SavedAccount(
                                obj.get("username").getAsString(),
                                obj.get("uuid").getAsString(),
                                obj.get("token").getAsString()
                        ));
                    } catch (RuntimeException entryError) {
                        LOGGER.warn("Skipping an unreadable account entry in the library", entryError);
                    }
                }
            }
        } catch (JsonParseException corrupted) {
            LOGGER.error("session-login-library.json is corrupted - backing it up instead of losing it", corrupted);
            backupCorruptedFile();
        } catch (IOException e) {
            LOGGER.error("Failed to load the session library", e);
        }
    }

    private static void backupCorruptedFile() {
        try {
            Path backup = CONFIG_FILE.resolveSibling("session-login-library.corrupted-" + System.currentTimeMillis() + ".json");
            Files.move(CONFIG_FILE, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Corrupted library backed up to {}", backup);
        } catch (IOException e) {
            LOGGER.error("Could not back up the corrupted library file", e);
        }
    }

    public static synchronized void save() {
        try {
            JsonArray array = new JsonArray();
            for (SavedAccount acc : ACCOUNTS) {
                JsonObject obj = new JsonObject();
                obj.addProperty("username", acc.username);
                obj.addProperty("uuid", acc.uuid);
                obj.addProperty("token", acc.token);
                array.add(obj);
            }

            try (Writer writer = Files.newBufferedWriter(TEMP_FILE)) {
                GSON.toJson(array, writer);
            }

            try {
                Files.move(TEMP_FILE, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException notAtomic) {
                Files.move(TEMP_FILE, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save the session library", e);
        }
    }

    public static void addOrUpdate(String username, String uuid, String token) {
        ACCOUNTS.removeIf(acc -> acc.username.equalsIgnoreCase(username));
        ACCOUNTS.add(new SavedAccount(username, uuid, token));
        save();
        validateAllBackground();
    }

    public static void remove(SavedAccount account) {
        ACCOUNTS.remove(account);
        save();
    }

    public static void validateAllBackground() {
        CompletableFuture.runAsync(() -> {
            List<SavedAccount> toRemove = new ArrayList<>();

            for (SavedAccount acc : ACCOUNTS) {
                try {
                    APIUtils.getProfileInfo(acc.token);
                    acc.isValid = true;
                } catch (Exception e) {
                    toRemove.add(acc);
                }
            }

            if (!toRemove.isEmpty()) {
                ACCOUNTS.removeAll(toRemove);
                save();
                LOGGER.info("Cleaned up {} expired session(s)", toRemove.size());
            }
        }).exceptionally(e -> {
            LOGGER.error("Account validation pass failed unexpectedly", e);
            return null;
        });
    }
}
