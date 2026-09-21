package org.codejive.miniterm.colors;

import java.util.Objects;
import org.codejive.miniterm.ansiparser.Ansi;

public interface Color {

    public static final int COLOR_IDX_BLACK = 0;
    public static final int COLOR_IDX_RED = 1;
    public static final int COLOR_IDX_GREEN = 2;
    public static final int COLOR_IDX_YELLOW = 3;
    public static final int COLOR_IDX_BLUE = 4;
    public static final int COLOR_IDX_MAGENTA = 5;
    public static final int COLOR_IDX_CYAN = 6;
    public static final int COLOR_IDX_WHITE = 7;

    public static final int STYLE_FOREGROUND = 38;
    public static final int STYLE_DEFAULT_FOREGROUND = 39;
    public static final int STYLE_BACKGROUND = 48;
    public static final int STYLE_DEFAULT_BACKGROUND = 49;
    public static final int STYLE_RGB = 2;
    public static final int STYLE_INDEXED = 5;

    Color DEFAULT = DefaultColor.instance();

    static BasicColor basic(int index) {
        return BasicColor.byIndex(index);
    }

    static BasicColor basic(int index, BasicColor.Intensity intensity) {
        return BasicColor.byIndex(index, intensity);
    }

    static IndexedColor indexed(int index) {
        return IndexedColor.of(index);
    }

    static RgbColor rgb(int r, int g, int b) {
        return RgbColor.of(r, g, b);
    }

    static RgbColor rgb8(int r, int g, int b) {
        return RgbColor.ofRgb8(r, g, b);
    }

    static String styled(String text, Color foreground, Color background) {
        Color fg = foreground != null ? foreground : DefaultColor.instance();
        Color bg = background != null ? background : DefaultColor.instance();
        return Ansi.style(fg.toForegroundStyle())
                + Ansi.style(bg.toBackgroundStyle())
                + text
                + Ansi.style(DefaultColor.instance().toForegroundStyle())
                + Ansi.style(DefaultColor.instance().toBackgroundStyle());
    }

    /**
     * Convert this color to ANSI escape code for setting the foreground color to the color
     * represented by this instance. This is NOT the full CSI sequence, to get the full sequence
     * pass the result to <code>Ansi.style()</code>.
     *
     * @return Embeddable ANSI escape code string
     */
    String toForegroundStyle();

    /**
     * Convert this color to ANSI escape code for setting the background color to the color
     * represented by this instance. This is NOT the full CSI sequence, to get the full sequence
     * pass the result to <code>Ansi.style()</code>.
     *
     * @return Embeddable ANSI escape code string
     */
    String toBackgroundStyle();

    class DefaultColor implements Color {
        private static final DefaultColor INSTANCE = new DefaultColor();

        private DefaultColor() {}

        protected static Color instance() {
            return INSTANCE;
        }

        @Override
        public String toForegroundStyle() {
            return String.valueOf(STYLE_DEFAULT_FOREGROUND);
        }

        @Override
        public String toBackgroundStyle() {
            return String.valueOf(STYLE_DEFAULT_BACKGROUND);
        }

        @Override
        public String toString() {
            return "default";
        }
    }

    class BasicColor implements Color {
        private final String name;
        private final int index;
        private final Intensity intensity;
        private final String fgStyle;
        private final String bgStyle;

        public enum Intensity {
            normal,
            dark,
            bright;
        }

        public static final BasicColor black =
                BasicColor.of("black", COLOR_IDX_BLACK, Intensity.normal);
        public static final BasicColor red = BasicColor.of("red", COLOR_IDX_RED, Intensity.normal);
        public static final BasicColor green =
                BasicColor.of("green", COLOR_IDX_GREEN, Intensity.normal);
        public static final BasicColor yellow =
                BasicColor.of("yellow", COLOR_IDX_YELLOW, Intensity.normal);
        public static final BasicColor blue =
                BasicColor.of("blue", COLOR_IDX_BLUE, Intensity.normal);
        public static final BasicColor magenta =
                BasicColor.of("magenta", COLOR_IDX_MAGENTA, Intensity.normal);
        public static final BasicColor cyan =
                BasicColor.of("cyan", COLOR_IDX_CYAN, Intensity.normal);
        public static final BasicColor white =
                BasicColor.of("white", COLOR_IDX_WHITE, Intensity.normal);

