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

            printBaseColors(terminal);
            Color[] palette = printPalette(terminal);
            demoPaletteChange(terminal, palette);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private static Color[] printPalette(Terminal terminal) throws IOException {
        System.out.println("Full 256-color palette (queried via OSC 4):");
        Color[] palette = TermColors.queryPalette(terminal, () -> terminal.read(500));
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

    private static String swatch(int index, Color color) {
        if (color == null) {
            return String.format("%4d? ", index);
        }
        return String.format(
                "\u001b[48;2;%d;%d;%dm%4d\u001b[0m ", color.r8(), color.g8(), color.b8(), index);
    }

    private static void demoPaletteChange(Terminal terminal, Color[] original) throws IOException {
        System.out.println("Before: standard ANSI colors 1-6 using their current palette entries");
        printSample();

        System.out.println();
        System.out.println("Press Enter to swap those palette entries for a custom color scheme...");
        waitForEnter(terminal);

        Color[] custom = new Color[256];
        custom[1] = Color.ofRgb8(0xE0, 0x60, 0x60); // red -> soft red
        custom[2] = Color.ofRgb8(0x60, 0xC0, 0x80); // green -> mint
        custom[3] = Color.ofRgb8(0xE0, 0xC0, 0x60); // yellow -> gold
        custom[4] = Color.ofRgb8(0x60, 0x90, 0xE0); // blue -> sky blue
        custom[5] = Color.ofRgb8(0xC0, 0x70, 0xE0); // magenta -> lavender
        custom[6] = Color.ofRgb8(0x50, 0xC0, 0xC0); // cyan -> teal
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

    private static void restoreSample(Terminal terminal, Color[] original) throws IOException {
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
}
