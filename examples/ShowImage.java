///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.codejive.miniterm:miniterm${miniterm.ffm:}:${miniterm.version:0.1.5}
//DEPS org.codejive.miniterm:image:${miniterm.version:0.1.5}

package examples;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.codejive.miniterm.Terminal;
import org.codejive.miniterm.image.ImageEncoder;
import org.codejive.miniterm.image.ImageEncoders;

/**
 * Demo application showing how to use the terminal image encoding framework.
 *
 * <p>This example demonstrates rendering images to the terminal using different encoders (Sixel,
 * Kitty, iTerm2, and block-based Unicode rendering).
 *
 * <p>Usage: {@code ShowImage [--image=<path>] [--encoder=<name>] [--all]}
 *
 * <p>Supported encoder names: sixel, kitty, iterm2, block-full, block-half, block-quadrant,
 * block-sextant, block-octant.
 */
public class ShowImage {

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = Terminal.create()) {
            BufferedImage image = loadImage(args);

            // Define target size in terminal rows/columns
            int targetWidth = 20; // 20 columns wide
            int targetHeight = 10; // 10 rows tall

            terminal.write("=== Image Encoder Demo ===\n");

            boolean fitImage = true;

            String encoderName = getEncoderArg(args);

            if (encoderName != null) {
                // Use a specific encoder requested via --encoder=
                ImageEncoder.Provider provider = findProvider(encoderName);
                if (provider == null) {
                    terminal.write("Unknown encoder: " + encoderName + "\n");
                    terminal.write(
                            "Available: sixel, kitty, iterm2, block-full, block-half,"
                                    + " block-quadrant, block-sextant, block-octant\n");
                    return;
                }
                terminal.write("Using encoder: " + provider.name() + "\n\n");
                terminal.write("Rendering with " + provider.name() + " encoder:\n");
                renderImage(provider.create(image, targetWidth, targetHeight, fitImage), terminal);
                terminal.write("\n\n");
            } else {
                // Detect the best encoder for the current terminal
                ImageEncoder.Provider bestProvider = ImageEncoders.best();
                ImageEncoder detectedEncoder =
                        bestProvider.create(image, targetWidth, targetHeight, fitImage);
                terminal.write("Detected encoder: " + bestProvider.name() + "\n\n");

                // Try rendering with the detected encoder
                terminal.write("Rendering with " + bestProvider.name() + " encoder:\n");
                renderImage(detectedEncoder, terminal);
                terminal.write("\n\n");

                // Optionally try all available encoders
                if (shouldTestAllEncoders(args)) {
                    terminal.write("\n--- Testing all encoders ---\n\n");

                    for (ImageEncoder.Provider provider : ImageEncoders.providers()) {
                        testEncoder(
                                provider.name(),
                                provider.create(image, targetWidth, targetHeight, fitImage),
                                terminal);
                    }
                }
            }

            terminal.write("\nDemo complete!\n");
        }
    }

    private static String getEncoderArg(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--encoder=")) {
                return arg.substring("--encoder=".length());
            }
        }
        return null;
    }

    private static BufferedImage loadImage(String[] args) throws IOException {
        String imagePath = getImageArg(args);
        if (imagePath == null) {
            return createTestImage(200, 150);
        }

        BufferedImage image = ImageIO.read(new File(imagePath));
        if (image == null) {
            throw new IOException("Unsupported or unreadable image: " + imagePath);
        }
        return image;
    }

    private static String getImageArg(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--image=")) {
                return arg.substring("--image=".length());
            }
            if ("--image".equals(arg)) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --image");
                }
                return args[i + 1];
            }
        }
        return null;
    }

    private static ImageEncoder.Provider findProvider(String name) {
        String normalized = normalizeProviderName(name);
        List<ImageEncoder.Provider> all = ImageEncoders.providers();
        for (ImageEncoder.Provider provider : all) {
            String providerKey = normalizeProviderName(provider.name());
            if (providerKey.equals(normalized)) {
                return provider;
            }
        }
        return null;
    }

    private static String normalizeProviderName(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void testEncoder(String name, ImageEncoder encoder, Appendable output)
            throws IOException {
        output.append(name).append(" encoder:\n");
        renderImage(encoder, output);
        output.append("\n\n");
    }

    private static void renderImage(ImageEncoder encoder, Appendable output) throws IOException {
        encoder.render(output);
    }

    /**
     * Creates a simple test image with a gradient and some shapes.
     *
     * @param width the image width
     * @param height the image height
     * @return the created test image
     */
    private static BufferedImage createTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Draw gradient background
        for (int y = 0; y < height; y++) {
            float hue = (float) y / height;
            Color color = Color.getHSBColor(hue, 0.8f, 0.9f);
            g.setColor(color);
            g.fillRect(0, y, width, 1);
        }

        // Draw some shapes
        g.setColor(Color.WHITE);
        g.fillOval(width / 4, height / 4, width / 2, height / 2);

        g.setColor(Color.BLACK);
        g.drawString("Test Image", width / 3, height / 2);

        g.dispose();
        return image;
    }

    private static boolean shouldTestAllEncoders(String[] args) {
        for (String arg : args) {
            if ("--all".equals(arg) || "-a".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