        public static final BasicColor darkBlack =
                BasicColor.of("black", COLOR_IDX_BLACK, Intensity.dark);
        public static final BasicColor darkRed =
                BasicColor.of("red", COLOR_IDX_RED, Intensity.dark);
        public static final BasicColor darkGreen =
                BasicColor.of("green", COLOR_IDX_GREEN, Intensity.dark);
        public static final BasicColor darkYellow =
                BasicColor.of("yellow", COLOR_IDX_YELLOW, Intensity.dark);
        public static final BasicColor darkBlue =
                BasicColor.of("blue", COLOR_IDX_BLUE, Intensity.dark);
        public static final BasicColor darkMagenta =
                BasicColor.of("magenta", COLOR_IDX_MAGENTA, Intensity.dark);
        public static final BasicColor darkCyan =
                BasicColor.of("cyan", COLOR_IDX_CYAN, Intensity.dark);
        public static final BasicColor darkWhite =
                BasicColor.of("white", COLOR_IDX_WHITE, Intensity.dark);

        public static final BasicColor brightBlack =
                BasicColor.of("black", COLOR_IDX_BLACK, Intensity.bright);
        public static final BasicColor brightRed =
                BasicColor.of("red", COLOR_IDX_RED, Intensity.bright);
        public static final BasicColor brightGreen =
                BasicColor.of("green", COLOR_IDX_GREEN, Intensity.bright);
        public static final BasicColor brightYellow =
                BasicColor.of("yellow", COLOR_IDX_YELLOW, Intensity.bright);
        public static final BasicColor brightBlue =
                BasicColor.of("blue", COLOR_IDX_BLUE, Intensity.bright);
        public static final BasicColor brightMagenta =
                BasicColor.of("magenta", COLOR_IDX_MAGENTA, Intensity.bright);
        public static final BasicColor brightCyan =
                BasicColor.of("cyan", COLOR_IDX_CYAN, Intensity.bright);
        public static final BasicColor brightWhite =
                BasicColor.of("white", COLOR_IDX_WHITE, Intensity.bright);

        public static final BasicColor[] normalColors = {
            black, red, green, yellow,
            blue, magenta, cyan, white
        };

        public static final BasicColor[] darkColors = {
            darkBlack, darkRed, darkGreen, darkYellow,
            darkBlue, darkMagenta, darkCyan, darkWhite
        };

        public static final BasicColor[] brightColors = {
            brightBlack, brightRed, brightGreen, brightYellow,
            brightBlue, brightMagenta, brightCyan, brightWhite
        };

        public static final int FOREGROUND_BASE = 30;
        public static final int FOREGROUND_DARK_BASE = 60;
        public static final int FOREGROUND_BRIGHT_BASE = 90;
        public static final int BACKGROUND_BASE = 40;
        public static final int BACKGROUND_DARK_BASE = 70;
        public static final int BACKGROUND_BRIGHT_BASE = 100;

        protected static BasicColor of(String name, int index, Intensity intensity) {
            return new BasicColor(name, index, intensity);
        }

        public static BasicColor byIndex(int index) {
            return normalColors[index];
        }

        public static BasicColor byIndex(int index, Intensity intensity) {
            switch (intensity) {
                case dark:
                    return darkColors[index];
                case bright:
                    return brightColors[index];
                default:
                    return normalColors[index];
            }
        }

        private BasicColor(String name, int index, Intensity intensity) {
            if (index < 0 || index > 7) {
                throw new IllegalArgumentException(
                        "Color index must be between 0 and 7, got: " + index);
            }
            this.name = name;
            this.index = index;
            this.intensity = intensity;
            this.fgStyle = foregroundStyle(index, intensity);
            this.bgStyle = backgroundStyle(index, intensity);
        }

        public String name() {
            return name;
        }

        public int index() {
            return index;
        }

        public Intensity intensity() {
            return intensity;
        }

        public BasicColor normal() {
            if (intensity == Intensity.normal) {
                return this;
            } else {
                return normalColors[index];
            }
        }

        public BasicColor dark() {
            if (intensity == Intensity.dark) {
                return this;
            } else {
                return darkColors[index];
            }
        }

        public BasicColor bright() {
            if (intensity == Intensity.bright) {
                return this;
            } else {
                return brightColors[index];
            }
        }

        public String toForegroundStyle() {
            return fgStyle;
        }

        public String toBackgroundStyle() {
            return bgStyle;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            BasicColor that = (BasicColor) o;
            return index == that.index && intensity == that.intensity;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, intensity);
        }

