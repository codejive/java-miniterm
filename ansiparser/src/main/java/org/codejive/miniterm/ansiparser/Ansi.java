package org.codejive.miniterm.ansiparser;

import java.io.IOException;

public class Ansi {
    public static final char ESC = 0x1B;
    public static final String CSI = ESC + "["; // Control Sequence Introducer
    public static final String OSC = ESC + "]"; // Operating System Command

    public static final String OSC_BEL = "\007";
    public static final String OSC_ST = ESC + "\\";

    public static final String DEC_SET = "h";
    public static final String DEC_RST = "l";

    // Positive mode numbers are official ANSI modes
    public static final int MODE_ECHO = 12;

    // Negative mode numbers are private DEC modes
    public static final int MODE_MOUSE_X10 = -9;
    public static final int MODE_SHOWCURSOR = -25;
    public static final int MODE_ALTBUFFER = -47;
    public static final int MODE_MOUSE_BUTTON = -1000;
    public static final int MODE_MOUSE_DRAG = -1002;
    public static final int MODE_MOUSE_FULL = -1003;
    public static final int MODE_MOUSE_UTF8 = -1005; // Legacy, use MODE_MOUSE_SGR instead
    public static final int MODE_MOUSE_SGR = -1006;
    public static final int MODE_AUTOWRAP = -1007;
    public static final int MODE_AUTOREPEAT = -1008;
    public static final int MODE_MOUSE_URXVT = -1015;
    public static final int MODE_MOUSE_SGR_PIXELS = -1016;
    public static final int MODE_ALTBUFFER2 = -1047;
    public static final int MODE_SAVECURSOR = -1048;
    public static final int MODE_ALTBUFFER3 = -1049;
    public static final int MODE_BRACKETEDPASTE = -2004;
    public static final int MODE_SYNCDRAW = -2026;
    public static final int MODE_UNICODE = -2027;
    public static final int MODE_SYSTEMTHEME = -2031;

    public static String csi(String post, int... params) {
        return csi((String) null, post, params);
    }

    public static String csi(String pre, String post, int... params) {
        try {
            return csi(new StringBuilder(), pre, post, params).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Appendable csi(Appendable appendable, String post, int... params)
            throws IOException {
        return csi(appendable, null, post, params);
    }

    public static Appendable csi(Appendable appendable, String pre, String post, int... params)
            throws IOException {
        appendable.append(CSI);
        if (pre != null) {
            appendable.append(pre);
        }
        for (int i = 0; i < params.length; i++) {
            if (i > 0) appendable.append(";");
            appendable.append(Integer.toString(params[i]));
        }
        if (post != null) {
            appendable.append(post);
        }
        return appendable;
    }

    public static String modeQuery(int mode) {
        if (mode >= 0) {
            return csi("", "$p", mode);
        } else {
            return csi("?", "$p", -mode);
        }
    }

    public static Appendable modeQuery(Appendable appendable, int mode) throws IOException {
        if (mode >= 0) {
            return csi(appendable, "", "$p", mode);
        } else {
            return csi(appendable, "?", "$p", -mode);
        }
    }

    public static String modeSet(int mode, boolean enable) {
        String action = enable ? DEC_SET : DEC_RST;
        return (mode >= 0) ? csi("", action, mode) : csi("?", action, -mode);
    }

    public static Appendable modeSet(Appendable appendable, int mode, boolean enable)
            throws IOException {
        String action = enable ? DEC_SET : DEC_RST;
        return (mode >= 0)
                ? csi(appendable, "", action, mode)
                : csi(appendable, "?", action, -mode);
    }

    public static boolean isModeQueryResult(String sequence) {
        return sequence.startsWith(CSI) && sequence.endsWith("$y");
    }
}
