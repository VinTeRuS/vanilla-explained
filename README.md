# Why? — Vanilla Explained

A client-side mod that explains vanilla Minecraft in plain words. It adds no blocks, items or
other content, and it works on vanilla servers.

## How it works
- **G — Why?** Look at something and press G. A small Jade-style tooltip at the top of the screen
  shows the verdict and the key facts. Hold **Shift** for every detail and a ★ pro tip. The tooltip
  fades when you look away.
  - **Any block:** can monsters spawn here, and why not: light (with the exact chance), floor, room,
    distance, mob cap, and the biome's monsters shown as spawn-egg icons.
  - **Crops:** growth points out of 10, hydration, same-crop penalties, light, time until ripe.
  - **Redstone:** signal strength and its source, repeater delay and locking, comparator inputs and
    modes, observers, pistons (push check, the blocking block, quasi-connectivity), hoppers, note
    blocks, rails, daylight detectors, what a comparator reads from any container.
  - **Enchanting tables and beacons:** working bookshelves, blocked shelves, pyramid levels, what's
    missing, whether the beam is blocked.
  - **Nether portals:** where a portal leads, what it links to, and one-way links with a fix.
- **J — Planner** (a small panel docked to the right; the world stays visible):
  - **Elytra:** rockets, time and durability for a trip to X/Z; on-screen guidance while you fly.
    The mod learns your real blocks-per-rocket rate.
  - **Portals:** your coordinates in the other dimension and what a portal built here would link to.
  - **Redstone:** items needed for every comparator signal for 7 container types.
  - **Secrets:** 27 little-known mechanics, each checked against the game code.
- **Anvil — "Best order" button:** the cheapest order to apply your books, avoiding
  "Too Expensive!".

Languages: English, Русский, Українська.

## Versions
| Loader | Minecraft |
|---|---|
| Fabric | 1.20.1, 1.20.2–1.20.4, 1.20.5–1.20.6, 1.21–1.21.1, 1.21.2–1.21.4, 1.21.5, 1.21.6–1.21.8, 1.21.9–1.21.10, 1.21.11, 26.1–26.1.2, 26.2, 26.3 |
| Forge | 1.20.1 |

The project is a single source tree. [Stonecutter](https://stonecutter.kikugie.dev/) builds every
version from it. Version-specific calls live in `client/core/Mc.java`, `client/core/Gfx.java`,
`client/core/VeScreen.java` and `client/anvil/EnchCompat.java`.

## Build
Gradle has to run on a full JDK 25:
```
JAVA_HOME=/path/to/jdk-25 ./gradlew :26.3-fabric:build        # one version
JAVA_HOME=/path/to/jdk-25 ./gradlew build                     # everything
```
Jars land in `versions/<version>-<loader>/build/libs/`.

Tests:
- `./gradlew :26.3-fabric:test`: unit tests of the rule math.
- `./gradlew :26.3-fabric:runClientGameTest`: an in-game test that builds scenes in a real client,
  checks every explanation and saves screenshots.
- `VE_SMOKE=1 ./gradlew :<version>-<loader>:runClient`: a smoke test for versions without the
  gametest API. It opens every panel on the title screen, saves screenshots and quits.

## 📜 License

Except where otherwise stated (see [third-party license notices](thirdparty/NOTICE.txt)), the content of this
repository is provided under the [Polyform Shield 1.0.0](LICENSE.md) license by VinTeRuS.

Required Notice: Copyright 2026 VinTeRuS (https://github.com/VinTeRuS/vanilla-explained)