        @Override
        public String toString() {
            switch (intensity) {
                case normal:
                    return name;
                case dark:
                    return "dark " + name;
                case bright:
                    return "bright " + name;
                default:
                    throw new IllegalArgumentException("Unknown mode: " + intensity);
            }
        }

        public static String foregroundStyle(int index, Intensity intensity) {
            switch (intensity) {
                case normal:
                    return foregroundStyle(index);
                case dark:
                    return foregroundDarkStyle(index);
                case bright:
                    return foregroundBrightStyle(index);
                default:
                    throw new IllegalArgumentException("Unknown mode: " + intensity);
            }
        }

        public static String foregroundStyle(int index) {
            return String.valueOf(FOREGROUND_BASE + index);
        }

        public static String foregroundDarkStyle(int index) {
            return String.valueOf(FOREGROUND_DARK_BASE + index);
        }

        public static String foregroundBrightStyle(int index) {
            return String.valueOf(FOREGROUND_BRIGHT_BASE + index);
        }

        public static String backgroundStyle(int index, Intensity intensity) {
            switch (intensity) {
                case normal:
                    return backgroundStyle(index);
                case dark:
                    return backgroundDarkStyle(index);
                case bright:
                    return backgroundBrightStyle(index);
                default:
                    throw new IllegalArgumentException("Unknown mode: " + intensity);
            }
        }

        public static String backgroundStyle(int index) {
            return String.valueOf(BACKGROUND_BASE + index);
        }

        public static String backgroundDarkStyle(int index) {
            return String.valueOf(BACKGROUND_DARK_BASE + index);
        }

