package org.codejive.miniterm.termcap;

import static org.codejive.miniterm.ansiparser.Ansi.CSI;
import static org.codejive.miniterm.ansiparser.Ansi.OSC;
import static org.codejive.miniterm.ansiparser.Ansi.OSC_BEL;
import static org.codejive.miniterm.ansiparser.Ansi.OSC_ST;

/**
 * Immutable snapshot of detected terminal capabilities.
 *
 * <h2>Detection layers</h2>
 *
 * <p>{@link #detect()} applies four layers in order; later layers win:
 *
 * <ol>
 *   <li><b>{@code TERM}</b> environment variable — baseline capabilities for the terminal type
 *   <li><b>Supplemental env vars</b> — {@code COLORTERM}, {@code TERM_PROGRAM}, {@code
 *       VTE_VERSION}, {@code WT_SESSION}, {@code ConEmuANSI}, {@code TMUX}
 *   <li><b>Terminfo binary</b> (Unix only) — reads {@code max_colors} from the compiled terminfo
 *       entry for {@code $TERM}
 *   <li><b>Live probing</b> — opt-in via {@link TermProber#probe(Appendable,
 *       org.codejive.miniterm.ansiparser.IntReader)}; refines caps using DA1/DA2 responses
 * </ol>
 *
 * <h2>SSH sessions</h2>
 *
 * <p>{@code TERM} is always forwarded through SSH, so Layer 1 and 3 work normally on the remote
 * host. Layer 2 env vars ({@code COLORTERM}, {@code TERM_PROGRAM}, etc.) are <em>not</em> forwarded
 * by default — configure {@code SendEnv}/{@code AcceptEnv} in your SSH config, or use {@link
 * TermProber#probe} which queries the terminal directly and works over any PTY chain.
 *
 * <h2>Minimal usage</h2>
 *
 * <pre>{@code
 * TermCaps caps = TermCaps.detect();
 * if (caps.colors() >= 256) renderRichColors();
 * if (caps.bracketedPaste()) enableBracketedPaste(terminal);
 * }</pre>
 *
 * <h2>Manual override</h2>
 *
 * <pre>{@code
 * TermCaps caps = TermCaps.builder(TermCaps.detect()).colors(0).build();
 * }</pre>
 */
public final class TermCaps {

    /** Number of supported colours: 0, 8, 16, 256, or {@code 16_777_216} (true-colour). */
    private final int colors;

    /** Whether the terminal supports the alternate screen buffer ({@code smcup}/{@code rmcup}). */
    private final boolean altScreen;

    /** Whether the terminal supports xterm mouse tracking (any protocol variant). */
    private final boolean mouse;

    /** Whether the terminal supports bracketed paste (DEC mode 2004). */
    private final boolean bracketedPaste;

    /** Whether the terminal supports focus-in/focus-out tracking (DEC mode 1004). */
    private final boolean focusTracking;

    /** Whether the terminal supports synchronized output (DEC mode 2026). */
    private final boolean synchronizedOutput;

    /** Whether the terminal supports OSC 8 hyperlinks. */
    private final boolean hyperlinks;

    /** Whether the terminal title can be set via OSC 0/2. */
    private final boolean settableTitle;

    /** Whether the terminal processes UTF-8 output correctly. */
    private final boolean unicode;

    /** Whether the terminal supports italic text ({@code sitm}/{@code ritm}). */
    private final boolean italic;

    /** Whether the terminal supports strikethrough text ({@code smxx}/{@code rmxx}). */
    private final boolean strikethrough;

    /** Whether the terminal supports overline text. */
    private final boolean overline;

    /** Whether the terminal supports undercurl (curly underline) text decoration. */
    private final boolean undercurl;

    /** Whether the terminal supports the extended underline specification (styles and colors). */
    private final boolean extendedUnderline;

    /** Whether the terminal supports sixel bitmap graphics (DCS sequences). */
    private final boolean sixel;

    /** Whether the terminal supports the Kitty terminal graphics protocol (APC-based). */
    private final boolean kittyGraphics;

    /** Whether the terminal supports the iTerm2 inline image protocol (OSC 1337). */
    private final boolean iterm2Images;

    /** Whether the terminal supports the Kitty keyboard protocol (progressive enhancement). */
    private final boolean kittyKeyboard;

