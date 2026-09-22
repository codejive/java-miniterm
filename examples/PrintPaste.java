///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.codejive.miniterm:miniterm${miniterm.ffm:}:${miniterm.version:0.2.0}
//DEPS org.codejive.miniterm:ansiparser:${miniterm.version:0.2.0}

package examples;

import java.io.IOException;
import org.codejive.miniterm.Terminal;
import org.codejive.miniterm.ansiparser.AnsiParser;

public class PrintPaste {
    private static final String ENABLE_BRACKETED_PASTE = "\u001b[?2004h";
    private static final String DISABLE_BRACKETED_PASTE = "\u001b[?2004l";
    private static final String PASTE_START = "\u001b[200~";
    private static final String PASTE_END = "\u001b[201~";

    public static void main(String[] args) {
        try (Terminal terminal = Terminal.create()) {
            terminal.enableRawMode();
            terminal.write(ENABLE_BRACKETED_PASTE);
            System.out.println("Bracketed paste mode enabled.");
            System.out.println("Try pasting text (Ctrl+V or right-click paste).");
            System.out.println("Press Ctrl+C to exit.\n");

            boolean inPaste = false;
            StringBuilder pasteBuffer = new StringBuilder();

            while (true) {
                String token = AnsiParser.parse(() -> terminal.read(1000));
                if (token == null) {
                    break; // EOF
                } else if (token.isEmpty()) {
                    continue; // timeout
                } else if (token.equals("\u0003")) { // Ctrl+C
                    break;
                } else if (token.equals(PASTE_START)) {
                    inPaste = true;
                    pasteBuffer.setLength(0);
                    System.out.println("[PASTE START]");
                } else if (token.equals(PASTE_END)) {
                    inPaste = false;
                    System.out.print("\r\n[PASTE END]\r\n");
                    System.out.print("Pasted content (" + pasteBuffer.length() + " chars):\r\n");
                    System.out.print("---\r\n");
                    // Print with proper line endings for raw mode
                    String content = pasteBuffer.toString();
                    for (int i = 0; i < content.length(); i++) {
                        char ch = content.charAt(i);
                        if (ch == '\n') {
                            System.out.print("\r\n");
                        } else if (ch == '\r') {
                            // Skip standalone CR, will be handled with LF
                            if (i + 1 < content.length() && content.charAt(i + 1) != '\n') {
                                System.out.print("\r\n");
                            }
                        } else {
                            System.out.print(ch);
                        }
                    }
                    System.out.print("\r\n---\r\n\r\n");
                } else if (inPaste) {
                    pasteBuffer.append(token);
                } else {
                    // Regular typed input
                    if (token.length() == 1) {
                        char ch = token.charAt(0);
                        if (ch >= 32 && ch < 127) {
                            System.out.println("Typed: '" + ch + "' (code: " + (int) ch + ")");
                        } else {
                            System.out.println("Typed: code " + (int) ch);
                        }
                    } else {
                        System.out.println("Sequence: " + escapeString(token));
                    }
                }
            }

            terminal.write(DISABLE_BRACKETED_PASTE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\u001b') {
                sb.append("\\e");
            } else if (ch >= 32 && ch < 127) {
                sb.append(ch);
            } else {
                sb.append(String.format("\\x%02x", (int) ch));
            }
        }
        return sb.toString();
    }
}
