package dev.kagian.api;

/**
 * Thrown whenever a Mojang/Minecraft Services API call fails - bad token,
 * network error, rate limit, malformed response, etc. Carries a short,
 * user-presentable message so screens can show it directly without guessing
 * what went wrong.
 */
public class ApiException extends Exception {
    private final int statusCode;

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