    private TermCaps(Builder b) {
        this.colors = b.colors;
        this.altScreen = b.altScreen;
        this.mouse = b.mouse;
        this.bracketedPaste = b.bracketedPaste;
        this.focusTracking = b.focusTracking;
        this.synchronizedOutput = b.synchronizedOutput;
        this.hyperlinks = b.hyperlinks;
        this.settableTitle = b.settableTitle;
        this.unicode = b.unicode;
        this.italic = b.italic;
        this.strikethrough = b.strikethrough;
        this.overline = b.overline;
        this.undercurl = b.undercurl;
        this.extendedUnderline = b.extendedUnderline;
        this.sixel = b.sixel;
        this.kittyGraphics = b.kittyGraphics;
        this.iterm2Images = b.iterm2Images;
        this.kittyKeyboard = b.kittyKeyboard;
    }

    // ── Capability getters ────────────────────────────────────────────────

    /** Number of supported colours: 0, 8, 16, 256, or {@code 16_777_216} (true-colour). */
    public int colors() {
        return colors;
    }

    /** Whether the terminal supports the alternate screen buffer. */
    public boolean altScreen() {
        return altScreen;
    }

    /** Whether the terminal supports xterm mouse tracking (any protocol variant). */
    public boolean mouse() {
        return mouse;
    }

    /** Whether the terminal supports bracketed paste (DEC mode 2004). */
    public boolean bracketedPaste() {
        return bracketedPaste;
    }

    /** Whether the terminal supports focus-in/focus-out tracking (DEC mode 1004). */
    public boolean focusTracking() {
        return focusTracking;
    }

    /** Whether the terminal supports synchronized output (DEC mode 2026). */
    public boolean synchronizedOutput() {
        return synchronizedOutput;
    }

    /** Whether the terminal supports OSC 8 hyperlinks. */
    public boolean hyperlinks() {
        return hyperlinks;
    }

    /** Whether the terminal title can be set via OSC 0/2. */
    public boolean settableTitle() {
        return settableTitle;
    }

    /** Whether the terminal processes UTF-8 output correctly. */
    public boolean unicode() {
        return unicode;
    }

    /** Whether the terminal supports italic text. */
    public boolean italic() {
        return italic;
    }

    /** Whether the terminal supports strikethrough text. */
    public boolean strikethrough() {
        return strikethrough;
    }

    /** Whether the terminal supports overline text. */
    public boolean overline() {
        return overline;
    }

    /** Whether the terminal supports undercurl (curly underline) text decoration. */
    public boolean undercurl() {
        return undercurl;
    }

    /** Whether the terminal supports the extended underline specification (styles and colors). */
    public boolean extendedUnderline() {
        return extendedUnderline;
    }

    /** Whether the terminal supports sixel bitmap graphics (DCS sequences). */
    public boolean sixel() {
        return sixel;
    }

    /** Whether the terminal supports the Kitty terminal graphics protocol (APC-based). */
    public boolean kittyGraphics() {
        return kittyGraphics;
    }

    /** Whether the terminal supports the iTerm2 inline image protocol (OSC 1337). */
    public boolean iterm2Images() {
        return iterm2Images;
    }

    /** Whether the terminal supports the Kitty keyboard protocol (progressive enhancement). */
    public boolean kittyKeyboard() {
        return kittyKeyboard;
    }

    // ── Terminal control helpers ──────────────────────────────────────────

    /**
     * Enters the alternate screen buffer (DEC private mode 1049).
     *
     * <p>Saves the cursor position, switches to the alternate screen, and clears it. Pair with
     * {@link #disableAltScreen(Appendable)} before exit to restore the original screen contents.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableAltScreen(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?1049h");
    }

    /**
     * Exits the alternate screen buffer (DEC private mode 1049).
     *
     * <p>Restores the previous screen contents and cursor position.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void disableAltScreen(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?1049l");
    }

    /**
     * Enables bracketed paste mode (DEC private mode 2004).
     *
     * <p>When active, pasted text is wrapped with {@code ESC[200~} / {@code ESC[201~} markers so
     * the application can distinguish typed input from clipboard content.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableBracketedPaste(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?2004h");
    }

    /**
     * Disables bracketed paste mode (DEC private mode 2004).
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void disableBracketedPaste(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?2004l");
    }

    /**
     * Returns {@code true} if {@code sequence} is the bracketed-paste <em>start</em> marker ({@code
     * ESC[200~}).
     *
     * @param sequence input sequence to test
     * @return {@code true} when the sequence signals the beginning of a pasted region
     */
    public static boolean isPasteStart(String sequence) {
        return (CSI + "200~").equals(sequence);
    }

