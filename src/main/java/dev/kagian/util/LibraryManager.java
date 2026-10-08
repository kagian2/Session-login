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

/**
 * Persists the account library to a JSON file in the Fabric config folder.
 * Saves are atomic (write to a temp file, then move into place) so a crash
 * mid-save can never corrupt the real file, and a corrupted file found on
 * load is backed up instead of silently wiping out the user's accounts.
 */
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
        public transient volatile boolean isValid = false; // Checked in background, never persisted

        public SavedAccount(String username, String uuid, String token) {
            this.username = username;
            this.uuid = uuid;
            this.token = token;
        }
    }

    // Loads the JSON file
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

    // Saves the ACCOUNTS list to the JSON file, atomically
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

    // Adds a new account or updates an existing one
    public static void addOrUpdate(String username, String uuid, String token) {
        ACCOUNTS.removeIf(acc -> acc.username.equalsIgnoreCase(username));
        ACCOUNTS.add(new SavedAccount(username, uuid, token));
        save();
        validateAllBackground(); // Re-validate after adding
    }

    public static void remove(SavedAccount account) {
        ACCOUNTS.remove(account);
        save();
    }

    // Runs validation asynchronously so it doesn't freeze the client on startup
    public static void validateAllBackground() {
        CompletableFuture.runAsync(() -> {
            List<SavedAccount> toRemove = new ArrayList<>();

            for (SavedAccount acc : ACCOUNTS) {
                try {
                    APIUtils.getProfileInfo(acc.token);
                    acc.isValid = true;
                } catch (Exception e) {
                    // Token is invalid/expired
                    toRemove.add(acc);
                }
            }

            if (!toRemove.isEmpty()) {
                ACCOUNTS.removeAll(toRemove);
                save(); // Automatically update the file after deletion
                LOGGER.info("Cleaned up {} expired session(s)", toRemove.size());
            }
        }).exceptionally(e -> {
            LOGGER.error("Account validation pass failed unexpectedly", e);
            return null;
        });
    }
}
