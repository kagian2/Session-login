# 26.2 port notes - status as of latest compile round

This is built on top of the 26.1 port - **read `../26.1/PORT_NOTES.md` first**
for the full list of confirmed facts and remaining unknowns, all of which
apply here too and have already been applied to this version's source.

## What's different from 26.1
The one specific, documented 26.2-only change: `Minecraft.setScreen(Screen)`
was renamed to `Minecraft.setScreenAndShow(Screen)`. Applied everywhere the
mod opens a screen.

## Confirmed and already applied here (same as 26.1)
- `GuiGraphicsExtractor` (not `GuiGraphics`), with `fill` unchanged,
  `renderOutline` -> `outline`, `drawString` -> `text`, `drawTexture` -> `blit`
  taking `RenderPipelines.GUI_TEXTURED` (from
  `net.minecraft.client.renderer.RenderPipelines`) as a value, not a method
  reference.
- `Screen.render(...)` -> `Screen.extractRenderState(...)`.
- `ObjectSelectionList.Entry.render(...)` -> `extractContent(...)`, with the
  `index, y, x, entryWidth, entryHeight` parameters dropped in favor of
  `getX()`/`getY()`/`getWidth()`/`getHeight()`.
- `getNarration()` no longer overrides anything - left as a plain method with
  a PORT-TODO, same as 26.1.
- `User` has no `Type`/`AccountType` anymore - 5-param constructor only.
- `DynamicTexture` needs a `Supplier<String>` debug label as its first
  constructor argument.
- Build script: `net.fabricmc.fabric-loom` plugin (not legacy `fabric-loom`),
  no `mappings` block, plain `implementation` not `modImplementation`, Gradle
  9.4.0+, Java 25, and IntelliJ's Gradle JVM needs to be set explicitly (not
  `#JAVA_HOME`).

## Still unconfirmed
Same list as 26.1's notes: the mixin's target field name (`"user"`),
`Identifier.fromNamespaceAndPath(...)`'s exact name, and
`JoinMultiplayerScreen`'s package.

## How to proceed
Compile this one after 26.1 is clean - most fixes carry straight over, so
26.2 should need far fewer rounds than 26.1 did.