    /**
     * Returns {@code true} if {@code sequence} is the bracketed-paste <em>end</em> marker ({@code
     * ESC[201~}).
     *
     * @param sequence input sequence to test
     * @return {@code true} when the sequence signals the end of a pasted region
     */
    public static boolean isPasteEnd(String sequence) {
        return (CSI + "201~").equals(sequence);
    }

    /**
     * Enables focus-in / focus-out tracking (DEC private mode 1004).
     *
     * <p>The terminal sends {@code ESC[I} when the window gains focus and {@code ESC[O} when it
     * loses focus.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableFocusTracking(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?1004h");
    }

    /**
     * Disables focus-in / focus-out tracking (DEC private mode 1004).
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void disableFocusTracking(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?1004l");
    }

    /**
     * Returns {@code true} if {@code sequence} is the focus-in event ({@code ESC[I}).
     *
     * @param sequence input sequence to test
     * @return {@code true} when the sequence signals the terminal window has gained focus
     */
    public static boolean isFocusIn(String sequence) {
        return (CSI + "I").equals(sequence);
    }

    /**
     * Returns {@code true} if {@code sequence} is the focus-out event ({@code ESC[O}).
     *
     * @param sequence input sequence to test
     * @return {@code true} when the sequence signals the terminal window has lost focus
     */
    public static boolean isFocusOut(String sequence) {
        return (CSI + "O").equals(sequence);
    }

    /**
     * Begins a synchronized-output frame (DEC private mode 2026).
     *
     * <p>Wrap one full frame's worth of writes between {@code beginSynchronizedOutput} and {@link
     * #endSynchronizedOutput(Appendable)} to let the terminal batch the updates and avoid visual
     * tearing.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void beginSynchronizedOutput(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?2026h");
    }

    /**
     * Ends a synchronized-output frame (DEC private mode 2026).
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void endSynchronizedOutput(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?2026l");
    }

    /**
     * Sets the terminal window title (OSC 0 — icon name + window title).
     *
     * <p>The {@code title} string must not contain a {@code BEL} character ({@code \007}).
     *
     * @param out terminal output
     * @param title new title text
     * @throws java.io.IOException if writing fails
     */
    public static void setTitle(Appendable out, String title) throws java.io.IOException {
        out.append(OSC).append("0;").append(title).append(OSC_BEL);
    }

    /**
     * Opens an OSC 8 hyperlink.
     *
     * <p>All text appended after this call (until {@link #closeHyperlink(Appendable)}) will be
     * rendered as a clickable link to {@code uri} in terminals that support OSC 8.
     *
     * <pre>{@code
     * TermCaps.openHyperlink(out, "https://example.com");
     * out.append("click here");
     * TermCaps.closeHyperlink(out);
     * }</pre>
     *
     * @param out terminal output
     * @param uri the link target URI
     * @throws java.io.IOException if writing fails
     */
    public static void openHyperlink(Appendable out, String uri) throws java.io.IOException {
        out.append(OSC).append("8;;").append(uri).append(OSC_ST);
    }

    /**
     * Closes an OSC 8 hyperlink opened with {@link #openHyperlink(Appendable, String)}.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void closeHyperlink(Appendable out) throws java.io.IOException {
        out.append(OSC).append("8;;").append(OSC_ST);
    }

    /**
     * Pushes the Kitty keyboard protocol flags onto the stack and activates progressive
     * enhancement.
     *
     * <p>The Kitty keyboard protocol provides enhanced keyboard input with support for:
     *
     * <ul>
     *   <li>Disambiguating key events (press vs release, repeat)
     *   <li>Reporting all modifier keys (Shift, Ctrl, Alt, Super)
     *   <li>Encoding Unicode characters directly
     *   <li>Reporting alternate key codes
     * </ul>
     *
     * <p>Flags (bitwise OR):
     *
     * <ul>
     *   <li>1 — Disambiguate escape codes
     *   <li>2 — Report event types (press, release, repeat)
     *   <li>4 — Report alternate keys
     *   <li>8 — Report all keys as escape codes
     *   <li>16 — Report associated text
     * </ul>
     *
     * <p>Common flag combinations:
     *
     * <ul>
     *   <li>{@code 1} — Basic enhancement (disambiguate only)
     *   <li>{@code 3} — Enhanced (disambiguate + event types)
     *   <li>{@code 7} — Full (disambiguate + event types + alternate keys)
     * </ul>
     *
     * <p>Use {@link #popKittyKeyboard(Appendable)} to restore the previous state.
     *
     * @param out terminal output
     * @param flags bitwise OR of protocol flags (typically 1, 3, or 7)
     * @throws java.io.IOException if writing fails
     * @see <a href="https://sw.kovidgoyal.net/kitty/keyboard-protocol/">Kitty keyboard protocol</a>
     */
    public static void pushKittyKeyboard(Appendable out, int flags) throws java.io.IOException {
        out.append(CSI).append(">").append(String.valueOf(flags)).append("u");
    }

