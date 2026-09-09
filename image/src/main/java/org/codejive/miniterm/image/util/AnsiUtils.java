package org.codejive.miniterm.image.util;

import static org.codejive.miniterm.ansiparser.Ansi.CSI;

public class AnsiUtils {

    public static final CharSequence STYLE_RESET = CSI + "0m"; // Reset all attributes

    public static String rgbFg(int fgR, int fgG, int fgB) {
        return CSI + "38;2;" + fgR + ";" + fgG + ";" + fgB + "m";
    }

    public static String rgbBg(int bgR, int bgG, int bgB) {
        return CSI + "48;2;" + bgR + ";" + bgG + ";" + bgB + "m";
    }
}
