///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.codejive.miniterm:miniterm${miniterm.ffm:}:${miniterm.version:0.1.7}
//DEPS org.codejive.miniterm:colors:${miniterm.version:0.1.7}

package examples;

import java.io.IOException;
import org.codejive.miniterm.Terminal;
import org.codejive.miniterm.colors.Color;
import org.codejive.miniterm.colors.TermColors;

public class PrintColors {

    // The standard ANSI colors (red, green, yellow, blue, magenta, cyan) live at palette
    // indices 1-6 and are also reachable through the classic SGR 31-36 escape codes.
    private static final int[] SAMPLE_INDICES = {1, 2, 3, 4, 5, 6};

    public static void main(String[] args) {
        try (Terminal terminal = Terminal.create()) {
            terminal.enableRawMode();

            printColorKinds();
            printBaseColors(terminal);
            Color.RgbColor[] palette = printPalette(terminal);
            demoTruecolor();
            demoPaletteChange(terminal, palette);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Demonstrates the three Color implementations that don't require querying the terminal:
    // BasicColor (the 8 classic ANSI colors, in normal/dark/bright intensities), IndexedColor
    // (the 256-color palette by index) and DefaultColor (resets to the terminal's own color).
    private static void printColorKinds() {
        System.out.println("BasicColor - classic ANSI colors (normal / dark / bright):");
        printBasicColorRow(Color.BasicColor.normalColors);
        printBasicColorRow(Color.BasicColor.darkColors);
        printBasicColorRow(Color.BasicColor.brightColors);
        System.out.println();

        System.out.println("IndexedColor - selected entries from the 256-color palette:");
        StringBuilder line = new StringBuilder("  ");
        for (int index : new int[] {16, 82, 94, 129, 160, 196, 214, 226}) {
            line.append(colored(Color.indexed(index), String.format("%3d", index)));
        }
        System.out.println(line);
        System.out.println();

        System.out.println("DefaultColor - resets foreground/background to the terminal default:");
        System.out.println(
                "  "
                        + colored(Color.BasicColor.green, "colored")
                        + " "
                        + colored(Color.DEFAULT, "default")
                        + " text");
        System.out.println();
    }

    private static void printBasicColorRow(Color.BasicColor[] colors) {
        StringBuilder line = new StringBuilder("  ");
        for (Color.BasicColor color : colors) {
            line.append(colored(color, color.toString()));
        }
        System.out.println(line);
    }

    // Wraps text in the CSI sequence for the given color's foreground style, followed by a reset.
    private static String colored(Color color, String text) {
        return "\u001b[" + color.toForegroundStyle() + "m" + text + "\u001b[0m ";
    }

    private static void printBaseColors(Terminal terminal) throws IOException {
        Color fg = TermColors.queryForeground(terminal, () -> terminal.read(500));
        Color bg = TermColors.queryBackground(terminal, () -> terminal.read(500));
        Color cursor = TermColors.queryCursor(terminal, () -> terminal.read(500));
        System.out.println("Terminal colors (queried via OSC):");
        System.out.println("  foreground : " + (fg != null ? fg : "not reported"));
        System.out.println("  background : " + (bg != null ? bg : "not reported"));
        System.out.println("  cursor     : " + (cursor != null ? cursor : "not reported"));
        System.out.println();
    }

    private static Color.RgbColor[] printPalette(Terminal terminal) throws IOException {
        System.out.println("Full 256-color palette (queried via OSC 4):");
        Color.RgbColor[] palette = TermColors.queryPalette(terminal, () -> terminal.read(500));
        for (int row = 0; row < 16; row++) {
            StringBuilder line = new StringBuilder("  ");
            for (int col = 0; col < 16; col++) {
                int index = row * 16 + col;
                line.append(swatch(index, palette[index]));
            }
            System.out.println(line);
        }
        System.out.println();
        return palette;
    }

    private static String swatch(int index, Color.RgbColor color) {
        if (color == null) {
            return String.format("%4d? ", index);
        }
        // Use indexed color (48;5;N) instead of RGB (48;2;R;G;B) to display the actual
        // palette entry as the terminal interprets it, not our queried RGB approximation.
        return String.format("\u001b[48;5;%dm%4d\u001b[0m ", index, index);
    }

    private static void demoPaletteChange(Terminal terminal, Color.RgbColor[] original) throws IOException {
        System.out.println("Before: standard ANSI colors 1-6 using their current palette entries");
        printSample();

        System.out.println();
        System.out.println("Press Enter to swap those palette entries for a custom color scheme...");
        waitForEnter(terminal);

        Color.RgbColor[] custom = new Color.RgbColor[256];
        custom[1] = Color.rgb8(0xE0, 0x60, 0x60); // red -> soft red
        custom[2] = Color.rgb8(0x60, 0xC0, 0x80); // green -> mint
        custom[3] = Color.rgb8(0xE0, 0xC0, 0x60); // yellow -> gold
        custom[4] = Color.rgb8(0x60, 0x90, 0xE0); // blue -> sky blue
        custom[5] = Color.rgb8(0xC0, 0x70, 0xE0); // magenta -> lavender
        custom[6] = Color.rgb8(0x50, 0xC0, 0xC0); // cyan -> teal
        TermColors.setPalette(terminal, custom);

        System.out.println("After: same text, new palette entries");
        printSample();

        System.out.println();
        System.out.println("Press Enter to restore the original palette entries...");
        waitForEnter(terminal);

        restoreSample(terminal, original);
        System.out.println("Palette restored.");
    }

    private static void printSample() {
        // Deliberately not bold: many terminals (e.g. mintty) render bold + color 1-6 using the
        // *bright* palette entries (9-14) instead, which would hide the palette change we made.
        StringBuilder line = new StringBuilder("  ");
        for (int index : SAMPLE_INDICES) {
            line.append(
                    String.format("\u001b[3%dm\u2588\u2588 color %d \u2588\u2588\u001b[0m  ", index, index));
        }
        System.out.println(line);
    }

    private static void restoreSample(Terminal terminal, Color.RgbColor[] original) throws IOException {
        for (int index : SAMPLE_INDICES) {
            if (original[index] != null) {
                TermColors.setColor(terminal, index, original[index]);
            } else {
                TermColors.resetColor(terminal, index);
            }
        }
    }

    private static void waitForEnter(Terminal terminal) throws IOException {
        int c;
        do {
            c = terminal.read(-1);
        } while (c != '\r' && c != '\n' && c != -1);
    }

    private static void demoTruecolor() {
        System.out.println("Truecolor (24-bit RGB) - direct color specification:");
        System.out.println("  Gradient from red to blue:");
        StringBuilder gradient = new StringBuilder("  ");
        for (int i = 0; i < 40; i++) {
            int r = 255 - (i * 255 / 39);
            int g = 0;
            int b = (i * 255 / 39);
            Color.RgbColor color = Color.rgb8(r, g, b);
            gradient.append(String.format("\u001b[48;2;%d;%d;%dm ", r, g, b));
        }
        gradient.append("\u001b[0m");
        System.out.println(gradient);
        
        System.out.println("  Rainbow spectrum:");
        StringBuilder rainbow = new StringBuilder("  ");
        for (int i = 0; i < 40; i++) {
            float hue = i / 40.0f;
            int[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
            rainbow.append(String.format("\u001b[48;2;%d;%d;%dm ", rgb[0], rgb[1], rgb[2]));
        }
        rainbow.append("\u001b[0m");
        System.out.println(rainbow);
        System.out.println();
    }

    private static int[] hsvToRgb(float h, float s, float v) {
        int hi = (int) (h * 6);
        float f = h * 6 - hi;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        
        float r, g, b;
        switch (hi % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        
        return new int[] {(int)(r * 255), (int)(g * 255), (int)(b * 255)};
    }
}