    /**
     * Pops the Kitty keyboard protocol flags from the stack, restoring the previous state.
     *
     * <p>This should be called before program exit to restore the terminal's original keyboard
     * handling mode.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void popKittyKeyboard(Appendable out) throws java.io.IOException {
        out.append(CSI).append("<u");
    }

    /**
     * Queries the terminal for Kitty keyboard protocol support.
     *
     * <p>The terminal will respond with {@code CSI ? <flags> u} if supported, where {@code flags}
     * indicates the currently active protocol level. If the terminal does not support the protocol,
     * no response is sent.
     *
     * <p>This query should be sent in raw mode, and the response read with a timeout (typically
     * 100-500ms).
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void queryKittyKeyboard(Appendable out) throws java.io.IOException {
        out.append(CSI).append("?u");
    }

    /**
     * Returns {@code true} if {@code sequence} is a Kitty keyboard protocol query response.
     *
     * <p>The expected form is {@code CSI ? <flags> u}.
     *
     * @param sequence input sequence to test
     * @return {@code true} when the sequence is a Kitty keyboard protocol response
     */
    public static boolean isKittyKeyboardResponse(String sequence) {
        return sequence != null && sequence.startsWith(CSI + "?") && sequence.endsWith("u");
    }

    /**
     * Enables undercurl (curly underline) text decoration.
     *
     * <p>Undercurl renders text with a wavy/curly underline, commonly used in editors to indicate
     * spelling or grammar errors. This uses the SGR parameter {@code 4:3}.
     *
     * <p>Not all terminals support undercurl. Check {@link #undercurl()} before using.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableUndercurl(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:3m");
    }

    /**
     * Disables undercurl (curly underline) text decoration.
     *
     * <p>This resets the underline style to none using SGR parameter {@code 4:0}.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void disableUndercurl(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:0m");
    }

    /**
     * Enables single underline text decoration.
     *
     * <p>This is the standard underline style using SGR parameter {@code 4:1}.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableSingleUnderline(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:1m");
    }

    /**
     * Enables double underline text decoration.
     *
     * <p>Double underline renders text with two parallel underlines using SGR parameter {@code
     * 4:2}.
     *
     * <p>Not all terminals support double underline. Check {@link #extendedUnderline()} before
     * using.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableDoubleUnderline(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:2m");
    }

    /**
     * Enables dotted underline text decoration.
     *
     * <p>Dotted underline renders text with a dotted line underneath using SGR parameter {@code
     * 4:4}.
     *
     * <p>Not all terminals support dotted underline. Check {@link #extendedUnderline()} before
     * using.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableDottedUnderline(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:4m");
    }

    /**
     * Enables dashed underline text decoration.
     *
     * <p>Dashed underline renders text with a dashed line underneath using SGR parameter {@code
     * 4:5}.
     *
     * <p>Not all terminals support dashed underline. Check {@link #extendedUnderline()} before
     * using.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void enableDashedUnderline(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:5m");
    }

    /**
     * Disables all underline text decorations.
     *
     * <p>This resets the underline style to none using SGR parameter {@code 4:0}.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void disableUnderline(Appendable out) throws java.io.IOException {
        out.append(CSI).append("4:0m");
    }

    /**
     * Sets the underline color using RGB values.
     *
     * <p>This uses the extended underline specification's color support via SGR parameter {@code
     * 58:2:r:g:b}.
     *
     * <p>Not all terminals support underline colors. Check {@link #extendedUnderline()} before
     * using.
     *
     * @param out terminal output
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @throws java.io.IOException if writing fails
     */
    public static void setUnderlineColor(Appendable out, int r, int g, int b)
            throws java.io.IOException {
        out.append(CSI)
                .append("58:2:")
                .append(String.valueOf(r))
                .append(":")
                .append(String.valueOf(g))
                .append(":")
                .append(String.valueOf(b))
                .append("m");
    }