        public static String backgroundBrightStyle(int index) {
            return String.valueOf(BACKGROUND_BRIGHT_BASE + index);
        }
    }

    class IndexedColor implements Color {
        private final int index;
        private final String fgStyle;
        private final String bgStyle;

        public static IndexedColor of(int index) {
            return new IndexedColor(index);
        }

        protected IndexedColor(int index) {
            if (index < 0 || index > 255) {
                throw new IllegalArgumentException(
                        "Color index must be between 0 and 255, got: " + index);
            }
            this.index = index;
            this.fgStyle = foregroundIndexedStyle(index);
            this.bgStyle = backgroundIndexedStyle(index);
        }

        public int index() {
            return index;
        }

        @Override
        public String toForegroundStyle() {
            return fgStyle;
        }

        @Override
        public String toBackgroundStyle() {
            return bgStyle;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            IndexedColor that = (IndexedColor) o;
            return index == that.index;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(index);
        }

        @Override
        public String toString() {
            return "%" + index;
        }

        public static String foregroundIndexedStyle(int index) {
            return STYLE_FOREGROUND + ";" + STYLE_INDEXED + ";" + index;
        }

        public static String backgroundIndexedStyle(int index) {
            return STYLE_BACKGROUND + ";" + STYLE_INDEXED + ";" + index;
        }
    }

    class RgbColor implements Color {
        private final int r, g, b;
        private final String fgStyle;
        private final String bgStyle;
        private final String oscCode;

        /**
         * Creates a colour from 16-bit per channel values (0–65535 each).
         *
         * @param r red channel 0–65535
         * @param g green channel 0–65535
         * @param b blue channel 0–65535
         * @return new colour
         * @throws IllegalArgumentException if any component is out of range
         */
        public static RgbColor of(int r, int g, int b) {
            return new RgbColor(r, g, b);
        }

        /**
         * Creates a colour from 8-bit per channel values (0–255 each).
         *
         * <p>Each component is expanded to 16 bits by byte-replication ({@code 0xFF → 0xFFFF}).
         *
         * @param r red channel 0–255
         * @param g green channel 0–255
         * @param b blue channel 0–255
         * @return new colour
         * @throws IllegalArgumentException if any component is out of range
         */
        public static RgbColor ofRgb8(int r, int g, int b) {
            if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                throw new IllegalArgumentException(
                        "8-bit RGB component out of range [0..255]: " + r + "," + g + "," + b);
            }
            return new RgbColor((r << 8) | r, (g << 8) | g, (b << 8) | b);
        }

        protected RgbColor(int r, int g, int b) {
            if (r < 0 || r > 65535 || g < 0 || g > 65535 || b < 0 || b > 65535) {
                throw new IllegalArgumentException(
                        "RGB component out of range [0..65535]: " + r + "," + g + "," + b);
            }
            this.r = r;
            this.g = g;
            this.b = b;
            this.fgStyle = foregroundRgbStyle(r8(), g8(), b8());
            this.bgStyle = backgroundRgbStyle(r8(), g8(), b8());
            this.oscCode = oscCode(r, g, b);
        }

        /** Red channel as a 16-bit value (0–65535). */
        public int r() {
            return r;
        }

        /** Green channel as a 16-bit value (0–65535). */
        public int g() {
            return g;
        }

        /** Blue channel as a 16-bit value (0–65535). */
        public int b() {
            return b;
        }

        /** Red channel downsampled to 8 bits (0–255). */
        public int r8() {
            return r >> 8;
        }

        /** Green channel downsampled to 8 bits (0–255). */
        public int g8() {
            return g >> 8;
        }

        /** Blue channel downsampled to 8 bits (0–255). */
        public int b8() {
            return b >> 8;
        }

        /**
         * Parses an X11 {@code rgb:} colour specification.
         *
         * <p>Accepts 1–4 hex digits per channel. Each component is scaled to 16 bits:
         *
         * <ul>
         *   <li>4 digits — used as-is
         *   <li>2 digits — byte-replicated (e.g. {@code FF} → {@code FFFF})
         *   <li>1 digit — nibble-replicated (e.g. {@code F} → {@code FFFF})
         *   <li>3 digits — approximated as left-justified 12-bit value
         * </ul>
         *
         * @param s string starting with {@code rgb:} followed by three hex components separated by
         *     {@code /}
         * @return parsed colour, or {@code null} if {@code s} is null, does not start with {@code
         *     rgb:}, or contains invalid hex digits
         */
        public static RgbColor parse(String s) {
            if (s == null) return null;
            String t = s.trim();
            if (!t.startsWith("rgb:")) return null;
            String[] parts = t.substring(4).split("/", -1);
            if (parts.length != 3) return null;
            try {
                int r = normalizeHex(parts[0].trim());
                int g = normalizeHex(parts[1].trim());
                int b = normalizeHex(parts[2].trim());
                return new RgbColor(r, g, b);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * Returns the colour as a packed 24-bit RGB integer ({@code 0xRRGGBB}), using the 8-bit
         * downsampled values.
         *
         * @return packed RGB integer
         */
        public int toRgb8() {
            return (r8() << 16) | (g8() << 8) | b8();
        }

        @Override
        public String toForegroundStyle() {
            return fgStyle;
        }

        @Override
        public String toBackgroundStyle() {
            return bgStyle;
        }

        public String toOscCode() {
            return oscCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            RgbColor rgbColor = (RgbColor) o;
            return r == rgbColor.r && g == rgbColor.g && b == rgbColor.b;
        }

        @Override
        public int hashCode() {
            return Objects.hash(r, g, b);
        }

        @Override
        public String toString() {
            return String.format("#%02x%02x%02x", r, g, b);
        }

        public static String foregroundRgbStyle(int r, int g, int b) {
            return STYLE_FOREGROUND + ";" + rgbStyle(r, g, b);
        }

        public static String backgroundRgbStyle(int r, int g, int b) {
            return STYLE_BACKGROUND + ";" + rgbStyle(r, g, b);
        }

        public static String rgbStyle(int r, int g, int b) {
            return STYLE_RGB + ";" + r + ";" + g + ";" + b;
        }

        public static String oscCode(int r, int g, int b) {
            return String.format("rgb:%04X/%04X/%04X", r, g, b);
        }

        /**
         * Scale a hex string of 1–4 digits to a 16-bit integer using digit-replication, matching
         * the XTerm/X11 convention for the {@code rgb:} colour spec.
         */
        private static int normalizeHex(String hex) {
            if (hex.isEmpty() || hex.length() > 4) {
                throw new NumberFormatException("invalid hex component length: '" + hex + "'");
            }
            int val = Integer.parseInt(hex, 16);
            switch (hex.length()) {
                case 1:
                    return val * 0x1111; // nibble-repeat: F → FFFF
                case 2:
                    return (val << 8) | val; // byte-repeat: FF → FFFF
                case 3:
                    return (val << 4) | (val >> 8); // 12-bit left-justified: FFF → FFFF
                case 4:
                    return val;
                default:
                    return val;
            }
        }
    }
}
