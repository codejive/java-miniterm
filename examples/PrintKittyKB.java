///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.codejive.miniterm:miniterm${miniterm.ffm:}:${miniterm.version:0.2.0}
//DEPS org.codejive.miniterm:ansiparser:${miniterm.version:0.2.0}
//DEPS org.codejive.miniterm:termcap:${miniterm.version:0.2.0}

package examples;

import java.io.IOException;
import org.codejive.miniterm.Terminal;
import org.codejive.miniterm.ansiparser.AnsiReader;
import org.codejive.miniterm.termcap.TermCaps;
import org.codejive.miniterm.termcap.TermProber;

public class PrintKittyKB {

    // ANSI helpers
    private static final String CSI = "\033[";

    public static void main(String[] args) {
        try (Terminal terminal = Terminal.create()) {
            terminal.enableRawMode();

            // Detect terminal capabilities
            TermCaps caps = TermProber.probe(terminal, () -> terminal.read(500));

            System.out.println("Terminal capabilities:");
            System.out.println("  Kitty keyboard support: " + caps.kittyKeyboard());
            System.out.println();

            if (!caps.kittyKeyboard()) {
                System.out.println("Warning: Terminal does not report Kitty keyboard support.");
                System.out.println("Attempting to enable anyway (some terminals support it without reporting).");
                System.out.println();
            }

            // Query terminal for Kitty keyboard protocol support
            System.out.println("Querying terminal for Kitty keyboard protocol...");
            TermCaps.queryKittyKeyboard(terminal);

            // Try to read the response
            AnsiReader reader = new AnsiReader(() -> terminal.read(500));
            String response = reader.read();
            if (response != null && TermCaps.isKittyKeyboardResponse(response)) {
                System.out.println("Terminal responded: " + escape(response));
                System.out.println("Kitty keyboard protocol is supported!");
            } else {
                System.out.println("No response received (timeout or not supported).");
            }
            System.out.println();

            // Enable Kitty keyboard protocol with flags:
            // 1 = Disambiguate escape codes
            // 2 = Report event types (press, release, repeat)
            // 4 = Report alternate keys
            // Combined: 7 = Full enhancement
            int flags = 7;
            System.out.println("Enabling Kitty keyboard protocol with flags: " + flags);
            System.out.println("  1 = Disambiguate escape codes");
            System.out.println("  2 = Report event types (press, release, repeat)");
            System.out.println("  4 = Report alternate keys");
            System.out.println();

            TermCaps.pushKittyKeyboard(terminal, flags);

            System.out.println();
            System.out.println("Press keys to see enhanced keyboard events.");
            System.out.println("Try: letters (plain text), Ctrl+Shift+A, Alt+Shift+B, arrow keys.");
            System.out.println("(Press Ctrl+C to exit)");
            System.out.println();
            System.out.println(String.format("%-40s | %-30s | %s", "Raw", "Hex", "Type"));
            System.out.println(String.format("%-40s-+-%-30s-+-%s", 
                "-".repeat(40), "-".repeat(30), "-".repeat(30)));

            try {
                String token;
                while ((token = reader.read()) != null) {
                    if (token.isEmpty()) continue;

                    // Check for Ctrl+C (raw byte 3 or Kitty protocol CSI 99;5u)
                    if (!token.startsWith("\033") && token.length() == 1 && token.charAt(0) == 3) {
                        break;
                    }
                    // Check for Kitty-encoded Ctrl+C: CSI 99;5u (ESC[99;5u)
                    if ((CSI + "99;5u").equals(token)) {
                        break;
                    }

                    // Print the event details on a single line
                    String type = analyzeToken(token);
                    System.out.println(String.format("%-40s | %-30s | %s", 
                        escape(token), toHex(token), type));
                }
            } finally {
                // Restore original keyboard protocol
                TermCaps.popKittyKeyboard(terminal);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static String analyzeToken(String token) {
        if (token == null || token.isEmpty()) {
            return "Empty";
        }

        // Check for Kitty keyboard protocol sequences
        // Format: CSI <unicode-key-code> : <alternate-key-codes> ; <modifiers> : <event-type> ; <text> u
        if (token.startsWith(CSI) && token.endsWith("u")) {
            return "Kitty keyboard protocol sequence";
        }

        // Check for standard CSI sequences
        if (token.startsWith(CSI)) {
            return "CSI sequence (standard)";
        }

        // Check for escape sequences
        if (token.startsWith("\033")) {
            return "Escape sequence";
        }

        // Check for control characters
        if (token.length() == 1) {
            char c = token.charAt(0);
            if (c < 32) {
                return "Control character (Ctrl+" + (char) (c + 64) + ")";
            }
            if (c == 127) {
                return "DEL character";
            }
            return "Printable character: '" + c + "'";
        }

        // Multi-byte sequence
        return "Multi-byte sequence";
    }

    private static String escape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\033') {
                sb.append("ESC");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 32 || c == 127) {
                sb.append("\\x").append(String.format("%02x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toHex(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02x", (int) s.charAt(i)));
        }
        return sb.toString();
    }
}