    /**
     * Sets the underline color using a 256-color palette index.
     *
     * <p>This uses the extended underline specification's color support via SGR parameter {@code
     * 58:5:n}.
     *
     * <p>Not all terminals support underline colors. Check {@link #extendedUnderline()} before
     * using.
     *
     * @param out terminal output
     * @param index color index (0-255)
     * @throws java.io.IOException if writing fails
     */
    public static void setUnderlineColor(Appendable out, int index) throws java.io.IOException {
        out.append(CSI).append("58:5:").append(String.valueOf(index)).append("m");
    }

    /**
     * Resets the underline color to the default.
     *
     * <p>This uses SGR parameter {@code 59}.
     *
     * @param out terminal output
     * @throws java.io.IOException if writing fails
     */
    public static void resetUnderlineColor(Appendable out) throws java.io.IOException {
        out.append(CSI).append("59m");
    }

    // ── Detection ─────────────────────────────────────────────────────────

    /**
     * Detects terminal capabilities without performing any terminal I/O.
     *
     * <p>Applies three layers in order:
     *
     * <ol>
     *   <li>{@code TERM} environment variable
     *   <li>Supplemental environment variables ({@code COLORTERM}, {@code TERM_PROGRAM}, etc.)
     *   <li>Terminfo binary {@code max_colors} field (Unix only)
     * </ol>
     *
     * <p>This method is safe to call at any time, including before raw mode is enabled or before a
     * {@code Terminal} is opened.
     *
     * @return detected capabilities
     */
    public static TermCaps detect() {
        Builder b = new Builder();
        applyTerm(b, System.getenv("TERM"));
        applyEnvVars(b);
        applyTerminfo(b, System.getenv("TERM"));
        return b.build();
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Returns a new {@link Builder} with all capabilities set to their defaults (off/0). */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link Builder} pre-populated from {@code base}.
     *
     * <p>Use this to apply targeted overrides on top of a detected baseline:
     *
     * <pre>{@code
     * TermCaps caps = TermCaps.builder(TermCaps.detect()).colors(0).build();
     * }</pre>
     */
    public static Builder builder(TermCaps base) {
        return new Builder(base);
    }

    // ── Layer 1: TERM env var ─────────────────────────────────────────────

    static void applyTerm(Builder b, String term) {
        if (term == null || term.isEmpty()) return;

        // Baseline by well-known TERM values
        if ("dumb".equals(term)) {
            // no capabilities at all
            return;
        } else if (term.startsWith("vt1") || term.startsWith("vt2")) {
            // vt100, vt102, vt220 etc. — no colour, no mouse
            return;
        } else if ("ansi".equals(term)) {
            b.colors(8);
            return;
        } else if (term.startsWith("screen")) {
            b.colors(8).altScreen(true);
        } else if (term.startsWith("tmux")) {
            b.colors(8).altScreen(true);
        } else if (term.startsWith("rxvt")) {
            b.colors(8).altScreen(true).mouse(true).unicode(true);
        } else if ("xterm-kitty".equals(term)) {
            // Ghostty and other terminals that set TERM=xterm-kitty support the Kitty graphics
            // protocol
            b.colors(16_777_216)
                    .altScreen(true)
                    .mouse(true)
                    .settableTitle(true)
                    .unicode(true)
                    .kittyGraphics(true);
        } else if (term.startsWith("xterm") || term.startsWith("vte")) {
            b.colors(8).altScreen(true).mouse(true).settableTitle(true).unicode(true);
        } else if (term.startsWith("linux")) {
            b.colors(8).altScreen(true).unicode(true);
        } else if (term.startsWith("konsole")) {
            b.colors(8).altScreen(true).mouse(true).settableTitle(true).unicode(true);
        } else if (term.startsWith("foot")) {
            // foot (Wayland) 1.2.0+ supports sixel
            b.colors(256)
                    .altScreen(true)
                    .mouse(true)
                    .bracketedPaste(true)
                    .focusTracking(true)
                    .unicode(true)
                    .sixel(true);
        } else if (term.startsWith("mlterm")) {
            // mlterm 3.1.9+ supports sixel
            b.colors(256).altScreen(true).mouse(true).unicode(true).sixel(true);
        } else if (term.startsWith("mintty")) {
            // mintty 2.6.0+ supports sixel and the iTerm2 inline image protocol
            b.colors(256)
                    .altScreen(true)
                    .mouse(true)
                    .bracketedPaste(true)
                    .unicode(true)
                    .sixel(true)
                    .iterm2Images(true);
        } else if (term.startsWith("contour")) {
            // Contour supports sixel natively
            b.colors(16_777_216)
                    .altScreen(true)
                    .mouse(true)
                    .bracketedPaste(true)
                    .focusTracking(true)
                    .unicode(true)
                    .sixel(true);
        } else {
            // Unknown terminal — assume at least 8 colours
            b.colors(8);
        }

        // Suffix upgrades — apply on top of whatever baseline was set above
        if (term.endsWith("-truecolor") || term.endsWith("-direct")) {
            b.colors(16_777_216)
                    .bracketedPaste(true)
                    .italic(true)
                    .strikethrough(true)
                    .overline(true);
        } else if (term.endsWith("-256color")) {
            b.colors(256).bracketedPaste(true).italic(true).strikethrough(true);
        }
    }

