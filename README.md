# Session Login

A Fabric mod that lets you log into any of your Minecraft accounts instantly using a saved access token — no Microsoft login flow, no relaunching the game.

Built for people juggling a library of alt accounts (originally designed around a library of ~20) who got tired of signing in from scratch every time.

## Features

- **Token Login** — paste a Minecraft access token once and you're in. No browser popup, no re-entering credentials.
- **Account Library** — every account you log into is saved automatically. Pick one from a scrollable list (with live player-head avatars) and switch in a click.
- **Background validation** — saved tokens are checked in the background; expired ones are automatically cleaned out of your library instead of sitting there as dead entries.
- **Edit Account** — change the current account's username or skin directly from the Multiplayer screen, without leaving the game.
- **Restore** — jump back to your original logged-in account at any time.
- **Modern, responsive UI** — all network calls run off the render thread, so logging in or switching accounts never freezes the game. Clear status messages throughout ("Logging in...", "Token expired - removed", etc.).
- **Crash-resistant storage** — the account library is saved atomically (no corruption on a mid-write crash), and a corrupted library file is backed up rather than silently wiped.

## Supported Minecraft versions

| Version | Mappings | Notes |
|---|---|---|
| 1.21.4 | Yarn | Reference build |
| 1.21.8 | Yarn | |
| 1.21.11 | Yarn (last Yarn-mapped release) | |
| 26.1 | Official (unobfuscated) | Requires Java 25 — see `versions/26.1/PORT_NOTES.md` for a couple of still-unconfirmed mapping guesses |
| 26.2 | Official (unobfuscated) | Requires Java 25 — see `versions/26.2/PORT_NOTES.md` |

## Installation

1. Grab the jar matching your Minecraft version from the [Releases](../../releases) page.
2. Make sure you have the matching [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api) installed for that version.
3. Drop the jar into your `mods` folder.

## Building from source

Each supported version is its own independent, standalone Gradle project — there is no shared multi-module build. The repository root (this folder) is the **1.21.4** build; the others live under `versions/`:

```
Session-login/              # 1.21.4
versions/1.21.8/
versions/1.21.11/
versions/26.1/
versions/26.2/
```

To build one version, `cd` into its folder and run:

```
./gradlew build
```

1.21.4 / 1.21.8 / 1.21.11 need a Java 21 JDK. 26.1 / 26.2 need Java 25.

## Data storage

Saved accounts (username, UUID, access token) are stored locally in `config/session-login-library.json` inside your Minecraft instance folder. Nothing is sent anywhere except the standard Mojang/Minecraft Services API calls needed to log in, validate a token, or change a name/skin.

## License

All rights reserved.
