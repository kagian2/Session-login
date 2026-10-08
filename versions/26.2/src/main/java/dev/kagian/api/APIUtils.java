package dev.kagian.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
// PORT-TODO: see PORT_NOTES.md (shared with the 26.1 port) for this package guess.
import com.mojang.blaze3d.platform.NativeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class APIUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("Session-Login");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public record ProfileInfo(String username, String uuid) {
    }

    public static ProfileInfo getProfileInfo(String token) throws ApiException {
        if (token == null || token.isBlank()) {
            throw new ApiException("Token is empty", -1);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiException("Invalid or expired token (HTTP " + response.statusCode() + ")", response.statusCode());
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String name = json.get("name").getAsString();
            String uuid = json.get("id").getAsString();
            return new ProfileInfo(name, uuid);
        } catch (ApiException e) {
            throw e;
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            LOGGER.warn("Malformed profile response from Mojang", e);
            throw new ApiException("Mojang returned an unexpected response", e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("Failed to reach the Mojang API", e);
            throw new ApiException("Couldn't reach Mojang's servers - check your connection", e);
        }
    }

    public static int changeName(String newName, String token) {
        if (newName == null || newName.isBlank() || token == null) {
            return -1;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/name/" + newName))
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception e) {
            LOGGER.warn("Failed to change name", e);
            return -1;
        }
    }

    public static int changeSkin(String url, String token) {
        if (url == null || url.isBlank() || token == null) {
            return -1;
        }
        try {
            String jsonPayload = String.format("{ \"variant\": \"classic\", \"url\": \"%s\"}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception e) {
            LOGGER.warn("Failed to change skin", e);
            return -1;
        }
    }

    public static NativeImage getFaceByUsername(String username) throws Exception {
        URI uri = URI.create("https://minotar.net/helm/" + username + "/48.png");
        try (InputStream stream = uri.toURL().openStream()) {
            return NativeImage.read(stream);
        }
    }
}