    // ── Layer 2: supplemental env vars ───────────────────────────────────

    /** Package-private functional interface for injecting env vars in tests. */
    interface EnvSource {
        String get(String name);
    }

    static void applyEnvVars(Builder b) {
        applyEnvVars(b, System::getenv);
    }

    static void applyEnvVars(Builder b, EnvSource env) {
        // COLORTERM — explicit true-colour declaration
        String colorterm = env.get("COLORTERM");
        if ("truecolor".equals(colorterm) || "24bit".equals(colorterm)) {
            b.colors(16_777_216);
        }

        // WT_SESSION — Windows Terminal
        if (env.get("WT_SESSION") != null) {
            b.colors(16_777_216)
                    .altScreen(true)
                    .mouse(true)
                    .bracketedPaste(true)
                    .focusTracking(true)
                    .synchronizedOutput(true)
                    .hyperlinks(true)
                    .settableTitle(true)
                    .unicode(true)
                    .italic(true)
                    .strikethrough(true)
                    .overline(true)
                    .undercurl(true) // Windows Terminal 1.19+
                    .extendedUnderline(true) // Windows Terminal 1.19+
                    .sixel(true); // Windows Terminal 1.22+ (August 2024)
            return;
        }

        // ConEmuANSI — ConEmu / Cmder on Windows
        if ("ON".equalsIgnoreCase(env.get("ConEmuANSI"))) {
            b.colors(256).settableTitle(true).unicode(true);
        }

        // VTE_VERSION — GNOME Terminal, Tilix, Xfce Terminal, etc.
        String vte = env.get("VTE_VERSION");
        if (vte != null && !vte.isEmpty()) {
            if (b.colors < 256) b.colors(256);
            b.bracketedPaste(true).hyperlinks(true).italic(true).focusTracking(true);
        }

        // TERM_PROGRAM — self-reported by several terminals
        String termProgram = env.get("TERM_PROGRAM");
        String termProgramVersion = env.get("TERM_PROGRAM_VERSION");
        if (termProgram != null) {
            switch (termProgram) {
                case "iTerm.app":
                    b.colors(16_777_216)
                            .altScreen(true)
                            .mouse(true)
                            .bracketedPaste(true)
                            .focusTracking(true)
                            .hyperlinks(true)
                            .settableTitle(true)
                            .unicode(true)
                            .italic(true)
                            .strikethrough(true)
                            .overline(true)
                            .iterm2Images(true);
                    // iTerm2 3.3.0+ supports sixel
                    if (compareVersion(termProgramVersion, "3.3.0") >= 0) {
                        b.sixel(true);
                    }
                    // iTerm2 3.5.0+ supports Kitty graphics protocol
                    if (compareVersion(termProgramVersion, "3.5.0") >= 0) {
                        b.kittyGraphics(true);
                    }
                    // iTerm2 3.5.0+ supports Kitty keyboard protocol
                    if (compareVersion(termProgramVersion, "3.5.0") >= 0) {
                        b.kittyKeyboard(true);
                    }
                    // iTerm2 3.4.0+ supports undercurl and extended underline
                    if (compareVersion(termProgramVersion, "3.4.0") >= 0) {
                        b.undercurl(true).extendedUnderline(true);
                    }
                    break;
                case "WezTerm":
                    b.colors(16_777_216)
                            .altScreen(true)
                            .mouse(true)
                            .bracketedPaste(true)
                            .focusTracking(true)
                            .synchronizedOutput(true)
                            .hyperlinks(true)
                            .settableTitle(true)
                            .unicode(true)
                            .italic(true)
                            .strikethrough(true)
                            .overline(true)
                            .sixel(true)
                            .kittyGraphics(true)
                            .iterm2Images(true)
                            .undercurl(true)
                            .extendedUnderline(true);
                    // WezTerm 20220101+ supports Kitty keyboard protocol
                    if (compareVersion(termProgramVersion, "20220101") >= 0) {
                        b.kittyKeyboard(true);
                    }
                    break;
                case "kitty":
                    b.colors(16_777_216)
                            .altScreen(true)
                            .mouse(true)
                            .bracketedPaste(true)
                            .focusTracking(true)
                            .hyperlinks(true)
                            .settableTitle(true)
                            .unicode(true)
                            .italic(true)
                            .strikethrough(true)
                            .overline(true)
                            .kittyGraphics(true) // kitty does not support sixel by design
                            .undercurl(true)
                            .extendedUnderline(true);
                    // Kitty 0.20.0+ supports Kitty keyboard protocol (it invented it)
                    if (compareVersion(termProgramVersion, "0.20.0") >= 0) {
                        b.kittyKeyboard(true);
                    }
                    break;
                case "Apple_Terminal":
                    if (b.colors < 256) b.colors(256);
                    b.settableTitle(true).unicode(true);
                    // Apple Terminal 2.10+ (macOS 10.15+) supports undercurl
                    if (compareVersion(termProgramVersion, "2.10") >= 0) {
                        b.undercurl(true);
                    }
                    break;
                case "Ghostty":
                case "ghostty":
                    b.colors(16_777_216)
                            .altScreen(true)
                            .mouse(true)
                            .bracketedPaste(true)
                            .focusTracking(true)
                            .synchronizedOutput(true)
                            .hyperlinks(true)
                            .settableTitle(true)
                            .unicode(true)
                            .italic(true)
                            .strikethrough(true)
                            .overline(true)
                            .kittyGraphics(true)
                            .undercurl(true);
                    // Ghostty 1.0.0+ supports Kitty keyboard protocol
                    if (compareVersion(termProgramVersion, "1.0.0") >= 0) {
                        b.kittyKeyboard(true);
                    }
                    break;
                default:
                    break;
            }
        }

        // TMUX — inside a tmux session: mouse works through tmux, but cap ceiling
        // is determined by the outer terminal (already set by TERM/TERM_PROGRAM above).
        // We ensure mouse is reported as available since tmux passes it through.
        if (env.get("TMUX") != null) {
            b.mouse(true);
        }

        // KONSOLE_VERSION — KDE Konsole 22.04+ supports sixel and the Kitty graphics protocol.
        if (env.get("KONSOLE_VERSION") != null) {
            if (b.colors < 256) b.colors(256);
            b.sixel(true).kittyGraphics(true);
        }

        // ZELLIJ — Zellij multiplexer 0.31.0+ passes sixel through to the underlying terminal.
        if (env.get("ZELLIJ") != null) {
            b.sixel(true);
        }

        // Windows fallback: if no special env var fired but we are on Windows,
        // assume a VT-capable console host (Win10 1511+) with conservative caps.
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows") && b.colors == 0) {
            b.colors(256).altScreen(true).mouse(true).unicode(true);
        }
    }

    // ── Layer 3: terminfo binary ──────────────────────────────────────────

    // ── Version comparison helper ─────────────────────────────────────────

    /**
     * Compares two version strings in the format "major.minor.patch" or "YYYYMMDD".
     *
     * <p>Returns:
     *
     * <ul>
     *   <li>negative if {@code version} is less than {@code reference}
     *   <li>zero if {@code version} equals {@code reference}
     *   <li>positive if {@code version} is greater than {@code reference}
     *   <li>-1 if {@code version} is null or cannot be parsed
     * </ul>
     *
     * <p>Supports:
     *
     * <ul>
     *   <li>Semantic versioning: "1.2.3", "0.20.0"
     *   <li>Date-based versioning: "20220101" (WezTerm style)
     *   <li>Two-part versions: "2.10" (Apple Terminal style)
     * </ul>
     *
     * @param version the version string to compare (may be null)
     * @param reference the reference version string
     * @return comparison result
     */
    private static int compareVersion(String version, String reference) {
        if (version == null || version.isEmpty()) return -1;
        if (reference == null || reference.isEmpty()) return 1;

        // Try numeric comparison first (for date-based versions like "20220101")
        try {
            long v = Long.parseLong(version);
            long r = Long.parseLong(reference);
            return Long.compare(v, r);
        } catch (NumberFormatException e) {
            // Not purely numeric, fall through to component-wise comparison
        }

        // Split on dots and compare component-wise
        String[] vParts = version.split("\\.");
        String[] rParts = reference.split("\\.");
        int maxLen = Math.max(vParts.length, rParts.length);

        for (int i = 0; i < maxLen; i++) {
            int vNum = i < vParts.length ? parseVersionComponent(vParts[i]) : 0;
            int rNum = i < rParts.length ? parseVersionComponent(rParts[i]) : 0;
            if (vNum != rNum) {
                return Integer.compare(vNum, rNum);
            }
        }
        return 0;
    }

    private static int parseVersionComponent(String component) {
        if (component == null || component.isEmpty()) return 0;
        try {
            return Integer.parseInt(component);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void applyTerminfo(Builder b, String term) {
        if (term == null || term.isEmpty()) return;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) return; // terminfo not available on Windows

        int tiColors = TerminfoReader.readMaxColors(term);
        if (tiColors > b.colors) {
            b.colors(tiColors);
        }
    }

    // ── Builder class ─────────────────────────────────────────────────────

    /** Fluent builder for {@link TermCaps}. */
    public static final class Builder {
        private int colors;
        private boolean altScreen;
        private boolean mouse;
        private boolean bracketedPaste;
        private boolean focusTracking;
        private boolean synchronizedOutput;
        private boolean hyperlinks;
        private boolean settableTitle;
        private boolean unicode;
        private boolean italic;
        private boolean strikethrough;
        private boolean overline;
        private boolean undercurl;
        private boolean extendedUnderline;
        private boolean sixel;
        private boolean kittyGraphics;
        private boolean iterm2Images;
        private boolean kittyKeyboard;

        private Builder() {}

        private Builder(TermCaps base) {
            this.colors = base.colors;
            this.altScreen = base.altScreen;
            this.mouse = base.mouse;
            this.bracketedPaste = base.bracketedPaste;
            this.focusTracking = base.focusTracking;
            this.synchronizedOutput = base.synchronizedOutput;
            this.hyperlinks = base.hyperlinks;
            this.settableTitle = base.settableTitle;
            this.unicode = base.unicode;
            this.italic = base.italic;
            this.strikethrough = base.strikethrough;
            this.overline = base.overline;
            this.undercurl = base.undercurl;
            this.extendedUnderline = base.extendedUnderline;
            this.sixel = base.sixel;
            this.kittyGraphics = base.kittyGraphics;
            this.iterm2Images = base.iterm2Images;
            this.kittyKeyboard = base.kittyKeyboard;
        }

        /** Returns the currently set colors value. */
        public int colors() {
            return colors;
        }

        public Builder colors(int n) {
            this.colors = n;
            return this;
        }

        public Builder altScreen(boolean v) {
            this.altScreen = v;
            return this;
        }

        public Builder mouse(boolean v) {
            this.mouse = v;
            return this;
        }

        public Builder bracketedPaste(boolean v) {
            this.bracketedPaste = v;
            return this;
        }

        public Builder focusTracking(boolean v) {
            this.focusTracking = v;
            return this;
        }

        public Builder synchronizedOutput(boolean v) {
            this.synchronizedOutput = v;
            return this;
        }

        public Builder hyperlinks(boolean v) {
            this.hyperlinks = v;
            return this;
        }

        public Builder settableTitle(boolean v) {
            this.settableTitle = v;
            return this;
        }

        public Builder unicode(boolean v) {
            this.unicode = v;
            return this;
        }

        public Builder italic(boolean v) {
            this.italic = v;
            return this;
        }

        public Builder strikethrough(boolean v) {
            this.strikethrough = v;
            return this;
        }

        public Builder overline(boolean v) {
            this.overline = v;
            return this;
        }

        public Builder undercurl(boolean v) {
            this.undercurl = v;
            return this;
        }

        public Builder extendedUnderline(boolean v) {
            this.extendedUnderline = v;
            return this;
        }

        public Builder sixel(boolean v) {
            this.sixel = v;
            return this;
        }

        public Builder kittyGraphics(boolean v) {
            this.kittyGraphics = v;
            return this;
        }

        public Builder iterm2Images(boolean v) {
            this.iterm2Images = v;
            return this;
        }

        public Builder kittyKeyboard(boolean v) {
            this.kittyKeyboard = v;
            return this;
        }

        public TermCaps build() {
            return new TermCaps(this);
        }
    }
}
