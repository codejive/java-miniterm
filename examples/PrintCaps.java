///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.codejive.miniterm:miniterm${miniterm.ffm:}:${miniterm.version:0.2.0}
//DEPS org.codejive.miniterm:ansiparser:${miniterm.version:0.2.0}
//DEPS org.codejive.miniterm:termcap:${miniterm.version:0.2.0}

package examples;

import java.io.IOException;
import org.codejive.miniterm.Terminal;
import org.codejive.miniterm.termcap.TermCaps;
import org.codejive.miniterm.termcap.TermProber;

public class PrintCaps {
    public static void main(String[] args) {
        boolean force = args.length > 0 && "--force".equals(args[0]);
        
        TermCaps detected = TermCaps.detect();
        System.out.println("Terminal capabilities (detected from environment):");
        printCaps(detected);

        System.out.println();
        System.out.println("Terminal capabilities (queried):");
        try (Terminal terminal = Terminal.create()) {
            terminal.enableRawMode();
            TermCaps probed = TermProber.probe(terminal, () -> terminal.read(500));
            printCaps(probed);
            
            System.out.println();
            testStyles(probed, force);
        } catch (IOException e) {
            System.out.println("  (query failed: " + e.getMessage() + ")");
        }
    }

    private static void printCaps(TermCaps caps) {
        System.out.println("  colors              : " + caps.colors());
        System.out.println("  altScreen           : " + caps.altScreen());
        System.out.println("  mouse               : " + caps.mouse());
        System.out.println("  bracketedPaste      : " + caps.bracketedPaste());
        System.out.println("  focusTracking       : " + caps.focusTracking());
        System.out.println("  synchronizedOutput  : " + caps.synchronizedOutput());
        System.out.println("  hyperlinks          : " + caps.hyperlinks());
        System.out.println("  settableTitle       : " + caps.settableTitle());
        System.out.println("  unicode             : " + caps.unicode());
        System.out.println("  italic              : " + caps.italic());
        System.out.println("  strikethrough       : " + caps.strikethrough());
        System.out.println("  overline            : " + caps.overline());
        System.out.println("  undercurl           : " + caps.undercurl());
        System.out.println("  extendedUnderline   : " + caps.extendedUnderline());
        System.out.println("  sixel               : " + caps.sixel());
        System.out.println("  kittyGraphics       : " + caps.kittyGraphics());
        System.out.println("  iterm2Images        : " + caps.iterm2Images());
        System.out.println("  kittyKeyboard       : " + caps.kittyKeyboard());
    }

    private static void testStyles(TermCaps caps, boolean force) {
        System.out.println("  Testing text styles:");
        
        // Italic
        if (force || caps.italic()) {
            System.out.println("    \u001b[3mItalic text\u001b[23m" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Italic text (not supported)");
        }
        
        // Strikethrough
        if (force || caps.strikethrough()) {
            System.out.println("    \u001b[9mStrikethrough text\u001b[29m" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Strikethrough text (not supported)");
        }
        
        // Overline
        if (force || caps.overline()) {
            System.out.println("    \u001b[53mOverline text\u001b[55m" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Overline text (not supported)");
        }
        
        // Undercurl
        if (force || caps.undercurl()) {
            System.out.println("    \u001b[4:3mUndercurl text\u001b[4:0m" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Undercurl text (not supported)");
        }
        
        // Extended underline styles
        if (force || caps.extendedUnderline()) {
            System.out.println("    \u001b[4:1mSingle underline\u001b[4:0m" + (force ? "" : " (supported)"));
            System.out.println("    \u001b[4:2mDouble underline\u001b[4:0m" + (force ? "" : " (supported)"));
            System.out.println("    \u001b[4:4mDotted underline\u001b[4:0m" + (force ? "" : " (supported)"));
            System.out.println("    \u001b[4:5mDashed underline\u001b[4:0m" + (force ? "" : " (supported)"));
            System.out.println("    \u001b[4:1m\u001b[58:2::255:0:0mRed underline (RGB)\u001b[59m\u001b[4:0m" + (force ? "" : " (supported)"));
            System.out.println("    \u001b[4:1m\u001b[58:5:46mGreen underline (color 46)\u001b[59m\u001b[4:0m" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Extended underline styles (not supported)");
        }
        
        // Unicode
        if (force || caps.unicode()) {
            System.out.println("    Unicode: ✓ ★ ♥ ☺ → ⇒ ≈ ≠ ∞" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Unicode (not supported)");
        }
        
        // Hyperlinks
        if (force || caps.hyperlinks()) {
            System.out.println("    \u001b]8;;https://github.com/codejive/java-miniterm\u001b\\Hyperlink example\u001b]8;;\u001b\\" + (force ? "" : " (supported)"));
        } else {
            System.out.println("    Hyperlink example (not supported)");
        }
        
        System.out.println();
        if (!force) {
            System.out.println("  (Use --force to test all styles regardless of detected capabilities)");
        }
    }
}
