# image

`image` is a Java 8+ terminal image rendering utility, part of the [java-miniterm](../README.md) project.

It can render `BufferedImage` objects to terminal graphics protocols such as Kitty, iTerm2, and Sixel, with a Unicode block fallback for terminals that do not support a native graphics protocol. The module automatically detects the best protocol for the current terminal and exposes a small, configurable encoder API.

## Usage

The simplest entry point is `ImageEncoders.best()`, which picks the highest-priority provider currently supported by the environment:

```java
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.codejive.miniterm.Terminal;
import org.codejive.miniterm.image.ImageEncoder;
import org.codejive.miniterm.image.ImageEncoders;

BufferedImage image = ImageIO.read(new java.io.File("photo.png"));
ImageEncoder encoder = ImageEncoders.best().create(image, 40, 12, false);

try (Terminal terminal = Terminal.create()) {
    encoder.render(terminal);
}
```

`ImageEncoder` is stateful: the image and initial target size are fixed when the encoder is created, while the output size and fit mode can be adjusted afterwards.

```java
encoder.targetSize(60, 20).fitImage(true);
encoder.render(terminal);
```

### Detect supported providers

```java
for (ImageEncoder.Provider provider : ImageEncoders.supportedProviders()) {
    System.out.println(provider.name() + " -> " + provider.resolution());
}
```

This returns providers in priority order, with the best option first. `supportedProviders()` checks common terminal environment variables and chooses the most appropriate protocol for the current session.

## Supported protocols

| Protocol | Typical terminals | Notes |
|---|---|---|
| `Kitty` | Kitty, Ghostty, Konsole, WezTerm | Modern, efficient PNG graphics protocol |
| `iTerm2` | iTerm2, WezTerm, VS Code, Mintty | Inline images using OSC 1337 |
| `Sixel` | Konsole, Windows Terminal, mlterm, foot, others | Bitmap graphics via DCS/Sixel |
| `Block` | Any terminal with Unicode support | Fallback renderer using block characters |

The block encoders are exposed as several variants (`FULL`, `HALF`, `QUADRANT`, `SEXTANT`, `OCTANT`) to trade fidelity for compatibility.

## Encoding model

Each provider implements the same `ImageEncoder` API:

- `targetWidth()` / `targetHeight()` — target dimensions in terminal columns and rows
- `targetSize(int width, int height)` — resize the rendered output
- `fitImage()` / `fitImage(boolean)` — preserve aspect ratio or stretch to fill the target box
- `render(Appendable output)` — emit terminal escape sequences

The rendering work is cached. When you change the target size or fit mode, the encoder invalidates its cached transformation and re-renders lazily on the next call.

## Adding the dependency

`image` emits ANSI escape sequences and therefore requires `ansiparser` on the classpath at runtime. The module depends on it as an optional library in Maven so you can keep the dependency explicit in your own build.

### JBang

```java
//DEPS org.codejive.miniterm:image:0.1.5
//DEPS org.codejive.miniterm:ansiparser:0.1.5
```

### Maven

```xml
<dependency>
    <groupId>org.codejive.miniterm</groupId>
    <artifactId>image</artifactId>
    <version>0.1.5</version>
</dependency>
<dependency>
    <groupId>org.codejive.miniterm</groupId>
    <artifactId>ansiparser</artifactId>
    <version>0.1.5</version>
</dependency>
```

### Gradle

```kotlin
implementation("org.codejive.miniterm:image:0.1.5")
implementation("org.codejive.miniterm:ansiparser:0.1.5")
```

## Building

```bash
./mvnw clean install
```
